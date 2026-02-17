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
package fr.recia.sympaApi.groupfinder;

import java.util.Collection;
import java.util.Map;

/**
 * Interface definissant le mecanisme capable de recuperer tous les groupes
 * de l'etablissement concerne, en utilisant les infos passes en parametre
 * 
 * @author GIP Recia
 * 
 */
public interface IEtabGroupsFinder {

	/**
	 * Retourne une collection contenant les noms des groupes de l'etablissement
	 * @param userInfo Les informations utilisateur
	 * @return collection contenant les noms des groupes de l'etablissement
	 */
	public Collection<String> findGroupsOfEtab(Map<String,String> userInfo);
}
