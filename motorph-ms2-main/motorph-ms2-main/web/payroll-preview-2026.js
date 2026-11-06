// Browser-side money helpers only. The 2026 statutory calculation itself runs in Java
// (StatutoryCalculator2026, matched to sweldongpinoy.com); the browser formats its results.

const CENTS_PER_PESO = 100n;

export function parsePesoCents(value, fieldName = 'amount') {
  const text = String(value ?? '').trim().replace(/,/g, '');
  if (!/^\d+(?:\.\d{1,2})?$/.test(text)) {
    throw new TypeError(`${fieldName} must be a non-negative peso amount with up to two decimal places.`);
  }
  const [wholeText, fractionText = ''] = text.split('.');
  return BigInt(wholeText) * CENTS_PER_PESO + BigInt(fractionText.padEnd(2, '0'));
}

export function formatPesoCents(cents) {
  const value = BigInt(cents);
  const sign = value < 0n ? '-' : '';
  const absolute = value < 0n ? -value : value;
  const pesos = (absolute / CENTS_PER_PESO).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const centavos = (absolute % CENTS_PER_PESO).toString().padStart(2, '0');
  return `${sign}₱${pesos}.${centavos}`;
}

/** Converts the Java API's integer centavo fields (names ending in "Cents") to BigInt, recursively. */
export function statutoryFromApi(value) {
  if (Array.isArray(value)) return Object.freeze(value.map(statutoryFromApi));
  if (!value || typeof value !== 'object') return value;
  const converted = {};
  for (const [key, field] of Object.entries(value)) {
    if (key.endsWith('Cents')) {
      if (!Number.isSafeInteger(field)) throw new TypeError(`Unexpected payroll amount for ${key}.`);
      converted[key] = BigInt(field);
    } else {
      converted[key] = statutoryFromApi(field);
    }
  }
  return Object.freeze(converted);
}
