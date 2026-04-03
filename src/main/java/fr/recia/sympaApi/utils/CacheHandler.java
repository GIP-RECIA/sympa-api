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
package fr.recia.sympaApi.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.recia.sympaApi.config.bean.CacheProperties;
import fr.recia.sympaApi.exception.CacheNameNotDefinedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.util.annotation.Nullable;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class CacheHandler {



  @Autowired
  CacheManager cacheManager;

  @Autowired
  CacheProperties cacheProperties;

  @Autowired
  ObjectMapper objectMapper;

  Map<String, Cache> caches;

  @PostConstruct
  public void init() {
    caches = new HashMap<>();
    populateCaches(cacheProperties.getAdminServiceCacheName());
    populateCaches(cacheProperties.getLdapFilterRequestCacheName());
    populateCaches(cacheProperties.getSpringCachingSympaAxisServerCacheName());
  }

  private void populateCaches(String cacheName){
    if(Objects.nonNull(cacheName) && !cacheName.trim().isEmpty()){
      Cache cacheFromName = cacheManager.getCache(cacheName);
      if(Objects.isNull(cacheFromName)){
        throw new CacheNameNotDefinedException(cacheName);
      }
      caches.put(cacheName, cacheManager.getCache(cacheName));
    }
  }


  private Cache getCacheFromName(String cacheName){
    if(!caches.containsKey(cacheName)){
      throw new CacheNameNotDefinedException(cacheName);
    }
    return caches.get(cacheName);
  }

  @Nullable
  public <T> T getFromCache(String cacheName, String cacheKey, TypeReference<T> typeRef){

    Cache cache = getCacheFromName(cacheName);

    Cache.ValueWrapper wrapper = cache.get(cacheKey);
    if(Objects.isNull(wrapper)){
      log.debug("No cache value in cache {} with key {}", cacheName, cacheKey);
      return null;
    }

    Object result = wrapper.get();
    if (result == null) {
      return null;
    }
    log.debug("Found cache value in cache {} with key {}", cacheName, cacheKey);

    try {
      return objectMapper.convertValue(result, typeRef);
    } catch (
      IllegalArgumentException e) {
      log.error("Invalid type in cache {} with key {}, evicting value", cacheName, cacheKey, e);
      cache.evict(cacheKey);
      return null;
    }
  }

  public void putObjectInCache(String cacheName, String cacheKey, Object objectToCache) {
    Cache cache = getCacheFromName(cacheName);
    cache.put(cacheKey, objectToCache);
  }





}
