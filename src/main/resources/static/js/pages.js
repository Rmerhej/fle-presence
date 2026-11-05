/* FLE Présence — scripts de certaines pages : graphiques (tableau de bord, bilan annuel),
   valorisation du bénévolat et QR code de l'affiche.
   Les données des graphiques sont fournies par la page en JSON (<script type="application/json">). */
(function () {
    const COULEURS = ['#3459e6', '#2fb380', '#f4a100', '#da292e', '#7a4ff0', '#14a2b8', '#e85d9c', '#5c677d'];

    const lireDonnees = () => {
        const el = document.getElementById('donnees-graphique');
        if (!el) return { mois: [], series: [] };
        try {
            return JSON.parse(el.textContent) || { mois: [], series: [] };
        } catch (e) {
            return { mois: [], series: [] };
        }
    };

    // ------------------------------------------------ tableau de bord : graphique mensuel
    const canvasTableau = document.getElementById('graphique-mensuel');
    if (canvasTableau && window.Chart) {
        const donnees = lireDonnees();
        const jeux = (indicateur) => donnees.series.map((s, i) => ({
            label: s.unite,
            data: s[indicateur],
            backgroundColor: COULEURS[i % COULEURS.length],
            borderRadius: 6,
            maxBarThickness: 36
        }));

        // Couleurs des axes et légendes adaptées au mode clair / sombre.
        const appliquerTheme = () => {
            const sombre = document.documentElement.getAttribute('data-bs-theme') === 'dark';
            Chart.defaults.color = sombre ? '#9aa3bb' : '#6b7590';
            Chart.defaults.borderColor = sombre ? 'rgba(255,255,255,.08)' : 'rgba(31,42,68,.08)';
        };
        appliquerTheme();

        const graphique = new Chart(canvasTableau, {
            type: 'bar',
            data: { labels: donnees.mois, datasets: jeux('apprenants') },
            options: {
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { usePointStyle: true } } },
                scales: {
                    x: { stacked: true, grid: { display: false } },
                    y: { stacked: true, beginAtZero: true, ticks: { precision: 0 } }
                }
            }
        });

        document.addEventListener('fle-theme', () => {
            appliquerTheme();
            graphique.options.scales.x.ticks.color = Chart.defaults.color;
            graphique.options.scales.y.ticks.color = Chart.defaults.color;
            graphique.options.scales.y.grid.color = Chart.defaults.borderColor;
            graphique.options.plugins.legend.labels.color = Chart.defaults.color;
            graphique.update();
        });

        document.querySelectorAll('input[name="indicateur"]').forEach((r) =>
            r.addEventListener('change', () => {
                graphique.data.datasets = jeux(r.value);
                graphique.update();
            }));
    }

    // ------------------------------------------------ bilan annuel : graphique + valorisation
    const canvasBilan = document.getElementById('graphique-bilan');
    if (canvasBilan && window.Chart) {
        const donnees = lireDonnees();
        Chart.defaults.color = '#6b7590';
        new Chart(canvasBilan, {
            type: 'bar',
            data: {
                labels: donnees.mois,
                datasets: donnees.series.map((s, i) => ({
                    label: s.unite, data: s.apprenants, backgroundColor: COULEURS[i % COULEURS.length],
                    borderRadius: 4, maxBarThickness: 32
                }))
            },
            options: {
                maintainAspectRatio: false, animation: false,
                plugins: { legend: { position: 'bottom', labels: { usePointStyle: true } } },
                scales: { x: { stacked: true, grid: { display: false } }, y: { stacked: true, beginAtZero: true, ticks: { precision: 0 } } }
            }
        });
    }

    // Valorisation du bénévolat (calcul dans le navigateur, rien n'est enregistré)
    const taux = document.getElementById('taux');
    const sortie = document.getElementById('valorisation');
    const blocHeures = document.getElementById('heures-benevolat');
    if (taux && sortie && blocHeures) {
        const heures = parseFloat(blocHeures.dataset.heures || '0');
        taux.addEventListener('input', () => {
            const t = parseFloat(taux.value);
            if (!t || t <= 0) { sortie.classList.add('d-none'); return; }
            const montant = (heures * t).toLocaleString('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 });
            sortie.textContent = 'Valorisation : ' + montant + ' (' + t.toLocaleString('fr-FR') + ' €/h)';
            sortie.classList.remove('d-none');
        });
    }

    // ------------------------------------------------ affiche : QR code
    const zoneQr = document.getElementById('qrcode');
    if (zoneQr && window.QRCode) {
        new QRCode(zoneQr, { text: zoneQr.dataset.lien, width: 240, height: 240, correctLevel: QRCode.CorrectLevel.M });
    }
})();
