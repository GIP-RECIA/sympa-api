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
package fr.recia.sympaApi.sympa;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

@Slf4j
public class SpringCachingSympaServerAxisWsImpl extends
		CachingSympaServerAxisWsImpl {

	public SpringCachingSympaServerAxisWsImpl() {
		super();
	}


  @PostConstruct
	public void afterPropertiesSet() throws Exception {
    log.info(" SpringCachingSympaServerAxisWsImpl  post construct");
    log.debug(this.toString());
      this.init();

		
	}

	@Override
	public String toString() {
		return "SpringCachingSympaServerAxisWsImpl [getTimeout()=" + getTimeout() + ", getName()=" + getName()
				+ ", getHomeUrl()=" + getHomeUrl() + ", getConnectUrl()=" + getConnectUrl() + ", getAdminUrl()="
				+ getAdminUrl() + ", getNewListUrl()=" + getNewListUrl() + ", getNewListForRoles()="
				+ getNewListForRoles() + "]";
	}
	
}
