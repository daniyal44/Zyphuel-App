/**
 * ZYPHUEL: OFFLINE-RESILIENT SERVICE WORKER
 * 
 * This service worker provides immediate local caching and offline capabilities for the 
 * Zyphuel web portal (https://zyphuel.netlify.app/). It ensures critical resources, 
 * assets, metadata guidelines, and schemas are fully offline-accessible during intermittent 
 * network disruptions or cell-tower handover latency in Lahore, Pakistan.
 */

const CACHE_VERSION = 'v1.1.0';
const CACHE_NAME = `zyphuel-cache-${CACHE_VERSION}`;

// Critical resources to cache immediately on installation
const CRITICAL_ASSETS = [
  '/',
  '/index.html',
  '/robots.txt',
  '/sitemap.xml',
  '/WEBSITE_SEO_AEO_GEO_META_TAGS.html',
  '/WEBSITE_SEO_MANAGER.jsx',
  '/SEOManager.js',
  '/WEBSITE_INTEGRATION_AND_PLAY_STORE_GUIDE.md',
  '/MASTER_GLOBAL_SEO_AEO_GEO_STRATEGY.md',
  '/PLAY_STORE_ASO_BLUEPRINT.md'
];

// 1. INSTALL EVENT - Pre-cache critical application shells & documentation
self.addEventListener('install', event => {
  console.log('📦 Zyphuel ServiceWorker: Installing and caching critical offline shell...');
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        return cache.addAll(CRITICAL_ASSETS);
      })
      .then(() => {
        console.log('🎯 Zyphuel ServiceWorker: All core assets successfully pre-cached!');
        return self.skipWaiting(); // Force the waiting service worker to become the active one immediately
      })
      .catch(err => {
        console.error('❌ Zyphuel ServiceWorker: Error pre-caching assets on install:', err);
      })
  );
});

// 2. ACTIVATE EVENT - Clean up stale cache versions and claim clients
self.addEventListener('activate', event => {
  console.log('⚡ Zyphuel ServiceWorker: Activating and cleaning up obsolete caches...');
  event.waitUntil(
    caches.keys().then(cacheKeys => {
      return Promise.all(
        cacheKeys.map(key => {
          if (key !== CACHE_NAME) {
            console.log(`🧹 Zyphuel ServiceWorker: Deleting stale cache: ${key}`);
            return caches.delete(key);
          }
        })
      );
    }).then(() => {
      console.log('🚀 Zyphuel ServiceWorker: System is active and fully controlling clients.');
      return self.clients.claim(); // Take control of all open pages immediately
    })
  );
});

// 3. FETCH EVENT - Intelligent cache interception and delivery strategies
self.addEventListener('fetch', event => {
  const requestUrl = new URL(event.request.url);

  // Focus only on GET requests for standard caching
  if (event.request.method !== 'GET') {
    return;
  }

  // Strategy A: Cache-First with Network Fallback (For Static Local Assets & Shell)
  if (CRITICAL_ASSETS.includes(requestUrl.pathname) || event.request.destination === 'image' || event.request.destination === 'font') {
    event.respondWith(
      caches.match(event.request)
        .then(cachedResponse => {
          if (cachedResponse) {
            // Return cached response instantly
            console.log(`⚡ Zyphuel Cache Hits: Servicing ${requestUrl.pathname} directly from local storage.`);
            return cachedResponse;
          }

          // Fallback to fetch from the live network if not in cache
          return fetch(event.request).then(networkResponse => {
            if (!networkResponse || networkResponse.status !== 200 || networkResponse.type !== 'basic') {
              return networkResponse;
            }

            // Dynamically cache newly fetched static assets
            const responseToCache = networkResponse.clone();
            caches.open(CACHE_NAME).then(cache => {
              cache.put(event.request, responseToCache);
            });

            return networkResponse;
          }).catch(err => {
            console.error(`❌ Zyphuel Network Error: Offline static fetch failed for ${requestUrl.pathname}`, err);
          });
        })
    );
  } else {
    // Strategy B: Network-First with Cache Fallback (For Dynamic, Documentation, or API Resources)
    // This allows Lahore users to view the most updated live schemas/logs, but fall back gracefully to cache during connection loss.
    event.respondWith(
      fetch(event.request)
        .then(networkResponse => {
          // If valid response, clone and cache it dynamically
          if (networkResponse && networkResponse.status === 200) {
            const responseToCache = networkResponse.clone();
            caches.open(CACHE_NAME).then(cache => {
              cache.put(event.request, responseToCache);
            });
          }
          return networkResponse;
        })
        .catch(err => {
          console.warn(`🌐 Zyphuel Network Lost: Falling back to local cache for ${requestUrl.pathname}`);
          return caches.match(event.request).then(cachedResponse => {
            if (cachedResponse) {
              return cachedResponse;
            }

            // Ultimate offline HTML fallback if navigating to a page offline
            if (event.request.headers.get('accept').includes('text/html')) {
              return caches.match('/index.html');
            }

            return Promise.reject('Offline fallback unavailable.');
          });
        })
    );
  }
});
