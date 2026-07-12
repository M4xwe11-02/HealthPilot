import { motion } from 'framer-motion';

interface PageHeaderProps {
  /** Tiny italic label shown above the title (e.g. "Health Analytics") */
  label: string;
  /** Main title text */
  title: string;
  /** Optional secondary subtitle below the title */
  subtitle?: string;
  /** Optional content placed to the right of the header text block (actions, search, etc.) */
  action?: React.ReactNode;
  className?: string;
}

/**
 * Unified page-section heading using the HealthGuard design language:
 *
 *   HEALTH ANALYTICS  ────◈
 *   报告库
 *   管理您已分析过的所有体检报告
 */
export default function PageHeader({
  label,
  title,
  subtitle,
  action,
  className = '',
}: PageHeaderProps) {
  return (
    <motion.div
      className={`flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 sm:gap-6 mb-3 sm:mb-5 ${className}`}
      initial={{ opacity: 0, y: -12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
    >
      {/* ── Text block ── */}
      <div className="min-w-0">

        {/* Label row: tiny italic + gradient rule + diamond */}
        <div className="flex items-center gap-2.5 mb-[7px]">
          <span
            className="font-display italic font-normal uppercase select-none
              text-primary-500 dark:text-primary-400"
            style={{ fontSize: '10px', letterSpacing: '0.38em' }}
          >
            {label}
          </span>
          <div className="h-px w-16 bg-gradient-to-r from-primary-400/50 to-transparent flex-shrink-0" />
          <div
            className="w-[5px] h-[5px] rotate-45 flex-shrink-0 bg-primary-400
              shadow-[0_0_6px_rgba(52,211,153,0.75)]"
          />
        </div>

        {/* Main title */}
        <h1
          className="font-display font-bold text-[1.65rem] sm:text-[2rem] text-slate-900 dark:text-white
            leading-none tracking-tight"
        >
          {title}
        </h1>

        {/* Subtitle */}
        {subtitle && (
          <p className="text-[13px] text-slate-500 dark:text-forest-200 mt-2 leading-relaxed">
            {subtitle}
          </p>
        )}
      </div>

      {/* ── Right-side action ── */}
      {action && (
        <div className="w-full sm:w-auto flex items-center gap-2.5 flex-shrink-0 sm:mt-1 overflow-x-auto pb-1 sm:pb-0 scrollbar-none">
          {action}
        </div>
      )}
    </motion.div>
  );
}
