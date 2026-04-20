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
package fr.recia.sympaApi.service;

import fr.recia.sympaApi.config.bean.CacheProperties;
import fr.recia.sympaApi.config.bean.CasProperties;
import fr.recia.sympaApi.config.bean.ServerListMapProperties;
import fr.recia.sympaApi.pojo.RobotSympaConf;
import fr.recia.sympaApi.pojo.SympaList;
import fr.recia.sympaApi.pojo.SympaRobot;
import fr.recia.sympaApi.pojo.UserSympaList;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.sympa.ServerListMap;
import fr.recia.sympaApi.sympa.SpringCachingSympaServerAxisWsImpl;
import fr.recia.sympaApi.utils.CacheHandler;
import fr.recia.sympaApi.utils.SessionAttributesHandler;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class DomainService {

  public enum SympaListFields {
    address,
    owner,
    editor,
    subscriber
  }


  @Autowired
  SessionAttributesHandler sessionAttributesHandler;

  @Autowired
  private RobotSympaConf robotSympaConf;

  @Autowired
  private ServerListMapProperties serverListMapProperties;

  @Autowired
  private CASCredentialRetrieverService credentialRetriever;

  @Autowired
  private UserAttributesHandler userAttributesHandler;

  @Autowired
  private CasProperties casProperties ;

  @Autowired
  CacheProperties cacheProperties;

  @Autowired
  CacheHandler cacheHandler;

  private final String SERVER_LIST_KEY = "serverList";

  private ServerListMap getServerListInternal() throws Exception {
    Optional<ServerListMap> optional = sessionAttributesHandler.getSessionAttribute(SERVER_LIST_KEY, ServerListMap.class);
    if(optional.isPresent()){
      // re-inject because these attributes are not puts in cache
      optional.get().setUserAttributesHandler(this.userAttributesHandler);
      optional.get().setSessionAttributesHandler(this.sessionAttributesHandler);
      optional.get().setRobotSympaConf(this.robotSympaConf);
      optional.get().setCredentialRetriever(this.credentialRetriever);
      optional.get().setCasProperties(this.casProperties);
      optional.get().setServerListMapProperties(this.serverListMapProperties);
      optional.get().setCacheProperties(this.cacheProperties);
      optional.get().setCacheHandler(this.cacheHandler);
      return optional.get();
    }

    return new ServerListMap(robotSympaConf, serverListMapProperties, credentialRetriever, userAttributesHandler, sessionAttributesHandler, casProperties, cacheProperties, cacheHandler);

  }


  public List<UserSympaListWithUrl> getWhich() throws Exception {
    Collection<SpringCachingSympaServerAxisWsImpl> srvList = getServerList().values();
    List<UserSympaListWithUrl> result = new ArrayList<>();
    for ( SpringCachingSympaServerAxisWsImpl s : srvList ) {
      List<UserSympaListWithUrl> srvResult = s.getWhich(SympaRobot.getDefaultRobot());
      if ( srvResult != null && !srvResult.isEmpty()) {
        result.addAll(srvResult);
      }
    }
    // default sort on list address
    sortResults(result);
    return result;
  }

  public List<SympaList> getLists() throws Exception {
    Collection<SpringCachingSympaServerAxisWsImpl> srvList = getServerList().values();
    List<SympaList> result = new ArrayList<>();
    for ( SpringCachingSympaServerAxisWsImpl s : srvList ) {
      List<SympaList> srvResult = s.getLists(SympaRobot.getDefaultRobot());
      if ( (srvResult != null) && (!srvResult.isEmpty()) ) {
        result.addAll(srvResult);
      }
    }
    return result;
  }


  //protected boolean have
  // sorting
  private void sortResults(List<UserSympaListWithUrl> toSort) {
    toSort.sort(new UserSympaListComparator());
  }

  static class UserSympaListComparator implements Comparator<UserSympaList> {
    boolean sortOrder; // true mean ascending
    SympaListFields sortOn;
    public UserSympaListComparator() {
      this.sortOrder = true;
      this.sortOn = SympaListFields.address;
    }

    public int compare(UserSympaList o1, UserSympaList o2) {
      int result = 0;
      switch (sortOn) {
        case address :
          result = compareString(o1.getAddress(), o2.getAddress());
          break;
        case owner :
          result = compareBoolean(o1.isOwner(), o2.isOwner());
          break;
        case editor:
          result = compareBoolean(o1.isEditor(), o2.isEditor());
          break;
        case subscriber:
          result = compareBoolean(o1.isSubscriber(), o2.isSubscriber());
          break;
      }
      return result;
    }
    private int compareBoolean(boolean b1, boolean b2) {
      int result;
      if ( (b1 && b2) || (!b1 && !b2) ) return 0;
      if ( sortOrder ) {
        result = ( b1 ) ? 1 : -1;
      } else {
        result = ( b1 ) ? -1 : 1;
      }
      return result;
    }
    private int compareString(String s1, String s2) {
      if ( s1 == null || s2 == null ) return 0;
      int result;
      if ( sortOrder ) {
        result = s1.compareTo(s2);
      } else {
        result = s2.compareTo(s1);
      }
      return result;
    }
  }
  /**
   * @return the serverList
   */
  public Map<String, SpringCachingSympaServerAxisWsImpl> getServerList() throws Exception {
    Map<String, SpringCachingSympaServerAxisWsImpl> serverListToUse = new HashMap<>();
    for(String serverKey: getServerListInternal().keySet()) {
        log.debug("Add this server to the list for the current user : " + serverKey);
        serverListToUse.put(serverKey, getServerListInternal().get(serverKey));
    }
    return serverListToUse;
  }
}
