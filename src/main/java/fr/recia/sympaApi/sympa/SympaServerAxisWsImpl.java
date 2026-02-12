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

import fr.recia.sympaApi.pojo.SympaCredential;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.axis.transport.http.HTTPConstants;
import org.sympa.client.ws.axis.v544.SOAPStub;
import org.sympa.client.ws.axis.v544.SympaPort_PortType;
import org.sympa.client.ws.axis.v544.SympaSOAP;
import org.sympa.client.ws.axis.v544.SympaSOAPLocator;

import javax.xml.rpc.ServiceException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class SympaServerAxisWsImpl extends AbstractSympaServer {
	
	private int timeout = 5000;
	
	private String endPointUrl;
	
	// must be session scope so the bean of type SympaServerAxisWsImpl is session scope
	private SympaPort_PortType port = null; 
	
	@Override
	public List<UserSympaListWithUrl> getWhich() {
		// first of all; get a fresh new port if needed
		if(port!=null) {
			try {
				String checkCookie = port.checkCookie();
				if(checkCookie == null || "nobody".equals(checkCookie))
					port = null;
			} catch (RemoteException e) {
        log.debug("port is no more usable, we reinitate it",e);
				port = null;
			}
		}
		if(port == null) {
			try {
				port = getPort();
			} catch (MalformedURLException e) {
        log.error("unable to get a new SympaPort_PortType: MalformedURLException",e);
				return null;
			} catch (ServiceException e) {
        log.error("unable to get a new SympaPort_PortType: ServiceException",e);
				return null;
			} catch (RemoteException e) {
        log.error("unable to get a new SympaPort_PortType: RemoteException",e);
				return null;
			}
		}
		if (port == null ) {
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
			whichList = port.which();
		} catch (RemoteException e) {
      log.error("complexWhich() failed !",e);
			return null;
		}
		List<UserSympaListWithUrl> result = new ArrayList<UserSympaListWithUrl>();
		if ( whichList != null ) {
			for ( int idx = 0; idx < whichList.length; idx++ ) {
				String l = whichList[idx];
				Map<String, String> listeInfos = this.stringToMap(l);
				UserSympaListWithUrl item = new UserSympaListWithUrl();
				item.setEditor(listeInfos.get("isEditor").equals("1"));
				item.setOwner(listeInfos.get("isOwner").equals("1"));
				item.setSubscriber(listeInfos.get("isSubscriber").equals("1"));
				item.setAddress(listeInfos.get("listAddress"));
				item.setHomepage(listeInfos.get("homepage"));
				item.setSubject(listeInfos.get("subject"));
				
				item.setListUrl(generateListUrl(item.getHomepage()));
				item.setListAdminUrl(generateListAdminUrl(item.getAddress()));
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
	
}
