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

import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
@Profile("!test")

public class CustomCas20ProxyTicketValidator extends Cas20ProxyTicketValidator {

    public CustomCas20ProxyTicketValidator(String casServerUrlPrefix) {
        super(casServerUrlPrefix);
    }

    @Override
    protected void populateUrlAttributeMap(final Map<String, String> urlParameters) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        final String url = request.getRequestURL().toString();
        final String uri = request.getRequestURI();
        urlParameters.put("pgtUrl", url.substring(0, url.length() - uri.length()) + this.getProxyCallbackUrl());
    }
}
