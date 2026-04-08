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
package fr.recia.sympaApi.sympa.admin;

import fr.recia.sympaApi.pojo.RobotSympaConf;
import fr.recia.sympaApi.pojo.RobotSympaInfo;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Getter
@Setter
@Service
public class RobotDomaineNameResolver  {
	
	protected final Log logger = LogFactory.getLog(this.getClass());
	
	//private UserInfoBean userInfoBean;
  @Autowired
	private RobotSympaConf robotSympaConf;

  @Autowired
  UserAttributesHandler userAttributesHandler;
	
	public String resolveRobotDomainName() {

    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT);

    List<String> isMemberOf = userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF);

    RobotSympaInfo rsi = robotSympaConf.getRobotSympaInfoByUai(uai, isMemberOf, false);
    // todo read in session instead of RSC

		if (rsi == null) {
      rsi = robotSympaConf.getRobotSympaInfoByUai(uai, isMemberOf, false);
			if (rsi == null) {
				return null;
			}

      //todo store in session
		//	userInfoBean.setRobotSympaInfo(rsi);
		}
		return rsi.getNom();
		
	}


}
