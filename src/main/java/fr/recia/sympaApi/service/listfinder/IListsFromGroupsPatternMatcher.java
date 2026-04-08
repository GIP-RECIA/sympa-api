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
package fr.recia.sympaApi.service.listfinder;



import fr.recia.sympaApi.model.IMailingList;
import fr.recia.sympaApi.model.IMailingListModel;

import java.util.Collection;

/**
 * 
 * Interface definissant le mecanisme qui permet de deduire (par pattern matching)
 * les mailing lists qu'il est possible de creer, en fonction des groupes de l'etablissement,
 * pour un model de liste donne
 * 
 * @author GIP Recia
 *FzD
 */
public interface IListsFromGroupsPatternMatcher {

	/**
	 * Trouve, a partir des groupes de l'etablissement, toute les listes
	 * qu'il est possible de creer avec un modele donne
	 * @param groups la liste des groupes de l'etablissement concerne
	 * @param model le model concerne
	 * @return une liste de mailing lists (toute les listes
	 * qu'il est possible de creer avec ce modele, pour l'etablissement)
	 */
	public Collection<IMailingList> findPossibleListsWithModel(Collection<String> groups, IMailingListModel model);
}
