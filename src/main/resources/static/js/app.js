/* FLE Présence — petits comportements communs */
document.addEventListener('DOMContentLoaded', () => {

    // Bascule mode clair / sombre (choix mémorisé dans le navigateur).
    const boutonTheme = document.getElementById('bouton-theme');
    if (boutonTheme) {
        boutonTheme.addEventListener('click', () => {
            const html = document.documentElement;
            const nouveau = html.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
            html.setAttribute('data-bs-theme', nouveau);
            try { localStorage.setItem('fle-theme', nouveau); } catch (e) { }
            document.dispatchEvent(new CustomEvent('fle-theme', { detail: nouveau }));
        });
    }

    // Affiche automatiquement les toasts présents dans la page (messages, alertes de séances).
    document.querySelectorAll('.toast[data-auto="true"]').forEach((el, i) => {
        setTimeout(() => bootstrap.Toast.getOrCreateInstance(el).show(), 250 * i);
    });

    // Boutons + / − des compteurs de saisie.
    document.querySelectorAll('[data-compteur]').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = document.getElementById(btn.dataset.compteur);
            const pas = parseInt(btn.dataset.pas, 10) || 0;
            const min = parseInt(input.min || '0', 10);
            const max = parseInt(input.max || '500', 10);
            const valeur = (parseInt(input.value, 10) || 0) + pas;
            input.value = Math.max(min, Math.min(max, valeur));
        });
    });

    // Confirmation avant une action (suppression...).
    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', e => {
            if (!confirm(form.dataset.confirm)) {
                e.preventDefault();
            }
        });
    });

    // Fenêtres « Annuler » / « Déplacer » une séance : on remplit le formulaire avec la séance choisie.
    document.querySelectorAll('.modal[data-seance-modal]').forEach(modal => {
        modal.addEventListener('show.bs.modal', e => {
            const bouton = e.relatedTarget;
            if (!bouton) return;
            const form = modal.querySelector('form');
            form.action = bouton.dataset.action;
            modal.querySelectorAll('[data-libelle-seance]').forEach(el => el.textContent = bouton.dataset.libelle);
            const date = form.querySelector('input[name="date"]');
            if (date && bouton.dataset.date) date.value = bouton.dataset.date;
            const debut = form.querySelector('input[name="debut"]');
            if (debut && bouton.dataset.debut) debut.value = bouton.dataset.debut;
            const fin = form.querySelector('input[name="fin"]');
            if (fin && bouton.dataset.fin) fin.value = bouton.dataset.fin;
        });
    });

    // Copier un lien dans le presse-papiers.
    document.querySelectorAll('[data-copier]').forEach(btn => {
        btn.addEventListener('click', async () => {
            try {
                await navigator.clipboard.writeText(btn.dataset.copier);
                const texte = btn.innerHTML;
                btn.innerHTML = '<i class="bi bi-check2 me-1"></i>Copié';
                setTimeout(() => btn.innerHTML = texte, 1800);
            } catch (e) {
                prompt('Copiez ce lien :', btn.dataset.copier);
            }
        });
    });
});

/* ======================================================================
   Application installable + notifications des annulations
   ====================================================================== */
