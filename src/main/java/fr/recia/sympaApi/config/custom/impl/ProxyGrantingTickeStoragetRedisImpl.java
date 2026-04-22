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
package fr.recia.sympaApi.config.custom.impl;

import fr.recia.sympaApi.config.bean.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j @Profile("!test")

public class ProxyGrantingTickeStoragetRedisImpl implements ProxyGrantingTicketStorage {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisProperties redisProperties;

    @Override
    public void save(final String proxyGrantingTicketIou, final String proxyGrantingTicket) {
        log.debug("Saving ProxyGrantingTicketIOU and ProxyGrantingTicket combo: [{}, {}]", proxyGrantingTicketIou.substring(0,8),
                proxyGrantingTicket.substring(0,8));
        saveInRedis(proxyGrantingTicketIou, proxyGrantingTicket);
    }

    /**
     * NOTE: you can only retrieve a ProxyGrantingTicket once with this method.
     * Its removed after retrieval.
     */
    @Override
    public String retrieve(final String proxyGrantingTicketIou) {
        if (CommonUtils.isBlank(proxyGrantingTicketIou)) {
            return null;
        }

        final String proxyGrantingTicket = getFromRedis(proxyGrantingTicketIou);

        if (proxyGrantingTicket == null) {
            log.debug("No Proxy Ticket found for [{}].", proxyGrantingTicketIou);
            return null;
        }

        log.debug("Returned ProxyGrantingTicket of [{}]", proxyGrantingTicket);
        return proxyGrantingTicket;
    }

    @Override
    public void cleanUp() {
        log.warn("Redis does not require cleanup for PGT, entries have ttl in Redis");
    }

    public void saveInRedis(String Iou, String pgtIou) {
        redisTemplate.opsForValue().set(String.format("%1$s:%2$s",  redisProperties.getPgtiouPrefix(), Iou), pgtIou, redisProperties.getPgtiouExpiryInSeconds(), TimeUnit.SECONDS);
    }

    public String getFromRedis(String Iou){
        return redisTemplate.opsForValue().getAndDelete(String.format("%1$s:%2$s",  redisProperties.getPgtiouPrefix(), Iou));
    }
}


