const FALLBACK_IMAGE =
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&h=400&fit=crop';

export { FALLBACK_IMAGE };
/** Resolve relative upload paths for img src (Vite dev proxy or VITE_API_URL in prod). */
export function resolveMediaUrl(url?: string | null): string {
  if (!url) return FALLBACK_IMAGE;
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  const path = url.startsWith('/') ? url : `/${url}`;
  const apiBase = import.meta.env.VITE_API_URL ?? '';
  return apiBase ? `${apiBase.replace(/\/$/, '')}${path}` : path;
}

export function formatStatValue(value: number, suffix = '+'): string {
  if (value >= 1000) return `${Math.floor(value / 1000)}K${suffix}`;
  return value > 0 ? `${value}${suffix}` : '0';
}
