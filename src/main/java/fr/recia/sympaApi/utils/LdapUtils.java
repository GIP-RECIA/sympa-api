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
package fr.recia.sympaApi.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

import java.util.List;

/**
 * LDAP requesting utils class.
 * 
 * @author GIP - RECIA 2012 Maxime BOSSARD
 *
 */

@Slf4j
public class LdapUtils {

	/** Logger. */

	/** Hidden constructor. */
	private LdapUtils() {

	}

	/**
	 * Search in LDAP.
	 * 
	 * @param ldapTemplate the template to search into.
	 * @param searchFilter the search filter.
	 * @param searchString the search base.
	 * @param mapper the Attributes mapper to return data.
	 * @return the search result.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T ldapSearch(final LdapTemplate ldapTemplate, final String searchFilter,
			final String searchString, final AttributesMapper mapper) {
		try {
			List<String> l = ldapTemplate.search(searchString, searchFilter, mapper);

			log.debug("Number of item found {}", l.size());

			return (T) l;

		} catch (Exception ex) {
			log.error("Error during LDAP search !", ex);
			return null;
		}
	}

}
