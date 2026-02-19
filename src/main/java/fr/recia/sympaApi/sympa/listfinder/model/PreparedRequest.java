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
package fr.recia.sympaApi.sympa.listfinder.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigInteger;

/**
 * Object representing row in prepared_request table in the SympaRemote database. 
 * @author Eric Groning
 *
 */
@Getter
@Setter
@Entity
@Table(name = "prepared_request")
public class PreparedRequest {

  @Id
	@Column(name = "id_request")
	BigInteger id;

  @Column(name = "display_name")
	String displayName;
	
	@Column(name = "ldapfilter")	
	String ldapFilter;


  @Column(name = "data_source")
	String dataSource;
	
	@Column(name = "ldap_suffix")	
	String ldapSuffix;

}
