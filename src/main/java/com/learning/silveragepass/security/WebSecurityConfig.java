package com.learning.silveragepass.security;

import com.learning.silveragepass.security.jwt.AuthEntryPointJwt;
import com.learning.silveragepass.security.jwt.AuthTokenFilter;
import com.learning.silveragepass.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 使用 allowedOriginPatterns 代替 allowedOrigins 以支持通配符
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Enable async security context propagation for SSE streaming endpoints
            .securityContext(securityContext -> securityContext
                    .requireExplicitSave(false))
            .authorizeHttpRequests(auth -> 
                auth.requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/api/admin/auth/login")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/api/admin/auth/register")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/api/test/all")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/api/public/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/api/files/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/uploads/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/error")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/index.html")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/css/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/js/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/images/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/favicon.ico")).permitAll()
                    // WebSocket endpoints - authentication is handled by JwtChannelInterceptor at STOMP level
                    // (websocket-notification-center Requirements 1.1, 1.2)
                    .requestMatchers(new AntPathRequestMatcher("/ws/**")).permitAll()
                    .requestMatchers(new AntPathRequestMatcher("/ws")).permitAll()
                    // Health monitoring endpoints require authentication (Requirements 4.1)
                    .requestMatchers(new AntPathRequestMatcher("/api/health/**")).authenticated()
                    // Demand heatmap endpoints require authentication (demand-heatmap Requirements 1.1)
                    // /api/demands/heat-map and /api/demands/heat-map/aggregated are accessible to authenticated users
                    .requestMatchers(new AntPathRequestMatcher("/api/demands/heat-map/**")).authenticated()
                    .requestMatchers(new AntPathRequestMatcher("/api/demands/heat-map")).authenticated()
                    // API Key endpoints require authentication (api-key-security Requirements 1.3, 1.4)
                    // Unauthenticated requests will be rejected with 401 error
                    .requestMatchers(new AntPathRequestMatcher("/api/keys/**")).authenticated()
                    // Weather proxy endpoints require authentication (api-key-security Requirements 1.3, 1.4)
                    // All weather requests are proxied through backend to protect API keys
                    .requestMatchers(new AntPathRequestMatcher("/api/weather/**")).authenticated()
                    // Video streaming endpoints require authentication (video-streaming Requirements 1.4, 2.5)
                    // Unauthenticated users will receive 401 Unauthorized error
                    .requestMatchers(new AntPathRequestMatcher("/api/videos/**")).authenticated()
                    // Admin video management endpoints require ADMIN role (video-streaming Requirements 4.5)
                    // Non-admin users will receive 403 Forbidden error
                    .requestMatchers(new AntPathRequestMatcher("/api/admin/videos/**")).hasRole("ADMIN")
                    // Organization certification user endpoints require authentication (organization-certification Requirements 1.1, 2.1)
                    // Users must be logged in to submit or query certification status
                    .requestMatchers(new AntPathRequestMatcher("/api/certification/**")).authenticated()
                    // Admin certification management endpoints require ADMIN role (organization-certification Requirements 3.4)
                    // Only admins can review, approve, or reject certification applications
                    .requestMatchers(new AntPathRequestMatcher("/api/admin/certifications/**")).hasRole("ADMIN")
                    .anyRequest().authenticated()
            );

        http.authenticationProvider(authenticationProvider());

        // Add JWT filter before UsernamePasswordAuthenticationFilter
        // The filter is configured to skip async dispatch requests to avoid re-authentication
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
} 