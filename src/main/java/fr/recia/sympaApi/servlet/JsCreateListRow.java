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

/**
 * Class used to represent a row representing a 
 * group that can be subscribed to a list in the create list screen.
 * 
 * @author Eric Groning
 *
 */
public class JsCreateListRow {
	//Name of the group
	String name;
	
	//True if the group will be part of the mailing list
	Boolean checked;
	
	//True if the user can toggle the checked value
	Boolean editable;
	
	//Database id of the group
	String idRequest;

	/**
	 * @return the idRequest
	 */
	public String getIdRequest() {
		return idRequest;
	}

	/**
	 * @param idRequest
	 *            the idRequest to set
	 */
	public void setIdRequest(String idRequest) {
		this.idRequest = idRequest;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the checked
	 */
	public Boolean getChecked() {
		return checked;
	}

	/**
	 * @param checked
	 *            the checked to set
	 */
	public void setChecked(Boolean checked) {
		this.checked = checked;
	}

	/**
	 * @return the editable
	 */
	public Boolean getEditable() {
		return editable;
	}

	/**
	 * @param editable
	 *            the editable to set
	 */
	public void setEditable(Boolean editable) {
		this.editable = editable;
	}
}
