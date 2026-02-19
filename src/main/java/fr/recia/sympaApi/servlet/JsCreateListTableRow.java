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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a row in the table of lists that can be created
 * @author Eric Groning
 *
 */
@Getter
@Setter
@NoArgsConstructor
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


  @Override
	public String toString() {
		return "JsCreateListTableRow [name=" + name + ", subject=" + subject + ", modelId=" + modelId + ", modelParam="
				+ modelParam + ", urls=" + (urls != null ? urls.toString(): "null") + "]";
	}
}
