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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RobotSympaInfo {
  String dom;
  String nom;
  String uai;
  String url;
  String soapUrl;
  String adminUrl;
  String newListUrl;
  String adminPortletUrl;
  String archiveUrl;


  @Override
  public String toString() {
    return "RobotSympaInfo [dom=" + dom + ", nom=" + nom + ", uai=" + uai + ", url=" + url + ", soapUrl=" + soapUrl
      + ", adminUrl=" + adminUrl + ", newListUrl=" + newListUrl + ", adminPortletUrl=" + adminPortletUrl
      + "]";
  }
}
