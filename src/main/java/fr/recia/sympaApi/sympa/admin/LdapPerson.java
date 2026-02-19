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


import fr.recia.sympaApi.config.bean.LdapPersonProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
@Component
public class LdapPerson {

  @Autowired
	private LdapTemplate ldapTemplate;

	@Autowired
  private LdapPersonProperties properties;

  public String getWebmailProfileAttribute() {
    return properties.getWebmailProfileAttribute();
  }

  public String getMemberAttribute() {
    return properties.getMemberAttribute();
  }

  public String getAdminRegex() {
    return properties.getAdminRegex();
  }

  @Getter
  @Setter
  public static class Person {

		private List<String> memberOf = new ArrayList<>();
		private List<String> profile = new ArrayList<>();

  }

	@SuppressWarnings("unchecked")
	public Person getPerson(final String uid) {

		String searchString = this.properties.getPersonSearchString().replace("%UID", uid);

		try {
			List<Person> l = this.ldapTemplate.search(
					searchString, this.properties.getPersonSearchFilter(),
					new AttributesMapper() {
						@Override
						public Object mapFromAttributes(final Attributes attrs)
								throws NamingException {
							Person p = new Person();
							NamingEnumeration<?> o = attrs.get(LdapPerson.this.properties.getMemberAttribute())
									.getAll();
							while (o.hasMoreElements()) {
								p.getMemberOf().add(o.next().toString());
							}

							Attribute profiles = attrs.get(LdapPerson.this.properties.getWebmailProfileAttribute());
							if (profiles != null) {
								o = profiles.getAll();
								if (o!=null) {
									while (o.hasMoreElements()) {
										p.getProfile().add(o.next().toString());
									}
								}

							}

							return p;
						}
					});
			return l.size() == 1 ? l.get(0) : null;

		} catch (Exception ex) {
			log.error("Error during ldap requesting !", ex);
			return null;
		}
	}

}
