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

import fr.recia.sympaApi.sympa.listfinder.IMailingList;
import fr.recia.sympaApi.sympa.listfinder.IMailingListModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * Implementation d'une mailing list
 * le nom de la liste se construit a partir du modele de mailing list
 *
 * @author GIP Recia
 *
 */
@Getter
@Setter
public class BasicMailingList implements IMailingList, Comparable<BasicMailingList> {

	/** Nom de la mailing liste	 */
	private String name;

	/** Description de la mailing List */
	private String description;

	/** Model mailing list */
	private IMailingListModel model;

	/** Paramètre du modèle */
	private String modelParameter;

	/**
	 * Constructor.
	 * 
	 * @param modelToUse the parent model.
	 * @param modelParam the param
	 */
	public BasicMailingList(final IMailingListModel modelToUse, final String modelParam) {
		this.model = modelToUse;
		this.modelParameter = modelParam;
		String listName = this.replaceModelParamToken(this.model.getListname());
		listName = this.stripLdapSpecialChars(listName);
		this.name = this.stripEmailSpecialChars(listName);
		this.description = this.replaceModelParamToken(this.model.getDescription());
	}

	@Override
	public String toString() {
		return "BasicMailingList [name=" + this.name + ", description=" + this.description + "]";
	}

	/**
	 * Strip all Email special chars.
	 * 
	 * @param text the text to strip
	 * @return a striped text
	 */
	protected String stripEmailSpecialChars(final String text) {
		String result = null;

		if (StringUtils.hasText(text)) {
			// Strip forbid email chars
			result = text.replaceAll("[^\\w!#\\$%&'\\*\\+\\-/=\\?\\^_`\\{\\|\\}~\\.]", "");
			// Strip multiple dot
			result = result.replaceAll("[\\.]{2,}", "");
		}

		return result;
	}

	/**
	 * Strip all Ldap special chars.
	 * 
	 * @param text the text to strip
	 * @return a striped text
	 */
	protected String stripLdapSpecialChars(final String text) {
		String result = null;

		if (StringUtils.hasText(text)) {
			// Strip parenthesis content
			result = text.replaceAll("\\(.*\\)", "");
			// Strip ldap special chars ( ) \ * NUL
			result = result.replaceAll("[\\(\\)\\*\\\\\\x00]+", "");
			// Replace spaces by underscore
			result = result.trim();
			result = result.replaceAll("[\\s]+", "_");
		}

		return result;
	}

	/**
	 * @param text A string
	 * @return The string with tokens matching {...} tokens {UAI} is left alone as it is a reserved token
	 */
	protected String replaceModelParamToken(final String text) {
		return (this.modelParameter == null) || (text == null) ? text : text.replaceAll("\\{((?!UAI).)*\\}", this.modelParameter);
	}


	/** {@inheritDoc} */
	public int compareTo(final BasicMailingList list) {
		return this.getName().compareTo(list.getName());
	}
}
