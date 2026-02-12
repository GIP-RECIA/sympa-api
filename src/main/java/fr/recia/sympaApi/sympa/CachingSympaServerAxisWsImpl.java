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

import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Objects;

@Slf4j
public class CachingSympaServerAxisWsImpl extends SympaServerAxisWsImpl {

  @Autowired
	private CacheManager cacheManager = null;
	
	private Cache cache = null;

  public void init() {
    log.info(" CachingSympaServerAxisWsImpl init");

    cache = cacheManager.getCache("sympaServerCache");
  }

	private String cacheName = CachingSympaServerAxisWsImpl.class.getName();
	
	public CachingSympaServerAxisWsImpl() {
		super();
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public List<UserSympaListWithUrl> getWhich() {
		// cacheKey = serverInstance/methodName/useridentifier

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    //TODO retirer après test

    assert !"".equals(authentication.getName());

		String cacheKey = String.format("%1$s;%2$s;%3$s", getName(),"getWhich",authentication.getName());
    log.debug("cache key = "+cacheKey);
		Object cached = getCachedValue(cacheKey);
		if (cached != null) return (List<UserSympaListWithUrl>)cached;
		List<UserSympaListWithUrl> result = super.getWhich();
		setCachedValue(cacheKey,result);
		return result;
	}

  private void setCachedValue(String cacheKey, Object toCache) {
    // todo re enable cache after test
//    log.info(" setCachedValue cache is null ? = {}", Objects.isNull(cache));
//    cache.put(cacheKey, toCache);
  }

  private Object getCachedValue(String cacheKey) {

    log.info(" getCachedValue cache is null ? = {}", Objects.isNull(cache));

    Cache.ValueWrapper wrapper = cache.get(cacheKey);
    if (wrapper == null) {
      log.debug("no cache value for key {}", cacheKey);
      return null;
    }
    Object result = wrapper.get();
    log.debug("having cached value for key {}", cacheKey);
    return result;
  }
	
	/**
	 * @return the cacheManager
	 */
	public CacheManager getCacheManager() {
		return cacheManager;
	}

	/**
	 * @param cacheManager the cacheManager to set
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * @return the cacheName
	 */
	public String getCacheName() {
		return cacheName;
	}

	/**
	 * @param cacheName the cacheName to set
	 */
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}
}
