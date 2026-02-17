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
package fr.recia.sympaApi.sympa.listfinder.services;


import fr.recia.sympaApi.groupfinder.IEtabGroupsFinder;
import fr.recia.sympaApi.groupfinder.impl.ConcatenateGroupFinder;
import fr.recia.sympaApi.groupfinder.impl.LdapGroupFinder;
import fr.recia.sympaApi.sympa.listfinder.IAvailableListsFinder;
import fr.recia.sympaApi.sympa.listfinder.IExistingListsFinder;
import fr.recia.sympaApi.sympa.listfinder.IListsFromGroupsPatternMatcher;
import fr.recia.sympaApi.sympa.listfinder.IMailingList;
import fr.recia.sympaApi.sympa.listfinder.IMailingListModel;
import fr.recia.sympaApi.sympa.listfinder.model.AvailableMailingListsFound;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Implementation basique du 'module' permettant de sortir la liste des
 * listes de diffusion qu'il est possible de creer pour cet
 * etablissement, à partir des modeles de listes.
 * 
 * Par exemple :
 * 		eleves701 avec le model eleves_classe
 * 		eleves702 avec le model eleves_classe
 * 		parents701 avec le model parents_classe
 * 		parents702 ..
 * 		profs701 ..
 * 		profs702 ..
 * 
 * @author GIP Recia
 *
 */
@Slf4j
@Getter
@Setter
@Service
public class AvailableListsFinderBasicImpl implements IAvailableListsFinder {

	/** Logger */

	/** Le mecanisme de recherche des groupes de l'etablissement injecte par Spring */
  @Autowired
	private LdapGroupFinder etabGroupsFinder;
//  private ConcatenateGroupFinder etabGroupsFinder;


	/** Le mecanisme de recuperation des listes existantes de l'etablissement injecte par Spring */
  @Autowired
	private IExistingListsFinder existingListsFinder;

	/** Le mecanisme permettant de deduire (par pattern) les listes a creer, en fonction des groupes de l'etablissement */
  @Autowired
	private IListsFromGroupsPatternMatcher listsFromGroupsPatternMatcher;

//	/** {@inheritDoc} */
//	@Override
//	public void setExistingListsFinder(final IExistingListsFinder existingListFinder) {
//		this.existingListsFinder = existingListFinder;
//	}

	/**
	 * Retourne les listes qu'il est possible de creer pour cet etablissement a partir
	 * de l'ensemble des modeles connus.
	 * Chaque modele possede un pattern de groupe qui, s'il est respecte, autorise
	 * l'instanciation d'une liste avec ce modele.
	 * (Permet d'obtenir la liste des listes qui seront proposees a l'administrateur d'etablissement)
	 * 
	 * @param modeles les modeles de listes connus, a partir desquels on va deduire les
	 * listes qu'il est possible de creer
	 * @return la collection de mailing lists qu'il est possible de creer
	 * pour cet etablissement
	 */
	@Override
	public AvailableMailingListsFound getAvailableAndNonExistingLists (
			final Map<String,String> userInfo, final Collection<IMailingListModel> modeles) throws Exception {

		AvailableMailingListsFound availableLists = new AvailableMailingListsFound();

		// Create an empty Collection of Mailing Lists
		// => all the lists that could be created by the admin
		Collection<IMailingList> creatableLists = new TreeSet<>();
		Collection<IMailingList> updatableLists = new TreeSet<>();
		availableLists.setCreatableLists(creatableLists);
		availableLists.setUpdatableLists(updatableLists);

		// Get all the groups of the current educational establishment
		Collection<String> groupsOfEtab = this.etabGroupsFinder.findGroupsOfEtab(userInfo);

		AvailableListsFinderBasicImpl.log.debug("Groups found " + groupsOfEtab.size());

		// For each model, we must search for groups that match the model pattern
		// => each time a group matchs the current model's pattern, a new list will be added to the available lists
		// example :
		// the group "esco:etablissement:FICTIF_0450822x:Niveau Seconde:Profs_503'
		// matches the pattern "esco:Etablissements:FICTIF_0450822x:[^:]+:Profs_([\\ -]|\\w+)"
		// so the "profs503" list will be added to the available lists
		Iterator<IMailingListModel> modelesIt = modeles.iterator();
		if (modelesIt != null) {
			while (modelesIt.hasNext()) {
				IMailingListModel currentModel = modelesIt.next();
				Collection<IMailingList> results = this.listsFromGroupsPatternMatcher.findPossibleListsWithModel(
						groupsOfEtab, currentModel);
				AvailableListsFinderBasicImpl.log.debug("Mailing Lists found " + results.size() + " for model [" + currentModel.toString() + "]");
				creatableLists.addAll(results);
			}
		}

		AvailableListsFinderBasicImpl.log.debug("Finding existing lists with userInfo [" + userInfo.toString() + "]");
		Collection<String> existingLists = this.existingListsFinder.findExistingLists(userInfo);
		AvailableListsFinderBasicImpl.log.debug("Existing lists found " + existingLists.size());

		Iterator<IMailingList> itLists = creatableLists.iterator();
		if (itLists != null) {
			while (itLists.hasNext()) {
				IMailingList list = itLists.next();

				// On test si la liste fait partie des listes existantes
				if (existingLists.contains(list.getName().toLowerCase())) {
					updatableLists.add(list);
					itLists.remove();
					AvailableListsFinderBasicImpl.log.debug("List " + list.toString() + " already exists, removing");
				}
			}
		}

		AvailableListsFinderBasicImpl.log.debug("Available lists count " + creatableLists.size());
		return availableLists;
	}
}
