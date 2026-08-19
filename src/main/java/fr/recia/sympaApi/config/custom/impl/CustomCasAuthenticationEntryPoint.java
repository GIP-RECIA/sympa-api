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


import fr.recia.sympaApi.config.bean.CasProperties;
import fr.recia.sympaApi.exception.InvalidDomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@Slf4j
public class CustomCasAuthenticationEntryPoint extends CasAuthenticationEntryPoint {

    public CustomCasAuthenticationEntryPoint(CasProperties casProperties) {
        this.casProperties = casProperties;
    }

    private final CasProperties casProperties;

    @Override
    protected String createServiceUrl(HttpServletRequest request, HttpServletResponse response) {

        String host = request.getHeader("X-Forwarded-Host");

        if (host == null) {
            host = request.getServerName();
        }

        if (!casProperties.getAuthorizedDomains().contains(host)) {
            throw new InvalidDomainException("Invalid domain: " + host);
        }

        String baseUrl = ServletUriComponentsBuilder
                .fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();

        //todo mettre au propre
        return baseUrl
             //   .replace("http", "https")
                + casProperties.getCasServiceId();
    }

}
