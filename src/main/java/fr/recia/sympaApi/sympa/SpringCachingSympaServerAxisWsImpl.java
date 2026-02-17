/**
 * Copyright © 2026 GIP-RECIA (https://www.recia.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.recia.sympaApi.sympa;

import fr.recia.sympaApi.pojo.CreateListInfo;
import fr.recia.sympaApi.pojo.SympaCredential;
import fr.recia.sympaApi.pojo.SympaRobot;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.service.CASCredentialRetrieverService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.axis.transport.http.HTTPConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.sympa.client.ws.axis.v544.SOAPStub;
import org.sympa.client.ws.axis.v544.SympaPort_PortType;
import org.sympa.client.ws.axis.v544.SympaSOAP;
import org.sympa.client.ws.axis.v544.SympaSOAPLocator;

import javax.annotation.PostConstruct;
import javax.xml.rpc.ServiceException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Getter
@Setter
public class SpringCachingSympaServerAxisWsImpl {

  private CacheManager cacheManager = null;

  private Cache cache = null;

  private Map<SympaRobot, SympaPort_PortType> portCache = new HashMap<SympaRobot, SympaPort_PortType>(8);



  /**
   * name of the sympa server
   */
  private String name;
  /**
   * root url of the sympa server
   */
  private String homeUrl;
  /**
   * wrapper url (%s) will be replaced by various service url (userfull for cas)
   */
  private String connectUrl;
  /**
   * administrative url (%l) will be replaced by list name
   */
  private String adminUrl;
  /**
   * new list url
   */
  private String newListUrl;
  /**
   * createListInfos available on if user in this roles
   */
  private Set<String> newListForRoles;
  /**
   * this server is used only if user in this roles (or if usedForRoles is null)
   */
  private Set<String> usedForRoles;

  private String archivesUrl;


  private int timeout = 5000;

  private String endPointUrl;

  private CASCredentialRetrieverService credentialRetriever;

  public void init() {
    cache = cacheManager.getCache("sympaServerCache"); //todo make cache name a const/property
  }

  public CreateListInfo getCreateListInfo() {

    CreateListInfo infos  = new CreateListInfo();
    infos.setServerName(getName());
    infos.setAccessUrl(generateConnectUrl(getNewListUrl()));

    return infos;
  }

  protected String generateListUrl(final SympaRobot robot, final String listHomepage) {
    return this.generateConnectUrl(listHomepage);
  }

  protected String generateConnectUrl(final String url) {
    String tmpConnectUrl = this.getConnectUrl();
    if ( (tmpConnectUrl == null) || (tmpConnectUrl.trim().length() <= 0) ) {
      return url;
    }
    String tmpUrl = url;
    try {
      tmpUrl = URLEncoder.encode(tmpUrl,"UTF-8");
    } catch (UnsupportedEncodingException e) {
      log.error("unable to urlencode",e);
    }
    String strTmp = tmpConnectUrl.replaceFirst("%s", tmpUrl);
    return strTmp;
  }

  protected String generateListAdminUrl(final SympaRobot robot, final String listAddress) {
    String strListName = listAddress;
    if ( (listAddress != null) && (listAddress.length() > 0) ) {
      int atIdx = listAddress.indexOf("@");
      if ( atIdx > 0) {
        strListName = listAddress.substring(0, atIdx);
      }
    }
    String tmpUrl = this.getAdminUrl(robot);
    return this.generateConnectUrl(tmpUrl.replaceFirst("%l", strListName));
  }

  protected String generateListArchivesUrl(final SympaRobot robot, final String listAddress) {
    String strListName = listAddress;
    if ( (listAddress != null) && (listAddress.length() > 0) ) {
      int atIdx = listAddress.indexOf("@");
      if ( atIdx > 0) {
        strListName = listAddress.substring(0, atIdx);
      }
    }
    String tmpUrl = this.getArchivesUrl(robot);
    return this.generateConnectUrl(tmpUrl.replaceFirst("%l", strListName));
  }

  public String getArchivesUrl(final SympaRobot robot) {
    return robot.transformRobotUrl(this.archivesUrl);
  }

  public String getAdminUrl(final SympaRobot robot) {
    return robot.transformRobotUrl(this.adminUrl);
  }



  // must be session scope so the bean of type SympaServerAxisWsImpl is session scope
  private SympaPort_PortType port = null;






  private SympaPort_PortType getPort(final SympaRobot robot) throws MalformedURLException, ServiceException, RemoteException {
    SympaSOAP locator = new SympaSOAPLocator();
    ((SympaSOAPLocator)locator).setMaintainSession(true); // mandatory for cookie after login
    final String endpointUrl = this.getEndPointUrl(robot);
    if (log.isDebugEnabled()) {
      log.debug(String.format("SympaSoap endpoint URL: [%1$s] for Robot: [%2$s].", endpointUrl, robot));
    }
    SympaPort_PortType port = locator.getSympaPort(new URL(endpointUrl));
    // set a timeout on port (10 seconds)
    ((org.apache.axis.client.Stub)port).setTimeout(this.getTimeout());
    // now authenticate
    SympaCredential creds = this.getCredentialRetriever().getCredentialForService(endpointUrl);
    if ( creds == null ) {
      log.error("unable to retrieve credential for service " + endpointUrl);
      return null;
    }
        String tmp = port.casLogin(creds.getPassword());
        ((SOAPStub)port)._setProperty(HTTPConstants.HEADER_COOKIE,
          "sympa_session=" + tmp);
        if ( log.isDebugEnabled() ) {
          log.debug("CAS authentication ok : "+tmp);
        }

    return port;
  }

  public String getEndPointUrl(final SympaRobot robot) {
    return robot.transformRobotUrl(this.endPointUrl);
  }



  public List<UserSympaListWithUrl> getWhichCacheless(final SympaRobot robot) {
    // first of all; get a fresh new port if needed
    if(this.portCache.get(robot)!=null) {
      try {
        String checkCookie = this.portCache.get(robot).checkCookie();
        if((checkCookie == null) || "nobody".equals(checkCookie)) {
          this.portCache.put(robot, null);
        }
      } catch (RemoteException e) {
        log.debug("port is no more usable, we reinitate it",e);
        this.portCache.put(robot, null);
      }
    }
    if(this.portCache.get(robot) == null) {
      try {
        this.portCache.put(robot, this.getPort(robot));
      } catch (MalformedURLException e) {
        log.error("unable to get a new SympaPort_PortType",e);
        return null;
      } catch (ServiceException e) {
        log.error("unable to get a new SympaPort_PortType",e);
        return null;
      } catch (RemoteException e) {
        log.error("unable to get a new SympaPort_PortType",e);
        return null;
      }
    }
    if (this.portCache.get(robot) == null ) {
      log.error("unable to get a new SympaPort_PortType");
      return null;
    }
    // do the which
    //ListType[] whichList = null;
    String[] whichList = null;
    try {
			/* BUG
			 *    """org.xml.sax.SAXException:  No deserializer for {http://www.w3.org/2001/XMLSchema}anyType"""
			 * with Axis
			 * so we use port.which() ...
			whichList = SympaPort_PortType.complexWhich();
			 */
      whichList = this.portCache.get(robot).which();
    } catch (RemoteException e) {
      log.error("complexWhich() failed !",e);
      return null;
    }
    List<UserSympaListWithUrl> result = new ArrayList<UserSympaListWithUrl>();
    if ( whichList != null ) {
      for (String l : whichList) {
        Map<String, String> listeInfos = stringToMap(l);
        UserSympaListWithUrl item = new UserSympaListWithUrl();
        item.setEditor(listeInfos.get("isEditor").equals("1"));
        item.setOwner(listeInfos.get("isOwner").equals("1"));
        item.setSubscriber(listeInfos.get("isSubscriber").equals("1"));
        item.setAddress(listeInfos.get("listAddress"));
        item.setHomepage(listeInfos.get("homepage"));
        item.setSubject(listeInfos.get("subject"));
        //  append various urls


        item.setListUrl(this.generateListUrl(robot, item.getHomepage()));
        item.setListAdminUrl(this.generateListAdminUrl(robot, item.getAddress()));
        item.setListArchivesUrl(this.generateListArchivesUrl(robot, item.getAddress()));
        result.add(item);
      }
    }
    return result;
  }

