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

import fr.recia.sympaApi.pojo.SympaList;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.service.DomainService;
import fr.recia.sympaApi.sympa.listfinder.IExistingListsFinder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Existing list finder implementation responsible for querying Sympa to find
 * the list already existing.
 * 
 * @author GIP RECIA 2013 - Maxime BOSSARD.
 *
 */

@Getter
@Setter
@Slf4j
@Service
public class SympaExistingListFinder implements IExistingListsFinder {

  @Autowired
	private DomainService domainService;

	/** {@inheritDoc} */
	public Collection<String> findExistingLists(final Map<String, String> userInfo) throws Exception {
		log.debug("Finding existing lists...");

		List<SympaList> lists = this.domainService.getLists();

		log.debug("Total lists returned : " + lists.size());

		List<String> existingLists = new ArrayList<>();

		for (SympaList list : lists) {
			String[] address = list.getAddress().split("@");
			if (address.length != 2) {
				log.warn("Unexpected address, @ not found : " + list.getAddress());
				continue;
			}

			final String namePart = address[0];
			@SuppressWarnings("unused")
			final String domainPart = address[1];

			if (log.isDebugEnabled()) {
				log.debug(String.format(
						"Adding existing list %1$s ...", namePart));
			}
			existingLists.add(namePart.toLowerCase());

		}
		return existingLists;

	}
}
