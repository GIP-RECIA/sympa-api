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
package fr.recia.sympaApi.groupfinder.impl;


import fr.recia.sympaApi.groupfinder.IEtabGroupsFinder;
import fr.recia.sympaApi.sympa.admin.UserAttributeMapping;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Class used to filter a list of groups based on a regular expression.
 *
 */
public class RegexGroupFinder implements IEtabGroupsFinder {

	/** configured via spring with connection info and baseDN. */
	private IEtabGroupsFinder groupsFinder;

	/** [^:]*:Establishements:[^:]*_%UAI:*. */
	private String regularExpressionFilter;

	/** User attributes mapping. */
	private UserAttributeMapping userAttributeMapping;

	public Collection<String> findGroupsOfEtab(final Map<String, String> userInfo) {

		Collection<String> groups = this.groupsFinder.findGroupsOfEtab(userInfo);

		String regex = this.userAttributeMapping.substitutePlaceholder(this.regularExpressionFilter, userInfo);

		for (Iterator<String> iterator = groups.iterator(); iterator.hasNext();) {
			String name = iterator.next();

			if (!name.matches(regex)) {
				iterator.remove();
				continue;
			}
		}

		return groups;
	}

	public IEtabGroupsFinder getGroupsFinder() {
		return this.groupsFinder;
	}

	public void setGroupsFinder(final IEtabGroupsFinder groupsFinder) {
		this.groupsFinder = groupsFinder;
	}

	/**
	 * @return the regularExpressionFilter
	 */
	public String getRegularExpressionFilter() {
		return this.regularExpressionFilter;
	}

	/**
	 * @param regularExpressionFilter the regularExpressionFilter to set
	 */
	public void setRegularExpressionFilter(final String regularExpressionFilter) {
		this.regularExpressionFilter = regularExpressionFilter;
	}

	public UserAttributeMapping getUserAttributeMapping() {
		return this.userAttributeMapping;
	}

	public void setUserAttributeMapping(final UserAttributeMapping userAttributeMapping) {
		this.userAttributeMapping = userAttributeMapping;
	}

}
