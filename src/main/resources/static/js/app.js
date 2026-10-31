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
