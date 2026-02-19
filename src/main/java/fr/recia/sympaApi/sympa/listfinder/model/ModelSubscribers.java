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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Object representing row in model_subscribers table in the SympaRemote database. 
 * @author Eric Groning
 *
 */
@Getter
@Setter
@Entity
@Table(name = "model_subscribers")
public class ModelSubscribers {


  @EmbeddedId
  ModelSubscribersId id;

//	/**
//	 * @return the id
//	 */
//	@Id
//	@Column(name = "id")
//	public BigInteger getId() {
//		return id;
//	}
//
//	/**
//	 * @param id
//	 *            the id to set
//	 */
//	public void setId(BigInteger id) {
//		this.id = id;
//	}
//
//	/**
//	 * @return the groupFilter
//	 */
//	@Column(name = "group_filter")
//	public String getGroupFilter() {
//		return groupFilter;
//	}
//
//	/**
//	 * @param groupFilter
//	 *            the groupFilter to set
//	 */
//	public void setGroupFilter(String groupFilter) {
//		this.groupFilter = groupFilter;
//	}

}
