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
package fr.recia.sympaApi.config.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;

@ConfigurationProperties(prefix = "redis")
@Data
@Validated
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class RedisProperties {

    @NotNull
    private String hostName;

    @NotNull
    private int port;

    @NotNull
    private String userName;

    @NotNull
    private String password;

    @NotNull
    private int databaseIndex;

    private String pgtiouPrefix;

    private int pgtiouExpiryInSeconds = 120;


    private String responseCacheName = "";

    private int responseCacheTtlInSeconds = 360;

    private String uaiToImapCacheName = "";

    private int uaiToImapCacheTtlInSeconds = 360;

  private String mappingPrefix;
  private String cachePrefix;

    @PostConstruct
    public void setupAndDebug() {

        if(pgtiouExpiryInSeconds <= 0){
            pgtiouExpiryInSeconds = 10;
            log.warn("pgtiouExpiryInSeconds value too low, defaulted to 10");
        }
        if(databaseIndex < 0){
            databaseIndex = 0;
            log.warn("Negative database index provided, defaulted to 0");
        }
        log.info("RedisProperties {}", this);
    }

    @Override
    public String toString() {
        return "RedisProperties{" +
                "hostName='" + hostName + '\'' +
                ", port=" + port +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", databaseIndex=" + databaseIndex +
                ", pgtiouPrefix='" + pgtiouPrefix + '\'' +
                ", pgtiouExpiryInSeconds=" + pgtiouExpiryInSeconds +
                ", responseCacheName='" + responseCacheName + '\'' +
                ", responseCacheTtlInSeconds=" + responseCacheTtlInSeconds +
                ", uaiToIampCacheName='" + uaiToImapCacheName + '\'' +
                ", uaiToIampCacheTtlInSeconds=" + uaiToImapCacheTtlInSeconds +
                ", mappingPrefix=" + mappingPrefix +
                ", cachePrefix=" + cachePrefix +
                '}';
    }
}
