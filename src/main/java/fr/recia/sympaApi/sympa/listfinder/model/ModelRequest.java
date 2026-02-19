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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Object representing row in j_model_request table in the SympaRemote database. 
 * @author Eric Groning
 *
 */
@Getter
@Setter
@Entity
@Table(name = "j_model_request")
public class ModelRequest {
	
	@EmbeddedId
	ModelRequestId id;

  @Column(name = "category", columnDefinition="enum('MANDATORY','UNCHECKED','CHECKED')")
  String category;
	
	public enum ModelRequestRequired {
    	MANDATORY,
    	UNCHECKED,
    	CHECKED
    }
	
	
//
//	/**
//	 * @return the id
//	 */
//	public ModelRequestId getId() {
//		return id;
//	}
//	/**
//	 * @param id the id to set
//	 */
//	public void setId(ModelRequestId id) {
//		this.id = id;
//	}
//	/**
//	 * @return the category
//	 */
//	@Column(name = "category", columnDefinition="enum('MANDATORY','UNCHECKED','CHECKED')")
//	public String getCategory() {
//		return category;
//	}
//
	public ModelRequestRequired getCategoryAsEnum() {
		return ModelRequestRequired.valueOf(category);
	}
//	/**
//	 * @param category the category to set
//	 */
//	public void setCategory(String category) {
//		this.category = category;
//	}
//
//	/**
//	 * @return the idModel
//	 */
//	public BigInteger getIdModel() {
//		return id.idModel;
//	}
//	/**
//	 * @param idModel the idModel to set
//	 */
//	public void setIdModel(BigInteger idModel) {
//		id.idModel = idModel;
//	}
//	/**
//	 * @return the idRequest
//	 */
//	public BigInteger getIdRequest() {
//		return id.idRequest;
//	}
//	/**
//	 * @param idRequest the idRequest to set
//	 */
//	public void setIdRequest(BigInteger idRequest) {
//		this.id.idRequest = idRequest;
//	}
	
	
	
}


