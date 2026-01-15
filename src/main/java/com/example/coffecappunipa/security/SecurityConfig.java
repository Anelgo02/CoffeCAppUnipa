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
                        .ignoringRequestMatchers("/auth/login", "/auth/logout")
                        // IMPORTANTE: Ignora CSRF per il distributore
                        .ignoringRequestMatchers("/api/distributor/**")
                )

                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterAfter(new LegacySessionBridgeFilter(), UsernamePasswordAuthenticationFilter.class)

                // Filtro custom per token distributore
                .addFilterBefore(new DistributorTokenFilter(), UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // 1. RISORSE STATICHE PUBBLICHE (HTML, CSS, JS)
                        .requestMatchers("/login.html", "/cliente/registrazione.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/data/**", "/favicon.ico").permitAll()
                        .requestMatchers("/error", "/.well-known/**").permitAll()



                        // Questo permette di vedere boot.html e index.html senza login utente.
                        .requestMatchers("/distributore/**", "/distributore","/distributore/").permitAll()

                        // 2. API PUBBLICHE
                        .requestMatchers("/api/customer/register").permitAll()

                        // L'endpoint di BOOT deve essere pubblico per ottenere il primo token
                        .requestMatchers("/api/distributor/boot").permitAll()

                        // 3. API DISTRIBUTORE PROTETTE DA TOKEN (Ruolo DISTRIBUTOR assegnato dal filtro)
                        .requestMatchers(
                                "/api/distributor/poll",
                                "/api/distributor/beverages",
                                "/api/distributor/purchase",
                                "/api/distributor/reset"
                        ).hasRole("DISTRIBUTOR")

                        // 4. API GESTORE
                        .requestMatchers("/gestore/**").hasRole("MANAGER")
                        .requestMatchers(
                                "/api/manager/**",
                                "/api/monitor/sync"
                        ).hasRole("MANAGER")

                        // 5. API MANUTENTORE
                        .requestMatchers("/manutenzione/**").hasRole("MAINTAINER")
                        .requestMatchers(
                                "/api/maintainer/**"
                        ).hasRole("MAINTAINER")

                        // 6. API CLIENTE
                        .requestMatchers("/cliente/**").hasRole("CUSTOMER")
                        .requestMatchers(
                                "/api/customer/**"
                        ).hasRole("CUSTOMER")

                        // API CONDIVISA (Mappa monitoraggio)
                        .requestMatchers("/api/monitor/map").hasAnyRole("MANAGER","MAINTAINER")

                        // Tutto il resto richiede autenticazione
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
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}