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

import fr.recia.sympaApi.config.bean.UserMappingProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * PortletUserAttributeMapping describe the mapping between the portlet users attributes and
 * the placeholders used in the configuration.
 * For example, the establishment identifier %UAI to replace correspond to the portal user attribute ESCOUAICourant.
 * The portal user attributes are defined in the portlet.xml file.
 * 
 * @author GIP - RECIA Maxime BOSSARD 2012
 *
 */
@Slf4j
@Component
public class UserAttributeMapping {

  @Autowired
  UserMappingProperties userMappingProperties;

	/** User attribute : Siren key. */
	public static final String USER_ATTRIBUTE_SIREN_KEY = "USER_ATTRIBUTE_SIREN_KEY";

	/** Logger. */
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(UserAttributeMapping.class);

	/**
	 * Get the user attribute attached to the placeholder provided.
	 * 
	 * @param placeholder the placeholder key.
	 * @return the corresponding user attribute, or null.
	 */
	public String getMapping(final String placeholder) {
		String result = null;
		if (StringUtils.hasText(placeholder)) {
			result = this.userMappingProperties.getMapping().get(placeholder);
		}

		return result;
	}

	/**
	 * Replace user attributes placeholders like %UAI by their value in the portal.
	 * 
	 * @param chain supplied chain with placeholders to replace.
	 * @param userInfo map of the ser attributes.
	 * @return the chain with placeholders replaced by their values.
	 * Or null if the supplied chain is null.
	 */
	public String substitutePlaceholder(final String chain, final Map<String, String> userInfo) {
		String result = chain;

		String pattern = null;
		for (Entry<String, String> entry : this.userMappingProperties.getMapping().entrySet()) {
			// Retrieve the pattern from the mapping.
			pattern = entry.getKey();
			String value = userInfo.get(entry.getValue());
			if ((pattern != null) && StringUtils.hasText(value)) {
				// Replace the pattern by the user attribute.
				result = result.replaceAll(Pattern.quote(pattern), value);
			}
		}

		return result;
	}

	/**
	 * Build a map of user attributes placeholders and their values in the portal.
	 * The keys are placeholders alphanum chars only.
	 * 
	 * @param userInfo map of the ser attributes.
	 * @return a map with placeholders values.
	 */
	public Map<String, String> buildPlaceholderValuesMap(final Map<String, String> userInfo) {
		Map<String, String> result = new HashMap<String, String>();

		for (Entry<String, String> entry : this.userMappingProperties.getMapping().entrySet()) {
			result.put(entry.getKey().replaceAll("[^a-zA-Z0-9]", "").toLowerCase(),
					userInfo.get(entry.getValue()));
		}

		return result;
	}

  private class log {
  }
}
