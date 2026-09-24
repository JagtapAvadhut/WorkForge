/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Semantic tokens mapped to CSS variables (see index.css)
        canvas: 'rgb(var(--wf-canvas) / <alpha-value>)',
        surface: 'rgb(var(--wf-surface) / <alpha-value>)',
        'surface-raised': 'rgb(var(--wf-surface-raised) / <alpha-value>)',
        overlay: 'rgb(var(--wf-overlay) / <alpha-value>)',
        border: 'rgb(var(--wf-border) / <alpha-value>)',
        'border-strong': 'rgb(var(--wf-border-strong) / <alpha-value>)',
        content: {
          DEFAULT: 'rgb(var(--wf-text) / <alpha-value>)',
          muted: 'rgb(var(--wf-text-muted) / <alpha-value>)',
          subtle: 'rgb(var(--wf-text-subtle) / <alpha-value>)',
          inverted: 'rgb(var(--wf-text-inverted) / <alpha-value>)',
        },
        accent: {
          DEFAULT: 'rgb(var(--wf-accent) / <alpha-value>)',
          hover: 'rgb(var(--wf-accent-hover) / <alpha-value>)',
          soft: 'rgb(var(--wf-accent-soft) / <alpha-value>)',
          contrast: 'rgb(var(--wf-accent-contrast) / <alpha-value>)',
        },
        success: 'rgb(var(--wf-success) / <alpha-value>)',
        warning: 'rgb(var(--wf-warning) / <alpha-value>)',
        danger: 'rgb(var(--wf-danger) / <alpha-value>)',
        info: 'rgb(var(--wf-info) / <alpha-value>)',
      },
      fontFamily: {
        sans: ['"Source Sans 3"', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'monospace'],
      },
      borderRadius: {
        sm: '4px',
        DEFAULT: '6px',
        md: '8px',
        lg: '10px',
        xl: '14px',
      },
      boxShadow: {
        card: '0 1px 2px 0 rgb(0 0 0 / 0.04), 0 1px 3px 0 rgb(0 0 0 / 0.06)',
        raised: '0 4px 12px -2px rgb(0 0 0 / 0.12), 0 2px 6px -2px rgb(0 0 0 / 0.08)',
        popover: '0 10px 30px -8px rgb(0 0 0 / 0.25)',
      },
      keyframes: {
        'fade-in': {
          from: { opacity: '0' },
          to: { opacity: '1' },
        },
        'scale-in': {
          from: { opacity: '0', transform: 'translateY(6px) scale(0.98)' },
          to: { opacity: '1', transform: 'translateY(0) scale(1)' },
        },
        'slide-in-right': {
          from: { opacity: '0', transform: 'translateX(16px)' },
          to: { opacity: '1', transform: 'translateX(0)' },
        },
      },
      animation: {
        'fade-in': 'fade-in 0.15s ease-out',
        'scale-in': 'scale-in 0.16s cubic-bezier(0.16, 1, 0.3, 1)',
        'slide-in-right': 'slide-in-right 0.2s ease-out',
      },
    },
  },
  plugins: [],
};
