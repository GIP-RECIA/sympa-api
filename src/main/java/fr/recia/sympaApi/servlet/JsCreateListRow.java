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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class used to represent a row representing a 
 * group that can be subscribed to a list in the create list screen.
 * 
 * @author Eric Groning
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class JsCreateListRow {
	//Name of the group
	String name;
	
	//True if the group will be part of the mailing list
	Boolean checked;
	
	//True if the user can toggle the checked value
	Boolean editable;
	
	//Database id of the group
	String idRequest;

}