(() => {
    if (!('serviceWorker' in navigator)) return;

    const estIOS = /iphone|ipad|ipod/i.test(navigator.userAgent);
    const installee = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;

    // ------------------------------------------------ installation
    let invitation = null;
    window.addEventListener('beforeinstallprompt', (e) => {
        e.preventDefault();
        invitation = e;
        const b = document.getElementById('bouton-installer');
        if (b) b.classList.remove('d-none');
    });

    const toast = (texte, couleur = 'success') => {
        const zone = document.getElementById('zone-toasts') || document.querySelector('.toast-container');
        if (!zone) { alert(texte); return; }
        const el = document.createElement('div');
        el.className = `toast align-items-center text-bg-${couleur} border-0`;
        el.setAttribute('role', 'status');
        el.innerHTML = '<div class="d-flex"><div class="toast-body"></div>'
            + '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Fermer"></button></div>';
        el.querySelector('.toast-body').textContent = texte;
        zone.appendChild(el);
        bootstrap.Toast.getOrCreateInstance(el, { delay: 6000 }).show();
    };

    const base64VersOctets = (b64) => {
        const rembourrage = '='.repeat((4 - (b64.length % 4)) % 4);
        const brut = atob((b64 + rembourrage).replace(/-/g, '+').replace(/_/g, '/'));
        return Uint8Array.from([...brut].map((c) => c.charCodeAt(0)));
    };

    const envoyer = (url, corps) => fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(corps)
    });

    window.addEventListener('load', async () => {
        let enregistrement;
        try {
            enregistrement = await navigator.serviceWorker.register('/sw.js');
        } catch (e) {
            return;
        }

        const boutonInstaller = document.getElementById('bouton-installer');
        if (boutonInstaller) {
            boutonInstaller.addEventListener('click', async () => {
                if (!invitation) return;
                invitation.prompt();
                await invitation.userChoice;
                invitation = null;
                boutonInstaller.classList.add('d-none');
            });
        }

        // ------------------------------------------------ notifications
        const bouton = document.getElementById('bouton-notifications');
        if (!bouton) return;

        // Sur iPhone, les notifications ne marchent qu'une fois l'appli installée sur l'écran d'accueil.
        if (estIOS && !installee) {
            const aide = document.getElementById('aide-ios');
            if (aide) aide.classList.remove('d-none');
            return;
        }
        if (!('PushManager' in window) || !('Notification' in window)) return;

        let reglages;
        try {
            reglages = await (await fetch('/push/cle')).json();
        } catch (e) {
            return;
        }
        if (!reglages.actives) return;

        const libelle = bouton.querySelector('span');
        const afficher = (abonne) => {
            bouton.classList.toggle('btn-outline-primary', !abonne);
            bouton.classList.toggle('btn-success', abonne);
            bouton.querySelector('i').className = abonne ? 'bi bi-bell-fill me-1' : 'bi bi-bell me-1';
            libelle.textContent = abonne ? 'Alertes activées' : 'Me prévenir';
            bouton.title = abonne ? 'Touchez pour ne plus recevoir les alertes' : 'Recevoir une alerte si un cours est annulé';
        };

        let abonnement = await enregistrement.pushManager.getSubscription();
        afficher(!!abonnement);
        bouton.classList.remove('d-none');

        bouton.addEventListener('click', async () => {
            bouton.disabled = true;
            try {
                if (abonnement) {
                    await envoyer('/push/desabonnement', { endpoint: abonnement.endpoint });
                    await abonnement.unsubscribe();
                    abonnement = null;
                    afficher(false);
                    toast('Vous ne recevrez plus les alertes.', 'secondary');
                    return;
                }
                const permission = await Notification.requestPermission();
                if (permission !== 'granted') {
                    toast('Les alertes sont bloquées. Autorisez les notifications dans les réglages du navigateur.', 'warning');
                    return;
                }
                abonnement = await enregistrement.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: base64VersOctets(reglages.cle)
                });
                const json = abonnement.toJSON();
                const rep = await envoyer('/push/abonnement', {
                    unite: bouton.dataset.unite,
                    groupe: bouton.dataset.groupe ? Number(bouton.dataset.groupe) : null,
                    endpoint: json.endpoint,
                    keys: json.keys
                });
                if (!rep.ok) throw new Error('refus');
                afficher(true);
                toast('C\'est noté ! Vous serez prévenu si un cours est annulé ou déplacé.');
            } catch (e) {
                toast('Impossible d\'activer les alertes sur cet appareil.', 'danger');
            } finally {
                bouton.disabled = false;
            }
        });
    });
})();
