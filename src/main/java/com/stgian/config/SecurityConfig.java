package com.stgian.config;

import com.stgian.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.*;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Value("${stgian.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${stgian.frontend-url}")
    private String frontendUrl;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter      = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .headers(headers -> headers
                .contentTypeOptions(ct -> {})
                .frameOptions(fo -> fo.deny())
                .xssProtection(xss -> {})
                .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    // FIX-3: connect-src inclui o frontend e APIs externas usadas pelo site
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://sdk.mercadopago.com; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://fonts.gstatic.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "img-src 'self' data: blob:; " +
                    // connect-src agora inclui localhost e as APIs externas que o frontend chama
                    "connect-src 'self' http://localhost:8080 http://127.0.0.1:8080 " +
                    "https://viacep.com.br https://api.mercadopago.com https://wa.me; " +
                    "frame-ancestors 'none';"
                ))
            )

            .authorizeHttpRequests(auth -> auth
                // Público
                .requestMatchers("/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/drops/active").permitAll()
                .requestMatchers("/api/payments/webhook").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/payments/success").permitAll()

                // Cliente, ADM e Dono
                .requestMatchers("/api/orders/checkout").hasAnyRole("CLIENT","ADMIN","OWNER")
                .requestMatchers("/api/orders/my").hasAnyRole("CLIENT","ADMIN","OWNER")
                .requestMatchers("/api/orders/track/**").hasAnyRole("CLIENT","ADMIN","OWNER")
                .requestMatchers("/api/users/me/**").hasAnyRole("CLIENT","ADMIN","OWNER")
                .requestMatchers("/api/tracking/order/**").hasAnyRole("CLIENT","ADMIN","OWNER")

                // ADM e Dono
                .requestMatchers("/api/products/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/orders/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/drops/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/stock/**").hasAnyRole("ADMIN","OWNER")

                // Somente Dono
                .requestMatchers("/api/admins/**").hasRole("OWNER")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        configuration.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "X-Requested-With", "Accept"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
