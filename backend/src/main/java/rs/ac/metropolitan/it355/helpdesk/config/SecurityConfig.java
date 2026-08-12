package rs.ac.metropolitan.it355.helpdesk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import rs.ac.metropolitan.it355.helpdesk.security.JwtAuthenticationEntryPoint;
import rs.ac.metropolitan.it355.helpdesk.security.JwtAuthenticationFilter;
import rs.ac.metropolitan.it355.helpdesk.security.RestAccessDeniedHandler;

import java.util.Arrays;
import java.util.List;

/**
 * Centralna konfiguracija bezbednosti.
 *
 * Pristup je dvoslojan:
 * <ol>
 *   <li><b>Nivo rute</b> - u {@link SecurityFilterChain} se grubo odredjuje koja uloga
 *       uopste sme da dodirne odredjenu putanju (npr. /api/admin/** samo ADMIN).</li>
 *   <li><b>Nivo metode i zapisa</b> - {@code @PreAuthorize} i provere u servisima
 *       resavaju pitanja koja URL ne moze da odgovori, npr. "ovo jeste tiket, ali
 *       da li je bas tvoj".</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ukljucuje @PreAuthorize / @PostAuthorize anotacije
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF zastita je namenjena sesijama u pretrazivacu. Ovaj API je bez stanja
                // i autentifikuje se Authorization zaglavljem, koje pretrazivac ne salje
                // automatski, pa CSRF napad nije primenljiv.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Bez HTTP sesije - svaki zahtev nosi sopstveni token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)   // 401
                        .accessDeniedHandler(accessDeniedHandler))            // 403

                .authorizeHttpRequests(auth -> auth
                        // --- javne rute ---
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/priorities").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- administrativne rute ---
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories", "/api/priorities").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**", "/api/priorities/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/priorities/**").hasRole("ADMIN")

                        // --- rute rezervisane za osoblje podrske ---
                        .requestMatchers("/api/tickets/*/assign", "/api/tickets/*/status")
                            .hasAnyRole("AGENT", "ADMIN")
                        .requestMatchers("/api/reports/**").hasAnyRole("AGENT", "ADMIN")

                        // --- sve ostalo trazi prijavu ---
                        .anyRequest().authenticated())

                // H2 konzola se prikazuje u okviru <frame> elementa, koji podrazumevana
                // X-Frame-Options: DENY politika blokira. Izuzetak vazi samo za nju.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // Nas JWT filter mora da se izvrsi pre standardnog filtera za formu,
                // da bi SecurityContext vec bio popunjen kada se proverava autorizacija.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt automatski generise nasumican salt za svaku lozinku i ugradjuje ga u hes,
     * pa dve iste lozinke u bazi imaju razlicit zapis.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** Dozvoljava React razvojnom serveru da poziva API sa druge adrese. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
