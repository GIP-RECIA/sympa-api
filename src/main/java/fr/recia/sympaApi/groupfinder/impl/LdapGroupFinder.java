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
package fr.recia.sympaApi.groupfinder.impl;


import fr.recia.sympaApi.config.bean.LdapGroupFinderProperties;
import fr.recia.sympaApi.groupfinder.IEtabGroupsFinder;
import fr.recia.sympaApi.sympa.admin.UserAttributeMapping;
import fr.recia.sympaApi.utils.LdapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.Collection;
import java.util.Map;


@Slf4j
@Getter
@Setter
@Service
public class LdapGroupFinder implements IEtabGroupsFinder {


	/** configured via spring with connection info and baseDN.*/
  @Autowired
	private LdapTemplate ldapTemplate;

  @Autowired
  LdapGroupFinderProperties properties;

	/** Prefix to add to all groups. */
	private String groupPrefix;

	/** User attributes mapping. */
  @Autowired
	private UserAttributeMapping userAttributeMapping;

	public Collection<String> findGroupsOfEtab(final Map<String, String> userInfo) {

		String searchFilter = this.getUserAttributeMapping()
				.substitutePlaceholder(this.properties.getLdapSearchFilter(), userInfo);
		String searchString = this.getUserAttributeMapping()
				.substitutePlaceholder(this.properties.getLdapSearchBaseDN(), userInfo);

		log.debug("Searching for ldap groups for user attributes ["
				+ userInfo.toString() + "] with searchString " + searchString
				+ " and searchfilter " + searchFilter);

		AttributesMapper groupsMapper = new AttributesMapper() {
			public Object mapFromAttributes(final Attributes attrs) throws NamingException {
				Attribute groupNameAttr = attrs.get(LdapGroupFinder.this.properties.getLdapGroupAttribute());

				StringBuilder groupName = new StringBuilder();
				if (groupNameAttr != null) {
					if (!Strings.isEmpty(LdapGroupFinder.this.groupPrefix)) {
						groupName.append(LdapGroupFinder.this.groupPrefix);
					}
					groupName.append(groupNameAttr.get());
				}
				return groupName.toString();
			}
		};

		return LdapUtils.ldapSearch(this.ldapTemplate, searchFilter, searchString, groupsMapper);
	}



}
