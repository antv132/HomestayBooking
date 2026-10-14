/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        // Primary (Headings) — Geometric, modern, elegant
        display: ['"Outfit"', 'system-ui', 'sans-serif'],
        // Secondary (Body/UI) — Clean, highly legible for tables and forms
        sans:    ['"Plus Jakarta Sans"', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'sans-serif'],
        mono:    ['"JetBrains Mono"', '"Fira Code"', 'monospace'],
      },
      colors: {
        // ── Brand / Primary (Teal — Calm, Trustworthy) ──
        primary:              '#0F766E', // Teal 600
        'primary-hover':      '#0D9488', // Teal 500
        'primary-light':      '#CCFBF1', // Teal 50 — subtle highlights & focus ring

        // ── Surfaces ──
        'surface-canvas':     '#F8FAFC', // Slate 50  — default page background
        'surface-card':       '#FFFFFF', // White     — cards, dropdowns, modals
        'surface-inverted':   '#0F172A', // Slate 900 — sidebar, footer

        // ── Text / Ink ──
        'text-primary':       '#1E293B', // Slate 800
        'text-secondary':     '#64748B', // Slate 500
        'text-inverted':      '#FFFFFF',

        // ── Borders ──
        'border-subtle':      '#F1F5F9', // Slate 100 — table row dividers
        'border-base':        '#E2E8F0', // Slate 200 — input borders, card borders

        // ── Semantic Status ──
        success:              '#10B981', // Emerald 500
        warning:              '#F59E0B', // Amber 500
        danger:               '#EF4444', // Red 500
        info:                 '#3B82F6', // Blue 500

        // ── Legacy aliases (keep for backward compat in pages that use them) ──
        canvas:               '#F8FAFC',
        card:                 '#FFFFFF',
        ink:                  '#1E293B',
      },
      borderRadius: {
        none: '0px',
        sm:   '4px',   // radius-sm — checkboxes, tags
        md:   '8px',   // radius-md — inputs, buttons
        lg:   '16px',  // radius-lg — cards, modals
        full: '9999px', // avatars, badges
      },
      spacing: {
        // 4px base scale per DESIGN.md
        1:  '4px',
        2:  '8px',
        3:  '12px',
        4:  '16px',
        6:  '24px',
        8:  '32px',
        12: '48px',
        16: '64px',
        // Named aliases kept for existing pages
        xs:  '4px',
        sm:  '8px',
        md:  '12px',
        lg:  '16px',
        xl:  '24px',
        xxl: '32px',
        xxxl:'48px',
        section: '96px',
        band: '160px',
      },
      boxShadow: {
        // Per DESIGN.md elevation tokens
        sm:      '0 1px 2px rgba(0,0,0,0.05)',                      // shadow-sm — buttons, inputs on hover
        md:      '0 4px 6px -1px rgba(0,0,0,0.1)',                  // shadow-md — dropdowns
        lg:      '0 10px 15px -3px rgba(0,0,0,0.05)',               // shadow-lg — cards, modals (very soft)
        // Focus ring helper
        'focus-teal': '0 0 0 3px #CCFBF1',
        // Legacy
        xs:      '0 1px 2px rgba(32,32,32,0.06)',
        float:   '0 8px 24px rgba(32,32,32,0.08)',
        primary: '0 4px 16px rgba(15,118,110,0.30)',
      },
    },
  },
  plugins: [],
};
