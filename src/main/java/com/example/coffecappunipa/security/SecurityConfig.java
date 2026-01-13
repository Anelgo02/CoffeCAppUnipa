package com.example.coffecappunipa.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationSuccessHandler roleRedirectSuccessHandler() {
        return (HttpServletRequest req, HttpServletResponse resp, Authentication auth) -> {
            boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
            boolean isMaint   = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MAINTAINER"));
            boolean isCust    = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

            if (isManager) { resp.sendRedirect("/gestore/index.html"); return; }
            if (isMaint)   { resp.sendRedirect("/manutenzione/index.html"); return; }
            if (isCust)    { resp.sendRedirect("/cliente/index.html"); return; }

            resp.sendRedirect("/login.html?err=invalid_role");
        };
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AuthenticationSuccessHandler successHandler) throws Exception {

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // per evitare di dover iniettare CSRF nel form statico di login
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/auth/login", "/auth/logout", "/api/distributor/boot","/api/distributor/purchase")
                )

                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterAfter(new LegacySessionBridgeFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // static/public
                        .requestMatchers("/login.html", "/cliente/registrazione.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/data/**", "/favicon.ico").permitAll()
                        .requestMatchers("/distributore/**").permitAll()

                        // public endpoints
                        .requestMatchers("/api/customer/register").permitAll()
                        .requestMatchers("/api/distributor/boot","/api/distributor/poll", "/api/distributor/beverages", "/api/distributor/purchase").permitAll()

                        // pages role-based
                        .requestMatchers("/gestore/**").hasRole("MANAGER")
                        .requestMatchers("/manutenzione/**").hasRole("MAINTAINER")
                        .requestMatchers("/cliente/**").hasRole("CUSTOMER")

                        .requestMatchers(
                                "/api/manager/maintainers.xml",
                                "/api/manager/maintainers/list",
                                "/api/manager/maintainers/create",
                                "/api/manager/maintainers/delete",
                                "/api/manager/distributors/list",
                                "/api/manager/distributors/create",
                                "/api/manager/distributors/delete",
                                "/api/manager/distributors/status",
                                "/api/monitor/sync"

                ).hasRole("MANAGER")

                        // maintainer APIs
                        .requestMatchers(
                                "/api/maintainer/distributors/refill",
                                "/api/maintainer/distributors/status"
                        ).hasRole("MAINTAINER")


                        // customer APIs
                        .requestMatchers(
                                "/api/customer/connect",
                                "/api/customer/disconnect",
                                "/api/customer/current-connection",
                                "/api/customer/me",
                                "/api/customer/get",
                                "/api/customer/topup"
                        ).hasRole("CUSTOMER")

                        .requestMatchers("/api/monitor/map").hasAnyRole("MANAGER","MAINTAINER")


                        .anyRequest().authenticated()

                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/auth/login")
                        .successHandler(successHandler)
                        .failureUrl("/login.html?err=bad_credentials")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/login.html?logout")
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, resp, e) -> {
                            if (req.getRequestURI().startsWith("/api/")) {
                                resp.setStatus(HttpStatus.UNAUTHORIZED.value());
                                resp.setContentType("application/json");
                                resp.getWriter().write("{\"ok\":false,\"message\":\"unauthorized\"}");
                            } else {
                                resp.sendRedirect("/login.html");
                            }
                        })
                        .accessDeniedHandler((req, resp, e) -> {
                            if (req.getRequestURI().startsWith("/api/")) {
                                resp.setStatus(HttpStatus.FORBIDDEN.value());
                                resp.setContentType("application/json");
                                resp.getWriter().write("{\"ok\":false,\"message\":\"forbidden\"}");
                            } else {
                                resp.sendError(HttpStatus.FORBIDDEN.value());
                            }
                        })
                )
                // utile per curl/debug
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
