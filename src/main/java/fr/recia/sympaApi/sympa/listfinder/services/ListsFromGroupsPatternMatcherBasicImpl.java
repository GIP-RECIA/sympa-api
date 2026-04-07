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



import fr.recia.sympaApi.sympa.listfinder.IListsFromGroupsPatternMatcher;
import fr.recia.sympaApi.sympa.listfinder.IMailingList;
import fr.recia.sympaApi.sympa.listfinder.IMailingListModel;
import fr.recia.sympaApi.sympa.listfinder.model.BasicMailingList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * @see IListsFromGroupsPatternMatcher
 * 
 * @author GIP Recia
 *
 */

@Getter
@Setter
@Slf4j
@Service
public class ListsFromGroupsPatternMatcherBasicImpl implements
  IListsFromGroupsPatternMatcher {

	/**
	 * @see IListsFromGroupsPatternMatcher#findPossibleListsWithModel(Collection, IMailingListModel)
	 */
	public Collection<IMailingList> findPossibleListsWithModel(
			final Collection<String> groups, final IMailingListModel listModel) {
		Collection<IMailingList> theLists = new ArrayList<IMailingList>();
		String casePattern = listModel.getGroupPatternToMatch();
		ListsFromGroupsPatternMatcherBasicImpl.log.debug("List model pattern is set to " + casePattern);
    Pattern patternToMatch = Pattern.compile(casePattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		ListsFromGroupsPatternMatcherBasicImpl.log.debug("Groups to filter size " + groups.size());

    String argument = null;
    for (String group : groups) {
      Matcher matcher = patternToMatch.matcher(group);
      if (matcher.matches()) {
        // Si le group courant respecte le pattern, on ajoute la liste de diffusion
        // correspondante à la liste des listes possibles pour cet etablissement
        if (matcher.groupCount() == 1) {
          // Si un parametre a ete recuperer, il faudra utiliser ce parametre pour instancier le modele
          // (par exemple la classe ou le niveau correspondant)
          // group(0) = la chaine complete
          // group(1) = le parametre (= le premier groupe)
          argument = matcher.group(1);
        }
        IMailingList list = new BasicMailingList(listModel, argument);
        theLists.add(list);
        ListsFromGroupsPatternMatcherBasicImpl.log.debug("Group [" + group + "] matched");
        // Si aucun parametre n'est recupere, c'est qu'il n'y aura aucun parametre a passer au modele
      } else {
        ListsFromGroupsPatternMatcherBasicImpl.log.debug("Group [" + group + "] did not match");
      }
    }
		return theLists;
	}

}
