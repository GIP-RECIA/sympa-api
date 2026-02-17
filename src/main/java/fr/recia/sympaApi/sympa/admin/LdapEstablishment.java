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

import fr.recia.sympaApi.config.bean.LdapEstablishmentProperties;
import fr.recia.sympaApi.utils.LdapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@Component
public class LdapEstablishment {

  @Autowired
	private LdapTemplate ldapTemplate;


  @Autowired
  private LdapEstablishmentProperties properties;



//	private String defaultSiren;




	/**
	 * @return the ldapTemplate
	 */
	public LdapTemplate getLdapTemplate() {
		return this.ldapTemplate;
	}

	public void setLdapTemplate(final LdapTemplate ldapTemplate) {
		this.ldapTemplate = ldapTemplate;
	}

	public static class Person {
		private List<String> memberOf = new ArrayList<String>();
		private List<String> profile = new ArrayList<String>();


		public List<String> getProfile() {
			return this.profile;
		}

		public void setProfile(final List<String> profile) {
			this.profile = profile;
		}

		public List<String> getMemberOf() {
			return this.memberOf;
		}

		public void setMemberOf(final List<String> memberOf) {
			this.memberOf = memberOf;
		}

	}

	public String getSiren(final String uai) {
		String result = null;

		String searchString = StringUtils.replace(this.properties.getEstSearchString(), "%UAI", uai);
		String searchFilter = StringUtils.replace(this.properties.getEstSearchFilter(), "%UAI", uai);

		log.debug("Searching for siren for establishment ["
				+ uai + "] with searchString " + searchString + " and searchfilter " + searchFilter);

		AttributesMapper domainMapper = new AttributesMapper() {
			@Override
			public Object mapFromAttributes(final Attributes attrs)
					throws NamingException {
				Attribute attr = attrs.get(LdapEstablishment.this.getProperties().getSirenAttribute());
				Object result = null;
				if (attr != null) {
					result = attr.get();
				}
				return result;
			} };

			List<String> l = LdapUtils.ldapSearch(this.ldapTemplate, searchFilter,
					searchString, domainMapper);
			if ((l == null) || (l.size() != 1)) {
				log.debug("LDAP Establishement siren search "
						+ "did not return anything");
			} else {
				result = l.iterator().next();
				log.debug("LDAP Establishement siren search returned " + result);
			}

			return result;
	}

}
