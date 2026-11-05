/* Thème clair / sombre appliqué AVANT l'affichage (évite le flash blanc).
   Chargé dans <head>, sans defer : il doit s'exécuter avant que la page soit dessinée. */
(function () {
    var theme = 'light';
    try {
        theme = localStorage.getItem('fle-theme')
            || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    } catch (e) { }
    document.documentElement.setAttribute('data-bs-theme', theme);
})();
