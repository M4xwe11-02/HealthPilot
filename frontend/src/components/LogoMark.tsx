import { motion } from 'framer-motion';

interface LogoMarkProps {
  size?: number;
  /** Show animated entrance (once on mount) */
  animate?: boolean;
}

/**
 * HealthGuard biometric-shield logo mark.
 *
 * The mark is a precision geometric shield composed of:
 *  - Dark-to-deep-forest gradient body with a subtle inner-bevel highlight
 *  - A thin structural grid (crosshair + corner tabs) suggesting medical precision
 *  - An EKG pulse waveform drawn through the horizontal midline
 *  - A mint-glow filter on the pulse line for depth
 */
export default function LogoMark({ size = 36, animate = true }: LogoMarkProps) {
  const uid = 'hg'; // stable — single instance on page

  return (
    <motion.svg
      width={size}
      height={Math.round(size * 1.1)}
      viewBox="0 0 40 44"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      whileHover={{ scale: 1.06 }}
      transition={{ type: 'spring', stiffness: 400, damping: 20 }}
      aria-label="HealthGuard logo mark"
    >
      <defs>
        {/* ── Shield gradient: top jade → deep forest ── */}
        <linearGradient id={`${uid}-body`} x1="20" y1="2" x2="20" y2="42" gradientUnits="userSpaceOnUse">
          <stop offset="0%"   stopColor="#0a7a52" />
          <stop offset="100%" stopColor="#031a0e" />
        </linearGradient>

        {/* ── Shield border: left-edge highlight ── */}
        <linearGradient id={`${uid}-border`} x1="4" y1="0" x2="36" y2="44" gradientUnits="userSpaceOnUse">
          <stop offset="0%"   stopColor="#34d399" stopOpacity="0.55" />
          <stop offset="60%"  stopColor="#059669" stopOpacity="0.30" />
          <stop offset="100%" stopColor="#047857" stopOpacity="0.10" />
        </linearGradient>

        {/* ── EKG line gradient: fade in → bright → fade out ── */}
        <linearGradient id={`${uid}-pulse`} x1="4" y1="22" x2="36" y2="22" gradientUnits="userSpaceOnUse">
          <stop offset="0%"   stopColor="#6ee7b7" stopOpacity="0.0" />
          <stop offset="15%"  stopColor="#6ee7b7" stopOpacity="0.9" />
          <stop offset="50%"  stopColor="#ecfdf5" />
          <stop offset="85%"  stopColor="#34d399" stopOpacity="0.9" />
          <stop offset="100%" stopColor="#34d399" stopOpacity="0.0" />
        </linearGradient>

        {/* ── Glow on EKG peak ── */}
        <filter id={`${uid}-glow`} x="-40%" y="-120%" width="180%" height="340%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="1.2" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>

        {/* ── Drop shadow for depth ── */}
        <filter id={`${uid}-shadow`} x="-20%" y="-10%" width="140%" height="130%">
          <feDropShadow dx="0" dy="2" stdDeviation="2.5" floodColor="#000" floodOpacity="0.35" />
        </filter>

        {/* ── Clip to shield shape ── */}
        <clipPath id={`${uid}-clip`}>
          <path d="M20 2.5 L35.5 8.8 L35.5 24.8 C35.5 33.6 20 42 20 42 C20 42 4.5 33.6 4.5 24.8 L4.5 8.8 Z" />
        </clipPath>
      </defs>

      {/* ── Shadow layer ── */}
      <path
        d="M20 2.5 L35.5 8.8 L35.5 24.8 C35.5 33.6 20 42 20 42 C20 42 4.5 33.6 4.5 24.8 L4.5 8.8 Z"
        fill="#000"
        opacity="0.28"
        transform="translate(0 1.5)"
        filter={`url(#${uid}-shadow)`}
      />

      {/* ── Shield body ── */}
      <path
        d="M20 2.5 L35.5 8.8 L35.5 24.8 C35.5 33.6 20 42 20 42 C20 42 4.5 33.6 4.5 24.8 L4.5 8.8 Z"
        fill={`url(#${uid}-body)`}
      />

      {/* ── Inner highlight bevel (top arc) ── */}
      <path
        d="M20 5.5 L32.5 10.5 L32.5 12.5 L20 7.8 L7.5 12.5 L7.5 10.5 Z"
        fill="white"
        opacity="0.10"
      />

      {/* ── Structural grid: horizontal midline ── */}
      <line
        x1="6.5" y1="22" x2="13.5" y2="22"
        stroke="#34d399" strokeWidth="0.35" opacity="0.30"
        clipPath={`url(#${uid}-clip)`}
      />
      <line
        x1="26.5" y1="22" x2="33.5" y2="22"
        stroke="#34d399" strokeWidth="0.35" opacity="0.30"
        clipPath={`url(#${uid}-clip)`}
      />

      {/* ── Structural grid: vertical center ── */}
      <line
        x1="20" y1="6" x2="20" y2="14.5"
        stroke="#34d399" strokeWidth="0.35" opacity="0.20"
        clipPath={`url(#${uid}-clip)`}
      />
      <line
        x1="20" y1="29.5" x2="20" y2="39"
        stroke="#34d399" strokeWidth="0.35" opacity="0.20"
        clipPath={`url(#${uid}-clip)`}
      />

      {/* ── Corner tab marks (precision reticle feel) ── */}
      {/* top-left tab */}
      <path d="M7.5 11.5 L7.5 14 M7.5 11.5 L10 11.5" stroke="#34d399" strokeWidth="0.6" opacity="0.45" strokeLinecap="round" />
      {/* top-right tab */}
      <path d="M32.5 11.5 L32.5 14 M32.5 11.5 L30 11.5" stroke="#34d399" strokeWidth="0.6" opacity="0.45" strokeLinecap="round" />
      {/* mid-left */}
      <circle cx="6.5" cy="22" r="0.8" fill="#34d399" opacity="0.40" />
      {/* mid-right */}
      <circle cx="33.5" cy="22" r="0.8" fill="#34d399" opacity="0.40" />

      {/* ── EKG pulse waveform ── */}
      {/*
        Waveform (y=22 baseline, 40px wide):
        6→10  flat approach
        10→13 ramp up  (pre-bump)
        13→14.5 small bump (P-wave analog)
        14.5→16 return to baseline
        16→17 QRS down-deflection
        17→18.5 sharp QRS spike peak (y=11)
        18.5→20 return through baseline to S-wave trough (y=27)
        20→21.5 return to baseline
        21.5→23 T-wave hump
        23→24.5 return to baseline
        24.5→34  flat tail
      */}
      <motion.path
        d="
          M 6 22
          L 12.5 22
          L 13.8 19.8
          L 15.2 22
          L 16 22
          L 16.8 25.5
          L 17.8 11
          L 18.8 27.5
          L 19.6 22
          L 21.4 22
          L 22.2 18.5
          L 23.2 22
          L 27.5 22
          L 34 22
        "
        stroke={`url(#${uid}-pulse)`}
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
        filter={`url(#${uid}-glow)`}
        clipPath={`url(#${uid}-clip)`}
        initial={animate ? { pathLength: 0, opacity: 0 } : false}
        animate={animate ? { pathLength: 1, opacity: 1 } : false}
        transition={{ duration: 1.4, ease: [0.4, 0, 0.2, 1], delay: 0.25 }}
      />

      {/* ── QRS peak highlight dot ── */}
      <motion.circle
        cx="17.8" cy="11"
        r="1.2"
        fill="#ecfdf5"
        opacity="0.9"
        filter={`url(#${uid}-glow)`}
        initial={animate ? { scale: 0, opacity: 0 } : false}
        animate={animate ? { scale: 1, opacity: 0.9 } : false}
        transition={{ duration: 0.3, delay: 1.1 }}
        style={{ transformOrigin: '17.8px 11px' }}
      />

      {/* ── Shield border ── */}
      <path
        d="M20 2.5 L35.5 8.8 L35.5 24.8 C35.5 33.6 20 42 20 42 C20 42 4.5 33.6 4.5 24.8 L4.5 8.8 Z"
        stroke={`url(#${uid}-border)`}
        strokeWidth="0.7"
        fill="none"
      />
    </motion.svg>
  );
}
