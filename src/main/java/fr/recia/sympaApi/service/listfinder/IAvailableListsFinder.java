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


import fr.recia.sympaApi.model.IMailingListModel;
import fr.recia.sympaApi.model.AvailableMailingListsFound;

import java.util.Collection;
import java.util.Map;

/**
 * 
 * Interface definissant l'objet capable de determiner quelles sont
 * les listes de diffusion pouvant etre crees par l'administrateur.
 * 
 * @author Maxime BOSSARD - Gip Recia 2013
 * 
 */
public interface IAvailableListsFinder {

	/**
	 * Methode retournant le resultat final, c'est a dire la liste des mailing lists
	 * qu'il est possible de creer dans l'interface, en s'assurant de ne pas proposer celles deja existantes.
	 * 
	 * @param userInfo les informations utilisateur
	 * @param modeles les modeles de listes connus
	 * @return une collection de mailing lists
	 */
	public AvailableMailingListsFound getAvailableAndNonExistingLists(Map<String,String> userInfo, Collection<IMailingListModel> modeles) throws Exception;

}
