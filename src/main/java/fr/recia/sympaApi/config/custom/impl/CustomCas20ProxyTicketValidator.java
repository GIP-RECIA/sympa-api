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
import org.apereo.cas.client.validation.Cas20ProxyTicketValidator;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

public class CustomCas20ProxyTicketValidator extends Cas20ProxyTicketValidator {

    public CustomCas20ProxyTicketValidator(String casServerUrlPrefix, CasProperties casProperties) {
        super(casServerUrlPrefix);
        this.casProperties = casProperties;
    }

    private final CasProperties casProperties;

    @Override
    protected void populateUrlAttributeMap(final Map<String, String> urlParameters) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        final String url = request.getRequestURL().toString();
        final String uri = request.getRequestURI();

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

        logger.info("urlParameters map {}", urlParameters.entrySet().toString() );

        //todo remove
      //  urlParameters.put("service", urlParameters.get("service").replace("http", "https"));

        //todo replace
        urlParameters.put("pgtUrl", baseUrl
               // .replace("http","https")
                + this.getProxyCallbackUrl());
    }
}
