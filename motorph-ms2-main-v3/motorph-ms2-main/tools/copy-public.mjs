import { copyFileSync, mkdirSync } from 'node:fs';

mkdirSync('dist', { recursive: true });
for (const file of ['robots.txt', 'sitemap.xml', 'favicon.svg', 'manifest.webmanifest', 'motorph-logo.png', 'motorph-logo-transparent.png']) {
  copyFileSync(`web/${file}`, `dist/${file}`);
}
