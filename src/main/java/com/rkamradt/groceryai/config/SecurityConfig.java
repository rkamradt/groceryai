package com.rkamradt.groceryai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/me").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler)
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(ui -> ui.oidcUserService(oidcUserService()))
                .defaultSuccessUrl("/", true)
                .failureHandler((req, res, ex) -> {
                    log.error("OAuth2 login failed: {}", ex.getMessage(), ex);
                    res.sendRedirect("/?error=auth_failed");
                })
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .invalidateHttpSession(true)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.sendError(401, "Unauthorized");
                    } else {
                        res.sendRedirect("/oauth2/authorization/google");
                    }
                })
                .accessDeniedHandler((req, res, e) ->
                    res.sendError(403, "Forbidden — must sign in with a @kamradtfamily.net account"))
            );

        return http.build();
    }

    /**
     * Enforce @kamradtfamily.net domain: belt-and-suspenders on top of the
     * "Internal" consent screen. Rejects if hd != kamradtfamily.net OR
     * email_verified != true.
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser user = delegate.loadUser(request);
            Boolean emailVerified = user.getAttribute("email_verified");
            String email = user.getEmail();
            if (!Boolean.TRUE.equals(emailVerified)
                    || email == null
                    || !email.endsWith("@kamradtfamily.net")) {
                throw new OAuth2AuthenticationException("access_denied");
            }
            return user;
        };
    }
}
