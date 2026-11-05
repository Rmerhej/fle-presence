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
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ProtectionConnexion protectionConnexion;

    public SecurityConfig(ProtectionConnexion protectionConnexion) {
        this.protectionConnexion = protectionConnexion;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Pages publiques : accueil, planning des unités (apprenants), ressources statiques
                .requestMatchers("/", "/u/**", "/plan-du-site", "/confidentialite", "/login", "/error",
                        "/css/**", "/js/**", "/img/**", "/icons/**", "/favicon.ico",
                        "/manifest.json", "/sw.js", "/hors-ligne.html", "/push/**",
                        "/actuator/health").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/responsable/**").hasRole("RESPONSABLE")
                .requestMatchers("/saisie/**").hasAnyRole("BENEVOLE", "RESPONSABLE")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(redirectionParRole())
                .failureHandler(echecConnexion())
                .permitAll())
            // Abonnement anonyme aux notifications (appel JavaScript, sans session ni formulaire)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/push/**"))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?deconnecte")
                .permitAll());
        return http.build();
    }

    /** Après connexion, chacun arrive directement sur son espace. */
    private AuthenticationSuccessHandler redirectionParRole() {
        return (request, response, authentication) -> {
            protectionConnexion.succes(request.getRemoteAddr(), authentication.getName());
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

    /** Chaque échec est compté : trop d'échecs bloquent temporairement l'IP et l'identifiant. */
    private AuthenticationFailureHandler echecConnexion() {
        return (request, response, exception) -> {
            String ip = request.getRemoteAddr();
            String identifiant = request.getParameter("username");
            protectionConnexion.echec(ip, identifiant);
            boolean bloque = protectionConnexion.estBloque(ip, identifiant);
            response.sendRedirect(request.getContextPath() + (bloque ? "/login?bloque" : "/login?erreur"));
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
