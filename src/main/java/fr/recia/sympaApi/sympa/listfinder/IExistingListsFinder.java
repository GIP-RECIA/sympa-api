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
package fr.recia.sympaApi.sympa.listfinder;

import java.util.Collection;
import java.util.Map;

/**
 * 
 * @author GIP Recia
 *
 * Interface definissant le mecanisme capable de recuperer toutes les listes existantes
 * (en interrogeant le robot sympa concerne via le webservice sympa par exemple...)
 */
public interface IExistingListsFinder {

	/**
	 * Retourne une collection contenant les noms des listes existantes.
	 * Le nom de la liste est situé à gauche du @.
	 * Pour une liste "liste1@recia.fr" le nom de la liste est "liste1".
	 * 
	 * @param userInfo les informations utilisateur
	 * @return collection contenant les noms des listes existantes au niveau de sympa pour l'etablissement
	 */
	Collection<String> findExistingLists(Map<String,String> userInfo) throws Exception;
}
