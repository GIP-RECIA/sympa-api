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
package fr.recia.sympaApi.config;

import fr.recia.sympaApi.config.bean.LdapContextSourceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
public class LdapConfig {

  @Autowired
  LdapContextSourceProperties ldapContextSourceProperties;

  @Bean
  public LdapContextSource contextSource() {
    LdapContextSource contextSource = new LdapContextSource();
    contextSource.setUrl(ldapContextSourceProperties.getUrl());
    contextSource.setUserDn(ldapContextSourceProperties.getUserDn());
    contextSource.setBase(ldapContextSourceProperties.getBase());
    contextSource.setPassword(ldapContextSourceProperties.getPassword());
    contextSource.setPooled(true);
    return contextSource;
  }
}
