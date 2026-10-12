package com.sms.config;

import com.sms.repository.UserRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two kinds of login (HTTP Basic auth):
 *  - the single ADMIN, whose username/password live in application.properties
 *  - STUDENTs, whose logins are stored in the users table
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // let Spring Boot's error page through so real error codes (404, 400...) reach the client
                    .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    // file links (videos, materials, submission PDFs) carry their own signature instead of a login
                    .requestMatchers(HttpMethod.GET, "/api/videos/*/stream", "/api/materials/*/file",
                            "/api/submissions/*/file").permitAll()
                    .requestMatchers("/api/auth/**").authenticated()
                    .requestMatchers("/api/student/**").hasRole("STUDENT")
                    .anyRequest().hasRole("ADMIN"))
            // Plain 401 (no WWW-Authenticate header) so the browser doesn't show its own login popup
            .httpBasic(basic -> basic.authenticationEntryPoint(
                    (req, res, ex) -> res.setStatus(HttpServletResponse.SC_UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository,
                                                 PasswordEncoder passwordEncoder,
                                                 @Value("${app.admin.username}") String adminUsername,
                                                 @Value("${app.admin.password}") String adminPassword) {
        String adminPasswordHash = passwordEncoder.encode(adminPassword);

        // a fresh UserDetails is built each time, because Spring Security clears the password after login
        return username -> {
            if (username.equals(adminUsername)) {
                return User.withUsername(adminUsername)
                        .password(adminPasswordHash)
                        .roles("ADMIN")
                        .build();
            }
            com.sms.model.User student = userRepository.findByUsername(username.trim().toLowerCase())
                    .orElseThrow(() -> new UsernameNotFoundException(username));
            return User.withUsername(student.getUsername())
                    .password(student.getPassword())
                    .roles("STUDENT")
                    .build();
        };
    }
}
