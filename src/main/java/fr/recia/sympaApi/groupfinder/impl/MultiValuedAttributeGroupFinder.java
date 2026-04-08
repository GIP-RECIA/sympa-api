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

import fr.recia.sympaApi.service.UserAttributeMapping;
import fr.recia.sympaApi.utils.LdapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Search in LDAP for Groups in multi-valued attribute.
 * 
 * @author GIP - RECIA Maxime BOSSARD
 *
 */

@Getter
@Setter
@Slf4j
public class MultiValuedAttributeGroupFinder extends LdapGroupFinder {



  private final String ldapSearchFilter;
  private final String ldapSearchBaseDN;
  private final String ldapGroupAttribute;


	/** Constructor. */
	public MultiValuedAttributeGroupFinder(Map<String, String> args, LdapTemplate ldapTemplate, UserAttributeMapping userAttributeMapping, String ldapSearchBaseDN) {
    this.ldapSearchFilter = args.get("ldapSearchFilter");
    this.setGroupPrefix(args.get("groupPrefix"));
    this.ldapGroupAttribute = args.get("ldapGroupAttribute");
    this.setLdapTemplate(ldapTemplate);
    this.setUserAttributeMapping(userAttributeMapping);
    this.ldapSearchBaseDN = ldapSearchBaseDN;
    log.debug("initiated MultiValuedAttributeGroupFinder {}", ldapSearchFilter);
	}

	@Override
	public Collection<String> findGroupsOfEtab(final Map<String, String> userInfo) {
		String searchFilter = this.getUserAttributeMapping()
				.substitutePlaceholder(this.getLdapSearchFilter(), userInfo);
		String searchString = this.getUserAttributeMapping()
				.substitutePlaceholder(this.getLdapSearchBaseDN(), userInfo);

		AttributesMapper groupsMapper = new AttributesMapper() {
			@SuppressWarnings("unchecked")
			public Object mapFromAttributes(final Attributes attrs) throws NamingException {
				Attribute groupNameAttr = attrs.get(MultiValuedAttributeGroupFinder.this.getLdapGroupAttribute());

				Collection<String> groups = new HashSet<>();
				if (groupNameAttr != null) {
					NamingEnumeration<String> values =
							(NamingEnumeration<String>) groupNameAttr.getAll();
					while (values.hasMoreElements()) {
						groups.add(values.nextElement());
					}
				}

				return groups;
			}
		};

		Collection<Collection<String>> groupsList =
				LdapUtils.ldapSearch(this.getLdapTemplate(), searchFilter, searchString, groupsMapper);

		// Merge of all Sets and add the prefix on all groups
		Collection<String> groups = new HashSet<>();
		StringBuilder groupName = new StringBuilder(128);
		if (groupsList != null) {
			for (Collection<String> groupSet : groupsList) {
				for (String group : groupSet) {
					groupName.setLength(0);
					if (!Strings.isEmpty(this.getGroupPrefix())) {
						groupName.append(this.getGroupPrefix());
					}
					groupName.append(group);
					groups.add(groupName.toString());
				}
			}
		} else {
      log.warn("groupsList null dans la requette ldap :{}", searchFilter);
      log.debug("        searchString :{}", searchString);
		}
		
			if (groups.isEmpty()) {
				log.info("No multi-valued attribute groups found for filter: [{}]", this.getLdapSearchFilter());
			} else {
        log.info("[{}] Multi-valued attribute groups found for filter: [{}]", groups.size(), this.getLdapSearchFilter());
			}


			if (!groups.isEmpty()) {
				for (String group : groups) {
          log.debug("Multi-valued attribute group: {}", group);
				}
			}


		return groups;
	}

}
