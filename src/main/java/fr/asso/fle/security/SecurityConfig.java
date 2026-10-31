package fr.asso.fle.security;

import fr.asso.fle.model.Utilisateur;
import fr.asso.fle.repository.UtilisateurRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Pages publiques : accueil, planning des unités (apprenants), ressources statiques
                .requestMatchers("/", "/u/**", "/plan-du-site", "/confidentialite", "/login", "/error", "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/responsable/**").hasRole("RESPONSABLE")
                .requestMatchers("/saisie/**").hasAnyRole("BENEVOLE", "RESPONSABLE")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(redirectionParRole())
                .failureUrl("/login?erreur")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?deconnecte")
                .permitAll());
        return http.build();
    }

    /** Après connexion, chacun arrive directement sur son espace. */
    private AuthenticationSuccessHandler redirectionParRole() {
        return (request, response, authentication) -> {
            String cible = "/";
            for (GrantedAuthority a : authentication.getAuthorities()) {
                switch (a.getAuthority()) {
                    case "ROLE_ADMIN" -> cible = "/admin";
                    case "ROLE_RESPONSABLE" -> cible = "/responsable";
                    case "ROLE_BENEVOLE" -> cible = "/saisie";
                    default -> { }
                }
            }
            response.sendRedirect(request.getContextPath() + cible);
        };
    }

    @Bean
    public UserDetailsService userDetailsService(UtilisateurRepository repository) {
        return username -> {
            Utilisateur u = repository.findByUsername(username == null ? "" : username.strip().toLowerCase())
                    .orElseThrow(() -> new UsernameNotFoundException("Identifiant inconnu"));
            return User.withUsername(u.getUsername())
                    .password(u.getPassword())
                    .roles(u.getRole().name())
                    .disabled(!u.isActif())
                    .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
