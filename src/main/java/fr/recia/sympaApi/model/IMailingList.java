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
package fr.recia.sympaApi.model;

/**
 * Interface d'un objet MailingList, afin d'autoriser differentes implementations.
 *
 * @author GIP Recia
 *
 */
public interface IMailingList {

	/**
	 * Return the name of the mailing list.
	 * 
	 * @return the name of the mailing list
	 */
	String getName();

	/**
	 *
	 * @return The description of the mailing list
	 */
	String getDescription();

	/**
	 * Return the model of the mailing list.
	 * 
	 * @return the model of the mailing list
	 */
	IMailingListModel getModel();

	/**
	 * Return the modelParameter.
	 * 
	 * @return the modelParameter (ex. Classe, Niveau...)
	 */
	String getModelParameter();
}
