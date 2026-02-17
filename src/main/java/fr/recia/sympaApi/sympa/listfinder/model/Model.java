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
 * Object representing row in model table in the SympaRemote database. 
 * @author Eric Groning
 *
 */
@Getter
@Setter
@Entity
@Table(name = "model")
public class Model {

	@Id
	@Column(name = "id", insertable=false, updatable=false)
	private BigInteger id;
	@Column(name = "modelname")
	private String modelName;
	@Column(name = "listname")
	private String listname;
	@Column(name = "description")
	private String description;
	@Column(name = "subject")
	private String subject;
	@Column(name = "family")
	private String family;
	@Column(name = "need_parameter")	
	private Boolean needParameter;
	@Column(name = "pattern")
	private String pattern;


}
