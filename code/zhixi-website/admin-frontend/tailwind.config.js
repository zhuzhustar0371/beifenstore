/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js}"],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        theme: {
          bg: {
            1: "var(--color-bg-primary)",
            2: "var(--color-bg-secondary)",
            3: "var(--color-bg-tertiary)",
          },
          text: {
            1: "var(--color-text-primary)",
            2: "var(--color-text-secondary)",
            3: "var(--color-text-muted)",
            inverse: "var(--color-text-inverse)",
          },
          border: "var(--color-border)",
          "border-strong": "var(--color-border-strong)",
          glass: {
            bg: "var(--color-glass-bg)",
            "bg-strong": "var(--color-glass-bg-strong)",
            border: "var(--color-glass-border)",
            highlight: "var(--color-glass-highlight)",
          },
          accent: {
            cyan: "var(--color-accent-cyan)",
            violet: "var(--color-accent-violet)",
            blue: "var(--color-accent-blue)",
            emerald: "var(--color-accent-emerald)",
            orange: "var(--color-accent-orange)",
          },
          shadow: {
            sm: "var(--color-shadow-sm)",
            md: "var(--color-shadow-md)",
            lg: "var(--color-shadow-lg)",
          },
          input: {
            bg: "var(--color-input-bg)",
            border: "var(--color-input-border)",
            "focus-ring": "var(--color-input-focus-ring)",
          },
        },
        spatial: {
          midnight: "#070a1f",
          indigo: "#11134a",
          violet: "#7c3aed",
          lavender: "#c4b5fd",
          electric: "#22d3ee",
          plasma: "#2563eb"
        }
      },
      spacing: {
        "phi-1": "4px",
        "phi-2": "8px",
        "phi-3": "12px",
        "phi-4": "20px",
        "phi-5": "32px"
      },
      fontSize: {
        body: ["clamp(0.95rem, 0.88rem + 0.18vw, 1rem)", { lineHeight: "1.618" }],
        title: ["clamp(1.35rem, 1.05rem + 0.55vw, 1.618rem)", { lineHeight: "1.25" }],
        display: ["clamp(1.9rem, 1.35rem + 1.55vw, 2.618rem)", { lineHeight: "1.08" }]
      },
      boxShadow: {
        glass:
          "0 28px 80px rgba(4, 7, 24, 0.35), inset 0 1px 0 rgba(255,255,255,0.28)",
        "glass-soft": "0 16px 44px rgba(37, 99, 235, 0.22)",
        spatial:
          "0 38px 100px rgba(2, 6, 23, 0.42), 0 12px 36px rgba(37, 99, 235, 0.22), inset 0 1px 0 rgba(255,255,255,0.32)",
        "neon-blue": "0 0 34px rgba(34, 211, 238, 0.38), 0 18px 44px rgba(37, 99, 235, 0.24)",
        "neon-emerald": "0 0 34px rgba(16, 185, 129, 0.32), 0 18px 44px rgba(5, 150, 105, 0.2)",
        "neon-orange": "0 0 34px rgba(249, 115, 22, 0.32), 0 18px 44px rgba(234, 88, 12, 0.2)"
      },
      backgroundImage: {
        "admin-aurora":
          "radial-gradient(circle at 12% 10%, rgba(34,211,238,0.32), transparent 28%), radial-gradient(circle at 82% 8%, rgba(124,58,237,0.28), transparent 30%), radial-gradient(circle at 52% 95%, rgba(37,99,235,0.28), transparent 34%), linear-gradient(135deg, #070a1f 0%, #11134a 46%, #1e1b4b 100%)",
        "spatial-mesh":
          "radial-gradient(circle at 8% 14%, rgba(34,211,238,0.34), transparent 26%), radial-gradient(circle at 78% 8%, rgba(196,181,253,0.32), transparent 27%), radial-gradient(circle at 92% 82%, rgba(37,99,235,0.26), transparent 28%), radial-gradient(circle at 28% 76%, rgba(124,58,237,0.26), transparent 32%), linear-gradient(135deg, #060818 0%, #0f1240 42%, #17133f 100%)",
        "glass-glimmer":
          "linear-gradient(135deg, rgba(255,255,255,0.72), rgba(255,255,255,0.18) 32%, rgba(255,255,255,0.02) 68%)",
        grain:
          "radial-gradient(circle at 20% 30%, rgba(255,255,255,0.16) 0 1px, transparent 1px), radial-gradient(circle at 70% 60%, rgba(255,255,255,0.10) 0 1px, transparent 1px)"
      },
      keyframes: {
        "slow-float": {
          "0%, 100%": { transform: "translate3d(0, 0, 0) scale(1)" },
          "50%": { transform: "translate3d(18px, -24px, 0) scale(1.06)" }
        },
        shimmer: {
          "0%": { transform: "translateX(-120%)" },
          "100%": { transform: "translateX(120%)" }
        },
        "btn-pulse": {
          "0%": { boxShadow: "0 0 0 0 rgba(34, 211, 238, 0.5), 0 18px 44px rgba(37, 99, 235, 0.28)" },
          "100%": { boxShadow: "0 0 0 18px rgba(34, 211, 238, 0), 0 18px 44px rgba(37, 99, 235, 0.28)" }
        },
        "sidebar-icon-hover": {
          "0%": { transform: "scale(1) rotate(0deg)" },
          "40%": { transform: "scale(1.2) rotate(-6deg)" },
          "70%": { transform: "scale(0.95) rotate(3deg)" },
          "100%": { transform: "scale(1.1) rotate(-2deg)" }
        },
        "fade-in-up": {
          "0%": { opacity: "0", transform: "translateY(24px) scale(0.98)" },
          "100%": { opacity: "1", transform: "translateY(0) scale(1)" }
        }
      },
      animation: {
        "slow-float": "slow-float 12s ease-in-out infinite",
        shimmer: "shimmer 3.8s ease-in-out infinite",
        "btn-pulse": "btn-pulse 0.5s ease-out",
        "sidebar-icon-hover": "sidebar-icon-hover 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards",
        "fade-in-up": "fade-in-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards"
      }
    }
  },
  plugins: []
};
