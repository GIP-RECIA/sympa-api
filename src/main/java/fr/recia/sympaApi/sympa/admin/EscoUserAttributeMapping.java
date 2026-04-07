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

import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * PortletUserAttributeMapping describe the mapping between the portlet users attributes and
 * the placeholders used in the configuration.
 * For example, the establishment identifier %UAI to replace correspond to the portal user attribute ESCOUAICourant.
 * The portal user attributes are defined in the portlet.xml file.
 * 
 * @author GIP - RECIA Maxime BOSSARD 2012
 *
 */

@Component
@Slf4j
public class EscoUserAttributeMapping extends UserAttributeMapping {


  @Autowired
  UserAttributesHandler userAttributesHandler;
	/**
	 * Add informations in user info map.
	 * 
	 * @param userInfo portal user attributes map.
	 * @return an unmodifiable enhanced map.
	 */
	public Map<String, String> enhanceUserInfo(final Map<String, String> userInfo) {
		Map<String, String> result = new HashMap<String, String>(userInfo);

		this.addSirenToUserInfo(result);

		return Collections.unmodifiableMap(result);
	}

	/**
	 * Add the siren to the portal user attributes map.
	 * 
	 * @param userInfo modifiable attribute map.
	 */
	private void addSirenToUserInfo(final Map<String, String> userInfo) {
    userInfo.put(UserAttributeMapping.USER_ATTRIBUTE_SIREN_KEY, userAttributesHandler.getAttribute(UserAttributesHandler.SIREN_CURRENT).orElseThrow());
	}

}
