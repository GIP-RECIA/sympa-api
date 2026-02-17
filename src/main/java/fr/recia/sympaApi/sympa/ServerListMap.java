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

import fr.recia.sympaApi.config.bean.CasProperties;
import fr.recia.sympaApi.config.bean.ServerListMapProperties;
import fr.recia.sympaApi.service.CASCredentialRetrieverService;
import fr.recia.sympaApi.pojo.RobotSympaConf;
import fr.recia.sympaApi.pojo.RobotSympaInfo;
import fr.recia.sympaApi.utils.SessionAttributesHandler;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.CacheManager;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Slf4j
@NoArgsConstructor // only for deserialization
public class ServerListMap extends HashMap<String, SpringCachingSympaServerAxisWsImpl> {

  public ServerListMap(RobotSympaConf robotSympaConf, ServerListMapProperties serverListMapProperties, CASCredentialRetrieverService casCredentialRetriever, UserAttributesHandler userAttributesHandler, SessionAttributesHandler sessionAttributesHandler, CacheManager cacheManager, CasProperties casProperties) throws Exception {
    this.robotSympaConf = robotSympaConf;
    this.serverListMapProperties = serverListMapProperties;
    this.credentialRetriever = casCredentialRetriever;
    this.userAttributesHandler = userAttributesHandler;
    this.sessionAttributesHandler = sessionAttributesHandler;
    this.cacheManager = cacheManager;
    this.casProperties = casProperties;

    //no use of post construct since it must only be invoked at the "true" creation (when deserialized from session it will use the no args constructor)
    this.init();
  }

	private static final long serialVersionUID = -2957480650779043219L;

  private transient  RobotSympaConf robotSympaConf;

  private transient  ServerListMapProperties serverListMapProperties;

  private transient CASCredentialRetrieverService credentialRetriever;

  private transient UserAttributesHandler userAttributesHandler;

  private transient SessionAttributesHandler sessionAttributesHandler;

  private transient CasProperties  casProperties;

  private transient CacheManager cacheManager ;

  //TODO read value from conf
	private final  int timeout = 5000;

  private final String CONNECT_URL_KEY = "connectUrl";

  private String getConnectUrl(){
    return sessionAttributesHandler.getSessionAttribute(CONNECT_URL_KEY, String.class).orElse(null);
  }

  private void setConnectUrl(String connectUrl){
    sessionAttributesHandler.setSessionAttribute(CONNECT_URL_KEY,connectUrl);
  }

  public void init() throws Exception {

    log.warn("post construct de server list map");

    setConnectUrl( casProperties.getBaseServerUrl());

    String currentUai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElse(null);

    if(Objects.isNull(currentUai) || currentUai.isEmpty()){
      //TODO exception message
      return;
    }



    List<String> allUai = userAttributesHandler.getAttributeList(UserAttributesHandler.UAI_ALL).orElse(List.of(currentUai));

    List<String>  isMemberOf = userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF).orElse(List.of());

		if (robotSympaConf.isForAllUai()) {
			for (String uai : allUai) {
				creeSympaServer(uai, isMemberOf);
			}
		} else {
			RobotSympaInfo rsi = creeSympaServer(currentUai, isMemberOf);
			 if (rsi != null && robotSympaConf.isAdminRobotSympaByUai(currentUai, isMemberOf)) {
         userAttributesHandler.setIsAdminSympa(rsi.getAdminPortletUrl());
			}
		}
	}


	private RobotSympaInfo creeSympaServer(String uai, List<String> isMemberOf) throws Exception{
		if (uai != null) {
			RobotSympaInfo rsi = robotSympaConf.getRobotSympaInfoByUai(uai, isMemberOf, false);
			if (rsi != null) {
        log.debug("robotSympaInfo=" + rsi.toString());

				SpringCachingSympaServerAxisWsImpl server = new  SpringCachingSympaServerAxisWsImpl();

				server.setAdminUrl(rsi.getAdminUrl());
        server.setArchivesUrl(rsi.getArchiveUrl());
        server.setConnectUrl(getConnectUrl());
				server.setName(rsi.getNom());
				server.setNewListUrl(rsi.getNewListUrl());
				server.setHomeUrl(rsi.getUrl());
				server.setEndPointUrl(rsi.getSoapUrl());
				server.setTimeout(timeout);
				server.setCredentialRetriever(getCredentialRetriever());
				server.setCacheManager(getCacheManager());
				server.setNewListForRoles(serverListMapProperties.getNewListForRoles());
				server.init();

        log.info("created server {} in creeSympaServer ", server);

				this.put(rsi.getNom(), server);
				return rsi;
			}
		}
		return null;
	}
}
