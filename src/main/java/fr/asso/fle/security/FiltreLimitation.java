package fr.asso.fle.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Limitation du nombre de requêtes, avant même Spring Security :
 * - toutes les pages : N requêtes par minute et par adresse IP ;
 * - abonnements aux alertes : N par heure et par adresse IP ;
 * - connexion : refusée tant que l'adresse IP ou l'identifiant est bloqué (voir ProtectionConnexion).
 * Derrière Caddy, l'adresse IP réelle est fournie par l'en-tête X-Forwarded-For
 * (server.forward-headers-strategy=native).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltreLimitation extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltreLimitation.class);

    private final LimiteurRequetes limiteur;
    private final ProtectionConnexion protectionConnexion;
    private final boolean active;
    private final int parMinute;
    private final int abonnementsParHeure;

    public FiltreLimitation(LimiteurRequetes limiteur,
                            ProtectionConnexion protectionConnexion,
                            @Value("${app.limites.actives:true}") boolean active,
                            @Value("${app.limites.requetes-par-minute:120}") int parMinute,
                            @Value("${app.limites.abonnements-par-heure:10}") int abonnementsParHeure) {
        this.limiteur = limiteur;
        this.protectionConnexion = protectionConnexion;
        this.active = active;
        this.parMinute = parMinute;
        this.abonnementsParHeure = abonnementsParHeure;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!active) {
            return true;
        }
        // Fichiers statiques : mis en cache par le navigateur, inutile de les compter.
        String uri = request.getRequestURI();
        return uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/icons/")
                || uri.equals("/favicon.ico") || uri.equals("/sw.js") || uri.equals("/hors-ligne.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        boolean post = "POST".equalsIgnoreCase(request.getMethod());

        // 1. Limite générale
        if (!limiteur.autoriser("global:" + ip, parMinute, Duration.ofMinutes(1))) {
            refuser(request, response, "Trop de requêtes. Patientez une minute, puis réessayez.");
            return;
        }

        // 2. Abonnements aux alertes (adresse publique qui écrit en base)
        if (post && uri.equals("/push/abonnement")
                && !limiteur.autoriser("push:" + ip, abonnementsParHeure, Duration.ofHours(1))) {
            refuser(request, response, "Trop de demandes d'abonnement. Réessayez plus tard.");
            return;
        }

        // 3. Connexion bloquée après trop d'échecs
        if (post && uri.equals("/login") && protectionConnexion.estBloque(ip, request.getParameter("username"))) {
            log.warn("Connexion refusée (trop d'échecs) depuis {}", ip);
            response.sendRedirect(request.getContextPath() + "/login?bloque");
            return;
        }

        chain.doFilter(request, response);
    }

    private static void refuser(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        log.warn("Limite de requêtes atteinte : {} {} depuis {}", request.getMethod(),
                request.getRequestURI(), request.getRemoteAddr());
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String accept = request.getHeader("Accept");
        if (request.getRequestURI().startsWith("/push/") || (accept != null && accept.contains("application/json"))) {
            response.setContentType("application/json");
            response.getWriter().write("{\"ok\":false,\"message\":\"" + message + "\"}");
        } else {
            response.setContentType("text/html");
            response.getWriter().write("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                    + "<title>Patientez un instant</title></head>"
                    + "<body style=\"font-family:system-ui,sans-serif;text-align:center;padding:3rem 1rem;color:#1f2a44\">"
                    + "<h1 style=\"font-size:1.5rem\">⏳ Patientez un instant</h1><p>" + message + "</p>"
                    + "<p><a href=\"/\">Retour à l'accueil</a></p></body></html>");
        }
    }
}
