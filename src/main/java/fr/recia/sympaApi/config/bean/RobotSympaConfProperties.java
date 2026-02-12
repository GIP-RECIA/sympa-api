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
package fr.recia.sympaApi.config.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "robot.sympa.conf")
@Data
@Validated
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class RobotSympaConfProperties {

  private List<String> regexFormatByUai;

  private List<String> regexFormatAdminByUai;

  private Map<String, String> stem2domaine;
  private Map<String, String> stem2PortletAdmin;
  private String formatUrl;
  private String formatSoapUrl;
  private String formatAdminUrl;
  private String formatNewListUrl;
  private String defaultStem;
  private boolean forAllUai;



  @PostConstruct
  public void setupAndDebug() {
    log.info("RobotSympaConfProperties {}", this);
  }

  @Override
  public String toString() {
    return "RobotSympaConfProperties{" +
      "regexFormatByUai=" + regexFormatByUai +
      ", regexFormatAdminByUai=" + regexFormatAdminByUai +
      ", stem2domaine=" + stem2domaine +
      ", stem2PortletAdmin=" + stem2PortletAdmin +
      ", formatUrl='" + formatUrl + '\'' +
      ", formatSoapUrl='" + formatSoapUrl + '\'' +
      ", formatAdminUrl='" + formatAdminUrl + '\'' +
      ", formatNewListUrl='" + formatNewListUrl + '\'' +
      ", defaultStem='" + defaultStem + '\'' +
      ", forAllUai=" + forAllUai +
      '}';
  }
}
