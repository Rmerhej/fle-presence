package fr.asso.fle.service;

/** Erreur fonctionnelle dont le message peut être affiché tel quel à l'utilisateur. */
public class RegleMetierException extends RuntimeException {

    public RegleMetierException(String message) {
        super(message);
    }
}
