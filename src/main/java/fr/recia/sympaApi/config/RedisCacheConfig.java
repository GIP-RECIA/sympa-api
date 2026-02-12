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
package fr.recia.sympaApi.config;

import fr.recia.sympaApi.config.bean.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

  @Bean
  public RedisCacheManager cacheManager(
    RedisConnectionFactory connectionFactory,
    RedisProperties redisProperties) {

    //  default TTL
    // TODO put durations in conf
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
      .computePrefixWith(cacheName -> redisProperties.getCachePrefix() + "::" + cacheName + "::")
      .entryTtl(Duration.ofMinutes(10));

    // TTL per key TODO : choose value for each key
    Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
    cacheConfigs.put("sympaServerCache", defaultConfig.entryTtl(Duration.ofMinutes(30)));
//    cacheConfigs.put("key1", defaultConfig.entryTtl(Duration.ofMinutes(5)));
 //   cacheConfigs.put("key2", defaultConfig.entryTtl(Duration.ofHours(1)));

    return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(defaultConfig)
      .withInitialCacheConfigurations(cacheConfigs)
      .build();
  }
}
