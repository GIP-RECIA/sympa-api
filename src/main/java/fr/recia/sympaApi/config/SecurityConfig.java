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

import fr.recia.sympaApi.config.bean.AppConfProperties;
import fr.recia.sympaApi.config.custom.impl.CasSuccessHandler;
import fr.recia.sympaApi.config.custom.impl.CustomAuthenticationProvider;
import fr.recia.sympaApi.config.custom.impl.CustomCas20ProxyTicketValidator;
import fr.recia.sympaApi.config.custom.impl.CustomCasAuthenticationEntryPoint;
import fr.recia.sympaApi.config.custom.impl.CustomSessionMappingStorage;
import fr.recia.sympaApi.config.custom.impl.ProxyGrantingTickeStoragetRedisImpl;
import fr.recia.sympaApi.config.custom.impl.UserCustomImplementation;
import lombok.extern.slf4j.Slf4j;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class SecurityConfig {

  @Autowired
  AppConfProperties appConfProperties;

  @Autowired
  CorsConfigurationSource corsConfigurationSource;

  @Autowired
  private CustomSessionMappingStorage ticketSessionMappingStorage;

  @Autowired
  private CasSuccessHandler casSuccessHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    CookieCsrfTokenRepository cookieCsrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    cookieCsrfTokenRepository.setCookiePath("/");
    cookieCsrfTokenRepository.setCookieName("SYMPA-XSRF-TOKEN");

    http
      .cors(cors -> cors.configurationSource(corsConfigurationSource))
      .csrf(csrf -> csrf.csrfTokenRepository(cookieCsrfTokenRepository))
      .addFilterBefore(singleSignOutFilter(), CasAuthenticationFilter.class)
      .httpBasic(AbstractHttpConfigurer::disable)
      .formLogin(AbstractHttpConfigurer::disable)
      .authenticationProvider(customAuthProvider(serviceProperties()))
      .addFilterBefore(casAuthenticationFilter(authenticationManager(customAuthProvider(serviceProperties()))), UsernamePasswordAuthenticationFilter.class)
      .exceptionHandling(e -> e.authenticationEntryPoint(casAuthenticationEntryPoint()))
      .authorizeHttpRequests(authorize -> authorize
        .antMatchers("/health-check").permitAll()
        .antMatchers("/api/admin-sympa/**").authenticated()
        .antMatchers("/api/sympa/**").authenticated()
        .antMatchers(appConfProperties.getCasTicketCallback()).permitAll()
        .antMatchers(appConfProperties.getCasProxyReceptorUrl()).permitAll()
        .anyRequest().denyAll()
      );
    return http.build();
  }

  public CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
    CasAuthenticationEntryPoint casAuthenticationEntryPoint = new CustomCasAuthenticationEntryPoint(appConfProperties);
    casAuthenticationEntryPoint.setLoginUrl(this.appConfProperties.getCasServerLoginUrl()); //old concatenation
    casAuthenticationEntryPoint.setServiceProperties(serviceProperties());
    return casAuthenticationEntryPoint;
  }

  @Bean
  public ServiceProperties serviceProperties() {
    ServiceProperties serviceProperties = new ServiceProperties();
    serviceProperties.setService(appConfProperties.getCasServiceId());
    serviceProperties.setSendRenew(false);
    return serviceProperties;
  }

  @Bean
  public ProxyGrantingTicketStorage pgtStorage(){
    return new ProxyGrantingTickeStoragetRedisImpl();
  }

  @Bean
  public AuthenticationUserDetailsService<CasAssertionAuthenticationToken> customUserDetailsService() {
    return (CasAssertionAuthenticationToken token) -> {
      Assertion assertion = token.getAssertion();
      Map<String, Object> attributes = assertion.getPrincipal().getAttributes();
      String username = assertion.getPrincipal().getName();
      return new UserCustomImplementation(username, "", List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes);
    };
  }

  @Bean
  public CustomAuthenticationProvider customAuthProvider(ServiceProperties serviceProperties) {
    CustomAuthenticationProvider provider = new CustomAuthenticationProvider(appConfProperties);
    provider.setServiceProperties(serviceProperties);

    Cas20ProxyTicketValidator validator = new CustomCas20ProxyTicketValidator(appConfProperties.getCasServerUrl());
    validator.setProxyCallbackUrl(appConfProperties.getCasProxyTicketCallback());
    validator.setProxyGrantingTicketStorage(pgtStorage());

    provider.setTicketValidator(validator);
    provider.setAuthenticationUserDetailsService(customUserDetailsService());
    provider.setKey(appConfProperties.getCasProviderKey());
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(CustomAuthenticationProvider customAuthProvider) {
    return new ProviderManager(customAuthProvider);
  }

  @Bean
  public CasAuthenticationFilter casAuthenticationFilter(AuthenticationManager authenticationManager) {
    CasAuthenticationFilter filter = new CasAuthenticationFilter();
    filter.setAuthenticationManager(authenticationManager);
    filter.setFilterProcessesUrl(appConfProperties.getCasTicketCallback());
    filter.setProxyGrantingTicketStorage(pgtStorage());
    filter.setProxyReceptorUrl(appConfProperties.getCasProxyReceptorUrl());
    filter.setAuthenticationSuccessHandler(casSuccessHandler);
    return filter;
  }

  /**
   * Filtre CAS pour le Single Logout (SLO).
   */
  @Bean
  public Filter singleSignOutFilter() {
    SingleSignOutFilter delegate = new SingleSignOutFilter();
    delegate.setIgnoreInitConfiguration(true);
    delegate.setArtifactParameterName("ticket");
    delegate.setLogoutParameterName("logoutRequest");

    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {
        String logoutRequest = request.getParameter("logoutRequest");
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        log.debug("[SLO] Requête entrante : {} {} depuis IP={}", method, uri, ip);

        if (logoutRequest != null) {
          log.trace("[SLO] URI appelée : {}", uri);
          log.trace("[SLO] Adresse IP appelante : {}", ip);
          log.trace("[SLO] XML logoutRequest brut :\n{}", logoutRequest);

          // Parsing XML SAML pour extraire le ticket (SessionIndex)
          try {
            var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();
            var doc = builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(logoutRequest)));
            doc.getDocumentElement().normalize();

            var nameIdNode = doc.getElementsByTagName("saml:NameID").item(0);
            var sessionIndexNode = doc.getElementsByTagName("samlp:SessionIndex").item(0);

            String nameId = nameIdNode != null ? nameIdNode.getTextContent() : "inconnu";
            String ticket = sessionIndexNode != null ? sessionIndexNode.getTextContent() : "inconnu";

            // Lors du logout, le CAS envoie aussi des messages pour invalider les PGT, mais ici on ne traite que les
            // SessionTicket, qui commencent par ST

            int index = ticket.indexOf('-');
            boolean isSessionTicket = false;

            if (index != -1) {
              String beforeDash = ticket.substring(0, index + 1);
              if("ST-".equals(beforeDash)){
                isSessionTicket = true;
              }
            }

            if(isSessionTicket){
              log.debug("[SLO] Ticket Invalidation Request will be handled: {}", ticket);
            }else {
              log.debug("[SLO] Ticket Invalidation Request will be ignored: {}", ticket);
              filterChain.doFilter(request, response);
              return;
            }

            String sessionId = ticketSessionMappingStorage.getSessionIdFromSessionTicket(ticket);

            log.debug("[SLO] Utilisateur CAS (NameID) : {}", nameId);
            log.debug("[SLO] Session id: {}", sessionId);

            ticketSessionMappingStorage.removeSessionTicket(ticket);
            log.debug("[SLO] Le cache associé au mappage ticket-sessionID [{}:{}] a été supprimé avec succès.", ticket, sessionId);
            ticketSessionMappingStorage.deleteSessionContext(sessionId);
            log.debug("[SLO] Invalidation réussie de la session [{}].", sessionId);

          } catch (Exception e) {
            log.error("[SLO] Erreur de parsing XML logoutRequest", e);
          }
        }
        filterChain.doFilter(request, response);
      }
    };
  }

}
