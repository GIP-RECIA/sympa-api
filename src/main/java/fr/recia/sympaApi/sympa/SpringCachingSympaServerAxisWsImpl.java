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

import com.fasterxml.jackson.core.type.TypeReference;
import fr.recia.sympaApi.config.bean.CacheProperties;
import fr.recia.sympaApi.pojo.CreateListInfo;
import fr.recia.sympaApi.pojo.SympaCredential;
import fr.recia.sympaApi.pojo.SympaRobot;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.service.CASCredentialRetrieverService;
import fr.recia.sympaApi.utils.CacheHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.axis.transport.http.HTTPConstants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.sympa.client.ws.axis.v544.SOAPStub;
import org.sympa.client.ws.axis.v544.SympaPort_PortType;
import org.sympa.client.ws.axis.v544.SympaSOAPLocator;

import javax.xml.rpc.ServiceException;
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
import java.util.Set;

@Slf4j
@Getter
@Setter
public class SpringCachingSympaServerAxisWsImpl {


  private Map<SympaRobot, SympaPort_PortType> portCache = new HashMap<>(8);


  private CacheProperties cacheProperties;

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

  private CacheHandler cacheHandler;

  public void init() {
  }

  public CreateListInfo getCreateListInfo() {

    CreateListInfo infos  = new CreateListInfo();
    infos.setServerName(getName());
    infos.setAccessUrl(generateConnectUrl(getNewListUrl()));

    return infos;
  }

  protected String generateListUrl(final String listHomepage) {
    return this.generateConnectUrl(listHomepage);
  }

  protected String generateConnectUrl(final String url) {
    String tmpConnectUrl = this.getConnectUrl();
    if ( (tmpConnectUrl == null) || (tmpConnectUrl.trim().length() <= 0) ) {
      return url;
    }
    String tmpUrl = url;
    tmpUrl = URLEncoder.encode(tmpUrl, StandardCharsets.UTF_8);
    return tmpConnectUrl.replaceFirst("%s", tmpUrl);
  }

  protected String generateListAdminUrl(final SympaRobot robot, final String listAddress) {
    String strListName = listAddress;
    if ( (listAddress != null) && (!listAddress.isEmpty()) ) {
      int atIdx = listAddress.indexOf("@");
      if ( atIdx > 0) {
        strListName = listAddress.substring(0, atIdx);
      }
    }
    String tmpUrl = this.getAdminUrl(robot);
    assert strListName != null;
    return this.generateConnectUrl(tmpUrl.replaceFirst("%l", strListName));
  }

  protected String generateListArchivesUrl(final SympaRobot robot, final String listAddress) {
    String strListName = listAddress;
    if ( (listAddress != null) && (!listAddress.isEmpty()) ) {
      int atIdx = listAddress.indexOf("@");
      if ( atIdx > 0) {
        strListName = listAddress.substring(0, atIdx);
      }
    }
    String tmpUrl = this.getArchivesUrl(robot);
    assert strListName != null;
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
    SympaSOAPLocator locator = new SympaSOAPLocator();
    locator.setMaintainSession(true); // mandatory for cookie after login
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
      } catch (MalformedURLException | RemoteException | ServiceException e) {
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
    String[] whichList;
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
    List<UserSympaListWithUrl> result = new ArrayList<>();
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


        item.setListUrl(this.generateListUrl(item.getHomepage()));
        item.setListAdminUrl(this.generateListAdminUrl(robot, item.getAddress()));
        item.setListArchivesUrl(this.generateListArchivesUrl(robot, item.getAddress()));
        result.add(item);
      }
    }
    return result;
  }

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
      } catch (MalformedURLException | RemoteException | ServiceException e) {
        log.error("unable to get a new SympaPort_PortType",e);
        return null;
      }
    }
    if (this.portCache.get(robot) == null ) {
      log.error("unable to get a new SympaPort_PortType");
      return null;
    }
    // do the which
    String[] lists;
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
    List<UserSympaListWithUrl> result = new ArrayList<>();
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

  protected static Map<String, String> stringToMap(String input) {
    Map<String, String> map = new HashMap<>();

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
    String cacheName = cacheProperties.getSpringCachingSympaAxisServerCacheName();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String cacheKey = String.format("%1$s;%2$s;%3$s", getName(),"getWhich",authentication.getName());
    log.debug("cache key = "+cacheKey);
    List<UserSympaListWithUrl> cached = cacheHandler.getFromCache(cacheName, cacheKey, new TypeReference<>() {
    });
    if (cached != null)  {
      log.info("return from cache {}", cached.subList(0,5) );
      return cached;
    }
    List<UserSympaListWithUrl> result = getWhichCacheless(robot);
     cacheHandler.putObjectInCache(cacheName, cacheKey, result);
    return result;
  }

  @Override
  public String toString() {
    return "SpringCachingSympaServerAxisWsImpl{" +
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
