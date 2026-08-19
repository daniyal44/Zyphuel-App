import React from 'react';
import { Helmet } from 'react-helmet';

/**
 * SEOManager Component for Zyphuel (React JS)
 * 
 * Programmatically injects high-priority SEO, AEO, and GEO Meta tags,
 * Open Graph (OG) social tags, Twitter/X cards, and JSON-LD SoftwareApplication schema
 * for the Zyphuel delivery platform using React Helmet.
 */
export default function SEOManager() {
  const softwareSchema = {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    "name": "Zyphuel",
    "operatingSystem": "Android",
    "applicationCategory": "BusinessApplication, Utilities",
    "downloadUrl": "https://zyphuel.netlify.app/zyphuel.apk",
    "featureList": [
      "Premium Super Petrol Euro V & High-Octane delivery",
      "Safe LPG Gas cylinder refills with pressure-tested seals",
      "Purified premium mineral water gallon direct delivery",
      "Active live location GPS tracking with ETA calculation",
      "One-tap standby rider automated routing system",
      "Offline-resilient dynamic cache architecture"
    ],
    "screenshot": "https://zyphuel.netlify.app/screenshot-dashboard.jpg",
    "offers": {
      "@type": "Offer",
      "price": "0.00",
      "priceCurrency": "PKR",
      "availability": "https://schema.org/InStock"
    },
    "aggregateRating": {
      "@type": "AggregateRating",
      "ratingValue": "4.9",
      "ratingCount": "1040",
      "bestRating": "5",
      "worstRating": "1"
    },
    "publisher": {
      "@type": "Organization",
      "name": "Zyphuel Logistics",
      "logo": "https://zyphuel.netlify.app/logo-vector.png",
      "url": "https://zyphuel.netlify.app/"
    }
  };

  return (
    <Helmet>
      {/* Set Document Title */}
      <title>Zyphuel App - Premium Fuel Delivery & Real-Time Logistics</title>

      {/* Standard Meta Tags & Open Graph Properties */}
      <meta name="description" content="Download the Zyphuel App: Lahore's ultra-premium on-demand fuel delivery, LPG gas cylinder refill, and mineral water logistics service. Live real-time location tracking & 100% transparent pricing." />
      <meta name="keywords" content="premium fuel delivery, real-time logistics, Zyphuel app, fuel app Lahore, on-demand petrol delivery, LPG cylinder refill Lahore, mineral water delivery app, download Android APK, automatic routing fuel, roadside fuel assistance, Lahore logistics app" />
      <meta name="robots" content="index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1" />
      <meta name="author" content="Zyphuel Logistics LLC" />
      
      {/* Geo-Targeting Metadata */}
      <meta name="geo.region" content="PK-PB" />
      <meta name="geo.placename" content="Lahore" />
      <meta name="geo.position" content="31.5204;74.3587" />
      <meta name="ICBM" content="31.5204, 74.3587" />

      {/* Open Graph / Facebook Cards */}
      <meta property="og:type" content="website" />
      <meta property="og:url" content="https://zyphuel.netlify.app/" />
      <meta property="og:title" content="Zyphuel App: Premium Fuel Delivery & Real-Time Logistics" />
      <meta property="og:description" content="Skip the station lines. Get premium super petrol, safe LPG gas cylinder refills, and purified mineral water gallons delivered straight to your exact coordinates with active live tracking." />
      <meta property="og:image" content="https://zyphuel.netlify.app/og-image.jpg" />
      <meta property="og:site_name" content="Zyphuel" />
      <meta property="og:locale" content="en_US" />

      {/* Twitter / X Cards */}
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:url" content="https://zyphuel.netlify.app/" />
      <meta name="twitter:title" content="Zyphuel App - Premium Fuel & Real-Time Logistics Tracker" />
      <meta name="twitter:description" content="Tesla-inspired, super high-speed logistics app for immediate petrol, gas, and mineral water delivery in Lahore. Direct-to-consumer delivery corridor." />
      <meta name="twitter:image" content="https://zyphuel.netlify.app/og-image.jpg" />
      <meta name="twitter:site" content="@ZyphuelApp" />

      {/* Structured Data JSON-LD (SoftwareApplication) */}
      <script type="application/ld+json">
        {JSON.stringify(softwareSchema)}
      </script>
    </Helmet>
  );
}
