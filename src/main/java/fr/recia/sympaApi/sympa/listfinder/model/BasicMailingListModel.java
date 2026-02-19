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
/**
 * 
 */
package fr.recia.sympaApi.sympa.listfinder.model;


import fr.recia.sympaApi.sympa.listfinder.IMailingListModel;
import lombok.Getter;
import lombok.Setter;

/**
 * @author GIP RECIA 2013 - Maxime BOSSARD.
 *
 */
@Getter
@Setter
public class BasicMailingListModel implements IMailingListModel {

	/** id */
	private String id;
	/** nom de la liste */
	private String listname;
	/** description */
	private String description;
	/** pattern de nom de groupe */
	private String groupPatternToMatch;

	/**
	 * Constructor.
	 * 
	 * @param theId id of the model
	 * @param theListname template for the name of the lists which will be created
	 * with the model
	 * @param groupPattern
	 * @param description
	 */
	public BasicMailingListModel(final String theId, final String theListname, final String groupPattern, final String description) {
		this.id = theId;
		this.listname = theListname;
		this.groupPatternToMatch = groupPattern;
		this.description = description;
	}

	/**
	 * @return object toString
	 */
	@Override
	public String toString() {
		return "Model ln: " + this.listname + " desc: " + this.description + " pattern: " + this.groupPatternToMatch;
	}

}
