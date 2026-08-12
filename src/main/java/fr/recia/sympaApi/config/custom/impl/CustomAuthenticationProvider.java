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

import fr.recia.sympaApi.config.bean.AppConfProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.client.validation.Assertion;
import org.apereo.cas.client.validation.TicketValidationException;
import org.apereo.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.cas.authentication.CasServiceTicketAuthenticationToken;
import org.springframework.security.cas.authentication.NullStatelessTicketCache;
import org.springframework.security.cas.authentication.StatelessTicketCache;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class CustomAuthenticationProvider implements AuthenticationProvider, InitializingBean, MessageSourceAware {

    private AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService;
    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();
    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private StatelessTicketCache statelessTicketCache = new NullStatelessTicketCache();
    private String key;
    private TicketValidator ticketValidator;
    private ServiceProperties serviceProperties;
    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
    private String SPRING_SECURITY_SERVICE_URL_ATTR = "SPRING_SECURITY_SERVICE_URL_ATTR";

    private final AppConfProperties appConfProperties;


    public CustomAuthenticationProvider(AppConfProperties appConfProperties) {
        log.info("IN CONTROLLER CustomAuthenticationProvider");
        this.appConfProperties = appConfProperties;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.authenticationUserDetailsService, "authenticationUserDetailsService must be set");
        Assert.notNull(this.ticketValidator, "ticketValidator must be set");
        Assert.notNull(this.statelessTicketCache, "statelessTicketCache must be set");
        Assert.hasText(this.key, "A key is required for CasAuthenticationToken identification");
        Assert.notNull(this.messages, "A message source must be set");
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        log.info("AUTHENTICATE METHOD");
        if (!supports(authentication.getClass())) {
            return null;
        }

        if (authentication instanceof UsernamePasswordAuthenticationToken
                && authentication.isAuthenticated()) {
            return null;
        }

        if (authentication instanceof CasAuthenticationToken casAuth) {
            if (this.key.hashCode() != casAuth.getKeyHash()) {
                throw new BadCredentialsException(
                        this.messages.getMessage(
                                "CasAuthenticationProvider.incorrectKey",
                                "The presented CasAuthenticationToken does not contain the expected key"
                        )
                );
            }
            return authentication;
        }

        if (authentication.getCredentials() == null
                || "".equals(authentication.getCredentials())) {
            throw new BadCredentialsException(
                    this.messages.getMessage(
                            "CasAuthenticationProvider.noServiceTicket",
                            "Failed to provide a CAS service ticket to validate"
                    )
            );
        }

        CasAuthenticationToken result;

        String ticket = authentication.getCredentials().toString();

        result = this.statelessTicketCache.getByTicketId(ticket);

        log.info("RESULT IS "+result);
        if (result == null) {
            result = this.authenticateNow(authentication);
            result.setDetails(authentication.getDetails());
            this.statelessTicketCache.putTicketInCache(result);
        }

        return result;
    }

    /**
     * Authentifie l'utilisateur en validant le ticket CAS et en chargeant les
     * UserDetails.
     */
    private CasAuthenticationToken authenticateNow(final Authentication authentication) {
        try {
            Assertion assertion = this.ticketValidator.validate(
                    authentication.getCredentials().toString(),
                    getServiceUrl(authentication)); // from redis

            if (log.isDebugEnabled())
                log.debug("authentication : Credentials : " +
                        authentication.getCredentials().toString() +
                        "with serviceUrl : " +
                        getServiceUrl(authentication));

            UserDetails userDetails = loadUserByAssertion(assertion);
            this.userDetailsChecker.check(userDetails);

            return new CasAuthenticationToken(
                    this.key,
                    userDetails,
                    authentication.getCredentials(),
                    this.authoritiesMapper.mapAuthorities(userDetails.getAuthorities()),
                    userDetails,
                    assertion);
        } catch (TicketValidationException ex) {
            throw new BadCredentialsException(ex.getMessage(), ex);
        }
    }

    /**
     * Récupère l'URL du service à partir de la requête ou de la configuration.
     */
    private String getServiceUrl(Authentication authentication) {
        log.info("GET SERVICE URL");

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) {
            throw new IllegalStateException("No request context available");
        }

        HttpServletRequest request = attrs.getRequest();



        String baseUrl = request.getScheme() + "://" +
                request.getServerName() +
                (request.getServerPort() == 80 || request.getServerPort() == 443
                        ? ""
                        : ":" + request.getServerPort());



        return baseUrl + appConfProperties.getCasServiceId();
    }

    /**
     * Charge l'utilisateur via le service configuré à partir des attributs CAS.
     */
    protected UserDetails loadUserByAssertion(final Assertion assertion) {
        final CasAssertionAuthenticationToken token = new CasAssertionAuthenticationToken(assertion, "");
        return this.authenticationUserDetailsService.loadUserDetails(token);
    }

    // ========================= Setters ==============================

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setUserDetailsService(final UserDetailsService userDetailsService) {
        this.authenticationUserDetailsService = new UserDetailsByNameServiceWrapper(userDetailsService);
    }

    public void setAuthenticationUserDetailsService(
            final AuthenticationUserDetailsService<CasAssertionAuthenticationToken> service) {
        this.authenticationUserDetailsService = service;
    }

    public void setServiceProperties(final ServiceProperties serviceProperties) {
        this.serviceProperties = serviceProperties;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setStatelessTicketCache(final StatelessTicketCache statelessTicketCache) {
        this.statelessTicketCache = statelessTicketCache;
    }

    public void setTicketValidator(final TicketValidator ticketValidator) {
        this.ticketValidator = ticketValidator;
    }

    public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
        this.authoritiesMapper = authoritiesMapper;
    }

    @Override
    public void setMessageSource(final MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return CasServiceTicketAuthenticationToken.class.isAssignableFrom(authentication)
                || CasAuthenticationToken.class.isAssignableFrom(authentication)
                || CasAssertionAuthenticationToken.class.isAssignableFrom(authentication)
                || UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
