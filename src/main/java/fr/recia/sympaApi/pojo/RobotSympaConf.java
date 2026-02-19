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
package fr.recia.sympaApi.pojo;

import fr.recia.sympaApi.config.bean.RobotSympaConfProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@Slf4j
@NoArgsConstructor //for deserialization
@Service
public class RobotSympaConf {

  @Autowired
  private RobotSympaConfProperties props;

  // forward to ServerListMap
  public boolean isForAllUai(){
    return props.isForAllUai();
  }

  @PostConstruct
  public void postConstruct() {
    log.info(this.toString());
  }

  private String findGrpStem(String uai, List<String> userGrps, List<String> regexFormatList) {
    if (uai == null || userGrps == null ||  regexFormatList == null) {
      return null;
    }
    for (String rgxFormat : regexFormatList) {
      if (rgxFormat != null) {
        try {
          String regex = String.format(rgxFormat, uai);
          Pattern pattern = Pattern.compile(regex);
          for (String grp : userGrps) {
            try {
              Matcher matcher = pattern.matcher(grp);
              if (matcher.matches()) {
                return matcher.group(1);
              }
            } catch (Exception e) {
              log.error(String.format("group user = %s erreur=%s", grp,  e.toString()));
            }
          }
        } catch (Exception e) {
          log.error(String.format("error=%d uai=%s rgxFormat=%s erreur=%s", uai, rgxFormat, e.toString() ));
        }
      }
    }
    return null;
  }
  /**
   * Donne les infos du robot correspondant a l'uai, si les groupes utilisateur le permetent.
   * sinon renvoie null;
   * @param uai
   * @param userGrps
   * @param forAdmin TODO
   * @return RobotSympaInfo :les info du robot ou null
   */
  public RobotSympaInfo getRobotSympaInfoByUai(String uai, List<String> userGrps, boolean forAdmin) {
    List<String> regexFormatList  = forAdmin ?  props.getRegexFormatAdminByUai() : props.getRegexFormatByUai() ;
    String stem  = findGrpStem(uai, userGrps, regexFormatList);
    if (stem != null) {
      String domaine = props.getStem2domaine().get(stem);
      if (domaine == null) {
        domaine = props.getStem2domaine().get(props.getDefaultStem());
      }
      if (domaine != null) {
        RobotSympaInfo rsi = new RobotSympaInfo();
        try {
          rsi.dom = domaine;
          rsi.nom = String.format("%s.%s", uai.toLowerCase(), domaine);
          rsi.uai = uai;
          rsi.url = String.format(props.getFormatUrl(), rsi.nom);
          rsi.soapUrl = String.format(props.getFormatSoapUrl(), rsi.nom);
          rsi.adminUrl = String.format(props.getFormatAdminUrl(), rsi.nom);
          rsi.archiveUrl = String.format(props.getFormatArchiveUrl(), rsi.nom);
          rsi.sympaRemoteUrl = valueOrDefault(props.getStem2sympaRemoteUrl(),stem);
          rsi.sympaRemoteDatabaseId = valueOrDefault(props.getStem2sympaRemoteDatabaseId(),stem);

          log.info("RSI adminUrl {} ", rsi.adminUrl);
          log.info("RSI archiveUrl {} ", rsi.archiveUrl);

          rsi.adminPortletUrl = props.getStem2PortletAdmin().get(stem);
          if (rsi.adminPortletUrl == null || "".equals(rsi.adminPortletUrl) ) {
            rsi.adminPortletUrl = props.getStem2PortletAdmin().get(props.getDefaultStem());
          }
          rsi.newListUrl =String.format(props.getFormatNewListUrl(), rsi.nom);
          return rsi;
        } catch (Exception e) {
          log.error(String.format(" robotInfo = %s \n  erreur=%s", rsi.toString(),  e.toString()));
        }
      }
    }
    return null;
  }

  private String valueOrDefault(Map<String, String> laMap, String stem) {
    String val = laMap.get(stem);
    if (val == null) {
      return laMap.get(props.getDefaultStem());
    }
    return val;
  }

  /**
   * indique si les groupes permetent l'administration des listes du robot donné par l'uai
   * @param uai
   * @param userGrps
   * @return
   */
  public boolean isAdminRobotSympaByUai(String uai, List<String> userGrps) {
    String stem = findGrpStem(uai, userGrps, props.getRegexFormatAdminByUai());
    return  (stem != null) ;

  }
}
