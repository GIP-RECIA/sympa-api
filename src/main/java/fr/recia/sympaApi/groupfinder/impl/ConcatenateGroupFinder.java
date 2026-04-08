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

import fr.recia.sympaApi.config.bean.ConcatenateGroupFinderProperties;
import fr.recia.sympaApi.groupfinder.IEtabGroupsFinder;
import fr.recia.sympaApi.service.UserAttributeMapping;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Concatenate the results of multiple groups finders.
 * 
 * @author GIP - RECIA Maxime BOSSARD.
 *
 */
@Getter
@Setter
@NoArgsConstructor
@Service
public class ConcatenateGroupFinder implements IEtabGroupsFinder {

	/** Configured via spring. */
	private List<IEtabGroupsFinder> groupsFinders;

  @Autowired
  RegexGroupFinder regexGroupFinder;

  @Autowired
  ConcatenateGroupFinderProperties properties;

  @Autowired
  LdapTemplate ldapTemplate;

  @Autowired
  UserAttributeMapping userAttributeMapping;


  @PostConstruct
  private void postConstruct() {
    groupsFinders = new ArrayList<>();
    for(Map<String, String> mapping: properties.getMapping()){
      MultiValuedAttributeGroupFinder multiValuedAttributeGroupFinder = new MultiValuedAttributeGroupFinder(mapping, this.ldapTemplate, this.userAttributeMapping, this.properties.getLdapSearchBaseDN());
      groupsFinders.add(multiValuedAttributeGroupFinder);
    }
    groupsFinders.add(regexGroupFinder);
  }

	/** {@inheritDoc} */
	public Collection<String> findGroupsOfEtab(final Map<String, String> userInfo) {

		Collection<String> groups = new ArrayList<String>();
		if (!CollectionUtils.isEmpty(this.groupsFinders)) {
			for (IEtabGroupsFinder groupsFinder : this.groupsFinders) {
				Collection<String> etabGroupes = groupsFinder.findGroupsOfEtab(userInfo);
				if (etabGroupes != null) {
					groups.addAll(etabGroupes);
				}
			}
		}

		return groups;
	}

}