//  protected List<UserSympaListWithUrl> getWhichCacheless() {
//    // first of all; get a fresh new port if needed
//    if(port!=null) {
//      try {
//        String checkCookie = port.checkCookie();
//        if(checkCookie == null || "nobody".equals(checkCookie))
//          port = null;
//      } catch (RemoteException e) {
//        log.debug("port is no more usable, we reinitate it",e);
//        port = null;
//      }
//    }
//    if(port == null) {
//      try {
//        port = getPort();
//      } catch (MalformedURLException e) {
//        log.error("unable to get a new SympaPort_PortType: MalformedURLException",e);
//        return null;
//      } catch (ServiceException e) {
//        log.error("unable to get a new SympaPort_PortType: ServiceException",e);
//        return null;
//      } catch (RemoteException e) {
//        log.error("unable to get a new SympaPort_PortType: RemoteException",e);
//        return null;
//      }
//    }
//    if (port == null ) {
//      log.error("unable to get a new SympaPort_PortType");
//      return null;
//    }
//    // do the which
//    String[] whichList = null;
//    try {
//			/* BUG
//			 *    """org.xml.sax.SAXException:  No deserializer for {http://www.w3.org/2001/XMLSchema}anyType"""
//			 * with Axis
//			 * so we use port.which() ...
//			whichList = SympaPort_PortType.complexWhich();
//			*/
//      whichList = port.which();
//    } catch (RemoteException e) {
//      log.error("complexWhich() failed !",e);
//      return null;
//    }
//    List<UserSympaListWithUrl> result = new ArrayList<UserSympaListWithUrl>();
//    if ( whichList != null ) {
//      for ( int idx = 0; idx < whichList.length; idx++ ) {
//        String l = whichList[idx];
//        Map<String, String> listeInfos = stringToMap(l);
//        UserSympaListWithUrl item = new UserSympaListWithUrl();
//        item.setEditor(listeInfos.get("isEditor").equals("1"));
//        item.setOwner(listeInfos.get("isOwner").equals("1"));
//        item.setSubscriber(listeInfos.get("isSubscriber").equals("1"));
//        item.setAddress(listeInfos.get("listAddress"));
//        item.setHomepage(listeInfos.get("homepage"));
//        item.setSubject(listeInfos.get("subject"));
//
//        item.setListUrl(generateListUrl(item.getHomepage()));
//        item.setListAdminUrl(generateListAdminUrl(item.getAddress()));
//        item.setListArchivesUrl(generateListArchiveUrl(item.getAddress()));
//        result.add(item);
//      }
//    }
//    return result;
//  }

  public List<UserSympaListWithUrl> getLists(final SympaRobot robot) {
    // first of all; get a fresh new port if needed
    if(this.portCache.get(robot)!=null) {
      try {
        String checkCookie = this.portCache.get(robot).checkCookie();
        if((checkCookie == null) || "nobody".equals(checkCookie)) {
          this.portCache.put(robot, null);
        }
      } catch (RemoteException e) {
        log.debug("port is no more usable, we reinitate it",e);
        this.portCache.put(robot, null);
      }
    }
    if(this.portCache.get(robot) == null) {
      try {
        this.portCache.put(robot, this.getPort(robot));
      } catch (MalformedURLException e) {
        log.error("unable to get a new SympaPort_PortType",e);
        return null;
      } catch (ServiceException e) {
        log.error("unable to get a new SympaPort_PortType",e);
        return null;
      } catch (RemoteException e) {
        log.error("unable to get a new SympaPort_PortType",e);
        return null;
      }
    }
    if (this.portCache.get(robot) == null ) {
      log.error("unable to get a new SympaPort_PortType");
      return null;
    }
    // do the which
    String[] lists = null;
    try {
			/* BUG
			 *    """org.xml.sax.SAXException:  No deserializer for {http://www.w3.org/2001/XMLSchema}anyType"""
			 * with Axis
			 * so we use port.which() ...
			whichList = SympaPort_PortType.complexWhich();
			 */
      lists = this.portCache.get(robot).lists("", "");
    } catch (RemoteException e) {
      log.error("lists() failed !",e);
      return null;
    }
    List<UserSympaListWithUrl> result = new ArrayList<UserSympaListWithUrl>();
    if ( lists != null ) {
      for (String l : lists) {
        Map<String, String> listeInfos = stringToMap(l);
        UserSympaListWithUrl item = new UserSympaListWithUrl();
        item.setAddress(listeInfos.get("listAddress"));
        item.setHomepage(listeInfos.get("homepage"));
        item.setSubject(listeInfos.get("subject"));
        result.add(item);
      }
    }
    return result;
  }

  private SympaPort_PortType getPort() throws MalformedURLException, ServiceException, RemoteException {
    SympaSOAP locator = new SympaSOAPLocator();
    ((SympaSOAPLocator)locator).setMaintainSession(true); // mandatory for cookie after login
    log.info("getPort getEndpointURl {}",getEndPointUrl());
    SympaPort_PortType port = locator.getSympaPort(new URL(getEndPointUrl()));
    // set a timeout on port (10 seconds)
    ((org.apache.axis.client.Stub)port).setTimeout(getTimeout());
    // now authenticate
    SympaCredential creds = getCredentialRetriever().getCredentialForService(endPointUrl);
    if ( creds == null ) {
      log.error("unable to retrieve credential for service "+endPointUrl);
      return null;
    }
    String tmp = port.casLogin(creds.getPassword());
    ((SOAPStub)port)._setProperty(HTTPConstants.HEADER_COOKIE,
      "sympa_session=" + tmp);
    if ( log.isDebugEnabled() ) {
      log.debug("CAS authentication ok : "+tmp);
    }
    return port;
  }

  protected static Map<String, String> stringToMap(String input) {
    Map<String, String> map = new HashMap<String, String>();

    String[] nameValuePairs = input.split(";");
    for (String nameValuePair : nameValuePairs) {
      String[] nameValue = nameValuePair.split("=");
      map.put(URLDecoder.decode(nameValue[0], StandardCharsets.UTF_8), nameValue.length > 1 ? URLDecoder.decode(
        nameValue[1], StandardCharsets.UTF_8) : "");
    }

    return map;
  }

  @SuppressWarnings("unchecked")
  public List<UserSympaListWithUrl> getWhich(SympaRobot robot) {
    // cacheKey = serverInstance/methodName/useridentifier
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String cacheKey = String.format("%1$s;%2$s;%3$s", getName(),"getWhich",authentication.getName());
    log.debug("cache key = "+cacheKey);
    Object cached = getCachedValue(cacheKey);
    if (cached != null) return (List<UserSympaListWithUrl>)cached;
    List<UserSympaListWithUrl> result = getWhichCacheless(robot);
    setCachedValue(cacheKey,result);
    return result;
  }

  private void setCachedValue(String cacheKey, Object toCache) {
    // todo re enable cache after test
//    log.info(" setCachedValue cache is null ? = {}", Objects.isNull(cache));
//    cache.put(cacheKey, toCache);
  }

  private Object getCachedValue(String cacheKey) {

    Cache.ValueWrapper wrapper = cache.get(cacheKey);
    if (wrapper == null) {
      log.debug("no cache value for key {}", cacheKey);
      return null;
    }
    Object result = wrapper.get();
    log.debug("having cached value for key {}", cacheKey);
    return result;
  }

  @Override
  public String toString() {
    return "SpringCachingSympaServerAxisWsImpl{" +
      "cacheManager=" + cacheManager +
      ", cache=" + cache +
      ", portCache=" + portCache +
      ", name='" + name + '\'' +
      ", homeUrl='" + homeUrl + '\'' +
      ", connectUrl='" + connectUrl + '\'' +
      ", adminUrl='" + adminUrl + '\'' +
      ", newListUrl='" + newListUrl + '\'' +
      ", newListForRoles=" + newListForRoles +
      ", usedForRoles=" + usedForRoles +
      ", archivesUrl='" + archivesUrl + '\'' +
      ", timeout=" + timeout +
      ", endPointUrl='" + endPointUrl + '\'' +
      ", credentialRetriever=" + credentialRetriever +
      ", port=" + port +
      '}';
  }
}
