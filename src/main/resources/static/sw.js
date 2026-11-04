/* FLE Présence — service worker
   - rend l'application installable et utilisable hors connexion (dernière version vue)
   - affiche les notifications d'annulation / de report */

// Changer ce numéro force les téléphones à vider l'ancien cache.
const VERSION = 'fle-v3';
const A_PRECHARGER = ['/hors-ligne.html', '/css/app.css', '/js/app.js', '/icons/icon-192.png'];

self.addEventListener('install', (e) => {
    e.waitUntil(caches.open(VERSION).then((c) => c.addAll(A_PRECHARGER)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (e) => {
    e.waitUntil(
        caches.keys()
            .then((cles) => Promise.all(cles.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (e) => {
    const req = e.request;
    if (req.method !== 'GET') return;
    const url = new URL(req.url);

    // Ressources externes (CDN, polices) : le navigateur s'en charge.
    if (url.origin !== self.location.origin) return;

    // Pages : toujours le réseau d'abord (les annulations doivent être à jour),
    // la dernière version enregistrée seulement si l'on est hors connexion.
    if (req.mode === 'navigate') {
        e.respondWith(
            fetch(req)
                .then((rep) => {
                    if (rep.ok && url.pathname.startsWith('/u/')) {
                        const copie = rep.clone();
                        caches.open(VERSION).then((c) => c.put(req, copie));
                    }
                    return rep;
                })
                .catch(() => caches.match(req).then((r) => r || caches.match('/hors-ligne.html')))
        );
        return;
    }

    // Styles et scripts : le réseau d'abord, pour que chaque mise à jour de l'application
    // arrive tout de suite ; la copie enregistrée sert seulement hors connexion.
    if (/^\/(css|js)\//.test(url.pathname)) {
        e.respondWith(
            fetch(req)
                .then((rep) => {
                    if (rep.ok) {
                        const copie = rep.clone();
                        caches.open(VERSION).then((c) => c.put(req, copie));
                    }
                    return rep;
                })
                .catch(() => caches.match(req))
        );
        return;
    }

    // Icônes (elles ne changent pas) : le cache d'abord.
    if (url.pathname.startsWith('/icons/')) {
        e.respondWith(
            caches.match(req).then((r) => r || fetch(req).then((rep) => {
                const copie = rep.clone();
                caches.open(VERSION).then((c) => c.put(req, copie));
                return rep;
            }))
        );
    }
});

// ------------------------------------------------------------------ notifications

self.addEventListener('push', (e) => {
    let donnees = { title: 'Cours de français', body: 'Le planning a changé.', url: '/' };
    try { donnees = Object.assign(donnees, e.data ? e.data.json() : {}); } catch (err) { }
    e.waitUntil(self.registration.showNotification(donnees.title, {
        body: donnees.body,
        icon: '/icons/icon-192.png',
        badge: '/icons/icon-192.png',
        lang: 'fr',
        tag: 'planning',
        renotify: true,
        data: { url: donnees.url }
    }));
});

self.addEventListener('notificationclick', (e) => {
    e.notification.close();
    const cible = (e.notification.data && e.notification.data.url) || '/';
    e.waitUntil(
        self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((fenetres) => {
            for (const f of fenetres) {
                if (new URL(f.url).pathname === cible && 'focus' in f) {
                    f.navigate(cible);
                    return f.focus();
                }
            }
            return self.clients.openWindow(cible);
        })
    );
});
