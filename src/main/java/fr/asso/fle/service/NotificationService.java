package fr.asso.fle.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.asso.fle.model.*;
import fr.asso.fle.repository.GroupeRepository;
import fr.asso.fle.repository.ParametreRepository;
import fr.asso.fle.repository.PushAbonnementRepository;
import fr.asso.fle.repository.SeanceRepository;
import fr.asso.fle.repository.UniteLocaleRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Notifications push vers les téléphones des apprenants qui les ont activées
 * (annulation ou report d'un cours). Les clés VAPID sont générées automatiquement
 * au premier usage et conservées en base : aucune configuration nécessaire.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String CLE_PUBLIQUE = "vapid.public";
    private static final String CLE_PRIVEE = "vapid.private";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE);

    private final ParametreRepository parametres;
    private final PushAbonnementRepository abonnements;
    private final UniteLocaleRepository unites;
    private final GroupeRepository groupes;
    private final SeanceRepository seances;
    private final ObjectMapper json;
    private final boolean actives;
    private final String sujet;

    private volatile PushService pushService;
    private volatile String clePublique;

    public NotificationService(ParametreRepository parametres, PushAbonnementRepository abonnements,
                               UniteLocaleRepository unites, GroupeRepository groupes, SeanceRepository seances,
                               ObjectMapper json,
                               @Value("${app.notifications.actives:true}") boolean actives,
                               @Value("${app.notifications.sujet:https://www.secours-catholique.org}") String sujet) {
        this.parametres = parametres;
        this.abonnements = abonnements;
        this.unites = unites;
        this.groupes = groupes;
        this.seances = seances;
        this.json = json;
        this.actives = actives;
        this.sujet = sujet;
    }

    public boolean isActives() {
        return actives;
    }

    // ======================================================================= clés VAPID

    /** Clé publique à donner au navigateur (format base64url, point non compressé). */
    @Transactional
    public synchronized String clePublique() {
        initialiser();
        return clePublique;
    }

    private synchronized void initialiser() {
        if (pushService != null) {
            return;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            Optional<Parametre> pub = parametres.findById(CLE_PUBLIQUE);
            Optional<Parametre> priv = parametres.findById(CLE_PRIVEE);
            String publique;
            String privee;
            if (pub.isPresent() && priv.isPresent()) {
                publique = pub.get().getValeur();
                privee = priv.get().getValeur();
            } else {
                KeyPairGenerator generateur = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
                generateur.initialize(new ECGenParameterSpec("prime256v1"));
                KeyPair paire = generateur.generateKeyPair();
                Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
                publique = b64.encodeToString(((ECPublicKey) paire.getPublic()).getQ().getEncoded(false));
                privee = b64.encodeToString(sansZeroInitial(((ECPrivateKey) paire.getPrivate()).getD().toByteArray()));
                parametres.save(new Parametre(CLE_PUBLIQUE, publique));
                parametres.save(new Parametre(CLE_PRIVEE, privee));
                log.info("Clés des notifications push générées.");
            }
            this.pushService = new PushService(publique, privee, sujet);
            this.clePublique = publique;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser les notifications push", e);
        }
    }

    private static byte[] sansZeroInitial(byte[] octets) {
        if (octets.length > 32 && octets[0] == 0) {
            return Arrays.copyOfRange(octets, octets.length - 32, octets.length);
        }
        return octets;
    }

    // ======================================================================= abonnements

    @Transactional
    public void abonner(String slugUnite, Long groupeId, String endpoint, String p256dh, String auth) {
        if (!actives) {
            throw new RegleMetierException("Les notifications sont désactivées.");
        }
        if (endpoint == null || !endpoint.startsWith("https://") || endpoint.length() > 1000
                || p256dh == null || p256dh.isBlank() || p256dh.length() > 255
                || auth == null || auth.isBlank() || auth.length() > 255) {
            throw new RegleMetierException("Abonnement invalide.");
        }
        UniteLocale unite = unites.findBySlugAndActiveTrue(slugUnite)
                .orElseThrow(() -> new RegleMetierException("Unité inconnue."));
        Groupe groupe = null;
        if (groupeId != null) {
            groupe = groupes.findById(groupeId)
                    .filter(g -> g.getUnite().getId().equals(unite.getId()))
                    .orElse(null);
        }
        String hash = sha256(endpoint);
        PushAbonnement a = abonnements.findByEndpointHash(hash).orElseGet(PushAbonnement::new);
        a.setUnite(unite);
        a.setGroupe(groupe);
        a.setEndpoint(endpoint);
        a.setEndpointHash(hash);
        a.setP256dh(p256dh);
        a.setAuth(auth);
        a.setCreeLe(LocalDateTime.now());
        abonnements.save(a);
    }

    @Transactional
    public void desabonner(String endpoint) {
        if (endpoint != null) {
            abonnements.findByEndpointHash(sha256(endpoint)).ifPresent(abonnements::delete);
        }
    }

    @Transactional(readOnly = true)
    public long nombreAbonnes(UniteLocale unite) {
        return abonnements.countByUnite(unite);
    }

    // ======================================================================= envoi

    /** Après validation d'une annulation ou d'un report, prévient les abonnés (en arrière-plan). */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void surSeancesModifiees(SeanceService.SeancesModifiees evenement) {
        if (!actives) {
            return;
        }
        List<Seance> modifiees = seances.findAllById(evenement.seanceIds()).stream()
                .filter(s -> !s.getDate().isBefore(LocalDate.now()))
                .filter(s -> s.getStatut() != StatutSeance.PREVUE)
                .sorted(Comparator.comparing(Seance::getDate))
                .toList();
        if (modifiees.isEmpty()) {
            return;
        }
        initialiser();

        // Regroupement par unité : un message par unité concernée.
        Map<Long, List<Seance>> parUnite = new LinkedHashMap<>();
        for (Seance s : modifiees) {
            parUnite.computeIfAbsent(s.getGroupe().getUnite().getId(), k -> new ArrayList<>()).add(s);
        }
        parUnite.forEach((uniteId, liste) -> {
            UniteLocale unite = liste.get(0).getGroupe().getUnite();
            Set<Long> groupesConcernes = new HashSet<>();
            liste.forEach(s -> groupesConcernes.add(s.getGroupe().getId()));
            String message = message(liste, unite);
            int envoyes = 0;
            for (PushAbonnement a : abonnements.findByUnite(unite)) {
                if (a.getGroupe() != null && !groupesConcernes.contains(a.getGroupe().getId())) {
                    continue;
                }
                if (envoyer(a, message)) {
                    envoyes++;
                }
            }
            log.info("Notification « {} » envoyée à {} abonné(s) de {}", liste.get(0).getStatut(), envoyes, unite.getNom());
        });
    }

    private String message(List<Seance> liste, UniteLocale unite) {
        Seance premiere = liste.get(0);
        String titre;
        String texte;
        if (liste.size() == 1 && premiere.getStatut() == StatutSeance.REPORTEE && premiere.getReporteeVers() != null) {
            titre = "Cours déplacé";
            texte = majuscule(JOUR.format(premiere.getDate())) + " → " + JOUR.format(premiere.getReporteeVers().getDate())
                    + " à " + SeanceService.heure(premiere.getReporteeVers().getHeureDebut())
                    + " (" + premiere.getGroupe().getNom() + ")";
        } else if (liste.size() == 1) {
            titre = "Cours annulé";
            texte = majuscule(JOUR.format(premiere.getDate())) + " à " + SeanceService.heure(premiere.getHeureDebut())
                    + " (" + premiere.getGroupe().getNom() + ")";
        } else {
            titre = liste.size() + " cours annulés";
            texte = "Du " + JOUR.format(premiere.getDate()) + " au " + JOUR.format(liste.get(liste.size() - 1).getDate());
        }
        if (premiere.getMotif() != null) {
            texte += " · " + premiere.getMotif();
        }
        Map<String, String> contenu = new LinkedHashMap<>();
        contenu.put("title", titre + " – " + unite.getNom());
        contenu.put("body", texte);
        contenu.put("url", "/u/" + unite.getSlug());
        try {
            return json.writeValueAsString(contenu);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Envoie une notification ; supprime l'abonnement s'il n'existe plus côté navigateur. */
    private boolean envoyer(PushAbonnement a, String message) {
        try {
            HttpResponse reponse = pushService.send(new Notification(a.getEndpoint(), a.getP256dh(), a.getAuth(), message));
            int code = reponse.getStatusLine().getStatusCode();
            if (code == 404 || code == 410) {
                abonnements.delete(a);
                return false;
            }
            if (code >= 400) {
                log.warn("Notification refusée ({}) par {}", code, hote(a.getEndpoint()));
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Échec d'envoi d'une notification vers {} : {}", hote(a.getEndpoint()), e.getMessage());
            return false;
        }
    }

    private static String hote(String endpoint) {
        try {
            return java.net.URI.create(endpoint).getHost();
        } catch (Exception e) {
            return "?";
        }
    }

    private static String majuscule(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.FRANCE) + s.substring(1);
    }

    private static String sha256(String texte) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texte.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
