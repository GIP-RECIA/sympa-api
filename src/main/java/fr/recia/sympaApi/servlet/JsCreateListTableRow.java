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
package fr.recia.sympaApi.servlet;


import fr.recia.sympaApi.pojo.UserSympaListWithUrl;

/**
 * Represents a row in the table of lists that can be created
 * @author Eric Groning
 *
 */
public class JsCreateListTableRow {
	//Name of the list (email address)
	String name;
	
	//Subject of list
	String subject;
	
	//Model id
	String modelId;
	
	//Name of the model parameter 
	String modelParam;
	
	UserSympaListWithUrl urls;
	
	
	
	/**
	 * @return the modelParam
	 */
	public String getModelParam() {
		return modelParam;
	}
	/**
	 * @param modelParam the modelParam to set
	 */
	public void setModelParam(String modelParam) {
		this.modelParam = modelParam;
	}
	/**
	 * @return the modelId
	 */
	public String getModelId() {
		return modelId;
	}
	/**
	 * @param modelId the modelId to set
	 */
	public void setModelId(String modelId) {
		this.modelId = modelId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}

	public UserSympaListWithUrl getUrls() {
		return urls;
	}

	public void setUrls(UserSympaListWithUrl urls) {
		this.urls = urls;
	}
	@Override
	public String toString() {
		return "JsCreateListTableRow [name=" + name + ", subject=" + subject + ", modelId=" + modelId + ", modelParam="
				+ modelParam + ", urls=" + (urls != null ? urls.toString(): "null") + "]";
	}
}
