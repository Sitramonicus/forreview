#!/usr/bin/env sh
# Compiles web/main.css into web/app.css, the stylesheet the browser actually loads.
#
# main.css is a Tailwind v4 *source* file: it is written in @apply and needs a compile step before a browser can read
# it. Serving it raw leaves the page completely unstyled, which is exactly what happened before this script existed.
#
# The font @imports are stripped because the page is served from disk and those packages live in npm, not the web root.
# Inter and Montserrat fall back to the system stack the theme already lists.
set -eu
cd "$(dirname "$0")/.."
TW=tools/twcss

if [ ! -d "$TW/node_modules/tailwindcss" ]; then
  echo "Installing the Tailwind compiler (one time) ..."
  ( cd "$TW" && npm install tailwindcss@4 @tailwindcss/cli@4 --no-save --silent )
fi

# The theme's own @import lines are dropped; everything after them is the real design.
{
  printf '@import "tailwindcss" source(none);\n@source "../../web/b_basic/*.html";\n@source "../../web/*.html";\n@source "../../web/b_basic/*.js";\n@source "../../web/f_flares/*.js";\n\n'
  sed -e '/^@import "tailwindcss";/d' \
      -e '/^@import "@fontsource-variable\/inter";/d' \
      -e '/^@import "@fontsource-variable\/montserrat";/d' web/main.css
} > "$TW/input.css"

( cd "$TW" && npx tailwindcss --input input.css --output ../../web/app.css --minify ) >/dev/null 2>&1
printf 'web/app.css rebuilt (%s bytes)\n' "$(wc -c < web/app.css)"
