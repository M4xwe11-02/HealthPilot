import {FormEvent, useState} from 'react';
import {Navigate, useLocation, useNavigate} from 'react-router-dom';
import {AlertCircle, Activity, KeyRound, UserRound} from 'lucide-react';
import {motion, AnimatePresence} from 'framer-motion';
import {useAuth} from '../auth/AuthContext';
import LogoMark from '../components/LogoMark';

/* ── Decorative background SVG for left panel ── */
function HexPattern() {
  return (
    <svg
      className="absolute inset-0 w-full h-full opacity-[0.04]"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <pattern id="hex" x="0" y="0" width="52" height="60" patternUnits="userSpaceOnUse">
          <polygon
            points="26,2 50,14 50,46 26,58 2,46 2,14"
            fill="none"
            stroke="#34d399"
            strokeWidth="1"
          />
        </pattern>
      </defs>
      <rect width="100%" height="100%" fill="url(#hex)" />
    </svg>
  );
}

/* ── Glassmorphism Card for left panel ── */
function GlassCard({label, value}: {label: string; value: string}) {
  return (
    <div className="relative overflow-hidden flex items-center gap-2.5 rounded-xl border border-white/10 bg-white/5 px-4 py-3 backdrop-blur-xl shadow-lg hover:bg-white/10 transition-all duration-500 group cursor-default">
      {/* Subtle top glare */}
      <div className="absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-white/30 to-transparent opacity-50" />
      {/* Hover glow effect */}
      <div className="absolute -inset-full bg-gradient-to-tr from-transparent via-white/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-700 transform group-hover:translate-x-1/2" />
      
      <div className="w-1.5 h-1.5 rounded-full bg-primary-400 animate-pulse shadow-[0_0_8px_rgba(52,211,153,0.8)] relative z-10" />
      <div className="relative z-10">
        <p className="text-[11px] text-primary-300/80 uppercase tracking-wider font-medium">{label}</p>
        <p className="text-white/90 text-sm font-medium mt-0.5">{value}</p>
      </div>
    </div>
  );
}

export default function LoginPage() {
  const {isAuthenticated, login, register} = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as {from?: {pathname?: string}} | null)?.from?.pathname || '/upload';

  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  if (isAuthenticated) return <Navigate to={from} replace />;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (mode === 'login') {
        await login({username, password});
      } else {
        await register({username, password, displayName});
      }
      navigate(from, {replace: true});
    } catch (err) {
      setError(err instanceof Error ? err.message : '操作失败');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-forest-950 font-sans">

      {/* ── Left panel ── */}
      <section className="hidden lg:flex w-[46%] min-h-screen flex-col justify-between relative overflow-hidden
        bg-gradient-to-br from-forest-900 via-forest-800 to-forest-950
        shadow-2xl after:content-[''] after:absolute after:top-0 after:right-0 after:w-[1px] after:h-full after:bg-gradient-to-b after:from-transparent after:via-forest-700/50 after:to-transparent">

        {/* Hexagonal grid pattern */}
        <HexPattern />

        {/* Radial jade glows */}
        <div className="absolute -top-32 -left-32 w-[36rem] h-[36rem] rounded-full
          bg-primary-500/10 blur-[100px] pointer-events-none" />
        <div className="absolute bottom-0 right-0 w-[28rem] h-[28rem] rounded-full
          bg-primary-600/15 blur-[120px] pointer-events-none" />

        {/* Top — brand wordmark */}
        <motion.div
          className="relative z-10 p-12"
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        >
          <div className="flex items-start gap-5">

            {/* Shield mark — larger, no entrance animation (handled by parent) */}
            <div className="mt-[5px] flex-shrink-0 drop-shadow-[0_0_18px_rgba(52,211,153,0.28)]">
              <LogoMark size={54} animate={false} />
            </div>

            {/* Wordmark stack */}
            <div className="flex flex-col select-none">

              {/* "HEALTH" — ghost italic light caps */}
              <span
                className="font-display italic font-normal uppercase leading-none tracking-[0.44em] text-white/45"
                style={{ fontSize: '11px', letterSpacing: '0.44em' }}
              >
                Health
              </span>

              {/* Decorative rule: ────◈──── */}
              <div className="my-[7px] flex items-center gap-0">
                <div className="h-px flex-1 bg-gradient-to-r from-primary-400/60 to-primary-400/20" />
                <div
                  className="mx-[7px] w-[6px] h-[6px] rotate-45 flex-shrink-0
                    bg-primary-400 shadow-[0_0_8px_rgba(52,211,153,0.9)]"
                />
                <div className="h-px w-4 bg-primary-400/20" />
              </div>

              {/* "GUARD" — massive jade gradient logotype */}
              <motion.span
                className="font-display font-bold leading-none"
                style={{
                  fontSize: '2.75rem',
                  letterSpacing: '-0.03em',
                  background: 'linear-gradient(128deg, #a7f3d0 0%, #34d399 30%, #059669 75%, #064e3b 100%)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  filter: 'drop-shadow(0 2px 14px rgba(16,185,129,0.45))',
                }}
                initial={{ opacity: 0, x: -8 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.55, delay: 0.18, ease: [0.22, 1, 0.36, 1] }}
              >
                GUARD
              </motion.span>

              {/* Subtitle */}
              <span
                className="font-sans font-normal uppercase text-primary-400/50 mt-[9px] tracking-[0.35em] leading-none"
                style={{ fontSize: '7.5px' }}
              >
                Intelligent Health Platform
              </span>
            </div>
          </div>
        </motion.div>

        {/* Middle — headline */}
        <motion.div
          initial={{opacity: 0, y: 20}}
          animate={{opacity: 1, y: 0}}
          transition={{duration: 0.5, delay: 0.1}}
          className="relative z-10 px-10 space-y-8"
        >
          <div className="space-y-6">
            {/* Label + diamond separator */}
            <div className="flex items-center gap-2.5">
              <div className="h-px w-8 bg-gradient-to-r from-transparent to-primary-400/60" />
              <div className="w-[5px] h-[5px] rotate-45 bg-primary-400 shadow-[0_0_7px_rgba(52,211,153,0.8)]" />
              <span
                className="font-display italic font-normal uppercase text-primary-400/80 select-none"
                style={{ fontSize: '10px', letterSpacing: '0.38em' }}
              >
                Personal Health Workspace
              </span>
            </div>
            <h2 className="font-display text-[3rem] xl:text-[4rem] font-extrabold leading-[1.15] tracking-tight text-transparent bg-clip-text bg-gradient-to-b from-white via-white/95 to-primary-200">
              体检报告<br />AI 问诊<br />健康知识库<br />一站式健康智慧管理
            </h2>
          </div>

          <div className="grid grid-cols-3 gap-2.5">
            <GlassCard label="报告库"    value="隐私隔离" />
            <GlassCard label="AI 问诊"   value="智慧服务" />
            <GlassCard label="知识库"    value="独立管理" />
          </div>
        </motion.div>

        {/* Bottom — tagline */}
        <div className="relative z-10 p-12">
          <div className="flex items-center gap-3 text-white/40">
            <Activity className="w-5 h-5 text-primary-500/50" />
            <p className="text-sm font-medium tracking-wide">AI 驱动的健康管理新体验，让每一份数据，都成为健康的守护者。</p>
          </div>
        </div>
      </section>

      {/* ── Right panel — form ── */}
      <main className="flex-1 min-h-screen flex items-center justify-center
        bg-slate-50 dark:bg-forest-950 px-4 sm:px-8 py-12 relative overflow-hidden">
        
        {/* Subtle background glow for right side */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[40rem] h-[40rem] bg-primary-400/5 dark:bg-primary-900/20 rounded-full blur-[120px] pointer-events-none" />

        {/* The prominent floating card */}
        <motion.div
          initial={{opacity: 0, scale: 0.96, y: 10}}
          animate={{opacity: 1, scale: 1, y: 0}}
          transition={{duration: 0.5, ease: [0.21, 0.47, 0.32, 0.98]}}
          className="w-full max-w-[480px] bg-white dark:bg-forest-900/80 p-8 sm:p-12 rounded-[2.5rem] shadow-[0_20px_60px_-15px_rgba(0,0,0,0.05)] dark:shadow-[0_20px_60px_-15px_rgba(0,0,0,0.5)] border border-slate-100 dark:border-forest-700 relative z-10 backdrop-blur-2xl"
        >
          {/* Mobile brand (hidden on lg+) */}
          <div className="flex lg:hidden items-start gap-4 mb-10">
            <div className="mt-[3px] flex-shrink-0">
              <LogoMark size={42} animate={false} />
            </div>
            <div className="flex flex-col select-none">
              <span
                className="font-display italic font-normal uppercase text-slate-400 dark:text-forest-300 leading-none tracking-[0.38em]"
                style={{ fontSize: '10px' }}
              >
                Health
              </span>
              <div className="my-[5px] flex items-center">
                <div className="h-px w-10 bg-gradient-to-r from-primary-400/70 to-primary-400/20" />
                <div className="ml-[6px] w-[5px] h-[5px] rotate-45 bg-primary-400 flex-shrink-0 shadow-[0_0_6px_rgba(52,211,153,0.8)]" />
              </div>
              <span
                className="font-display font-bold leading-none"
                style={{
                  fontSize: '2.1rem',
                  letterSpacing: '-0.025em',
                  background: 'linear-gradient(128deg, #34d399 0%, #059669 100%)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  filter: 'drop-shadow(0 1px 8px rgba(16,185,129,0.35))',
                }}
              >
                GUARD
              </span>
            </div>
          </div>

          {/* Heading with Heartbeat */}
          <div className="mb-10 flex items-center gap-4">
            <motion.div
              animate={{ scale: [1, 1.15, 1, 1.15, 1] }}
              transition={{ duration: 1.5, repeat: Infinity, repeatDelay: 2, ease: "easeInOut" }}
              className="w-14 h-14 rounded-2xl bg-primary-50 dark:bg-primary-900/30 flex items-center justify-center text-primary-500 shadow-inner"
            >
              <Activity className="w-7 h-7" />
            </motion.div>
            <div>
              <h1 className="text-[2rem] font-extrabold text-primary-600 dark:text-primary-400 tracking-tight leading-none">
                {mode === 'login' ? '欢迎回来' : '加入我们'}
              </h1>
              <p className="text-slate-500 dark:text-forest-300 mt-2 text-sm font-medium">
                {mode === 'login' ? '登录以继续您的健康管理之旅' : '注册开启您的智能健康管理'}
              </p>
            </div>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5">

            <label className="block">
              <span className="text-sm font-bold text-slate-700 dark:text-slate-300 ml-1">用户名</span>
              <div className="mt-2 flex items-center gap-3 rounded-2xl
                border border-slate-200 dark:border-forest-600
                bg-slate-50/50 dark:bg-forest-800/50 px-4
                focus-within:border-primary-400 dark:focus-within:border-primary-500
                focus-within:bg-white dark:focus-within:bg-forest-800
                focus-within:ring-4 focus-within:ring-primary-100 dark:focus-within:ring-primary-900/20
                transition-all duration-300">
                <UserRound className="w-5 h-5 text-slate-400 dark:text-forest-400 flex-shrink-0" />
                <input
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="h-14 flex-1 bg-transparent text-slate-900 dark:text-white outline-none
                             placeholder:text-slate-400/80 dark:placeholder:text-forest-400 text-base font-medium"
                  placeholder="输入您的用户名"
                  autoComplete="username"
                  required
                />
              </div>
            </label>

            <AnimatePresence>
              {mode === 'register' && (
                <motion.label
                  className="block overflow-hidden"
                  initial={{opacity: 0, height: 0}}
                  animate={{opacity: 1, height: 'auto'}}
                  exit={{opacity: 0, height: 0}}
                  transition={{duration: 0.3, ease: [0.21, 0.47, 0.32, 0.98]}}
                >
                  <div className="pt-1">
                    <span className="text-sm font-bold text-slate-700 dark:text-slate-300 ml-1">昵称</span>
                    <input
                      value={displayName}
                      onChange={(e) => setDisplayName(e.target.value)}
                      className="mt-2 h-14 w-full rounded-2xl px-4 text-base font-medium outline-none
                        border border-slate-200 dark:border-forest-600
                        bg-slate-50/50 dark:bg-forest-800/50
                        text-slate-900 dark:text-white
                        placeholder:text-slate-400/80 dark:placeholder:text-forest-400
                        focus:bg-white dark:focus:bg-forest-800
                        focus:border-primary-400 dark:focus:border-primary-500
                        focus:ring-4 focus:ring-primary-100 dark:focus:ring-primary-900/20
                        transition-all duration-300"
                      placeholder="显示名称（可选）"
                      autoComplete="name"
                    />
                  </div>
                </motion.label>
              )}
            </AnimatePresence>

            <label className="block">
              <span className="text-sm font-bold text-slate-700 dark:text-slate-300 ml-1">密码</span>
              <div className="mt-2 flex items-center gap-3 rounded-2xl
                border border-slate-200 dark:border-forest-600
                bg-slate-50/50 dark:bg-forest-800/50 px-4
                focus-within:border-primary-400 dark:focus-within:border-primary-500
                focus-within:bg-white dark:focus-within:bg-forest-800
                focus-within:ring-4 focus-within:ring-primary-100 dark:focus-within:ring-primary-900/20
                transition-all duration-300">
                <KeyRound className="w-5 h-5 text-slate-400 dark:text-forest-400 flex-shrink-0" />
                <input
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="h-14 flex-1 bg-transparent text-slate-900 dark:text-white outline-none
                             placeholder:text-slate-400/80 dark:placeholder:text-forest-400 text-base font-medium"
                  type="password"
                  placeholder="输入密码 (至少4位)"
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  required
                  minLength={4}
                />
              </div>
            </label>

            <AnimatePresence>
              {error && (
                <motion.div
                  initial={{opacity: 0, y: -10, scale: 0.98}}
                  animate={{opacity: 1, y: 0, scale: 1}}
                  exit={{opacity: 0, y: -10, scale: 0.98}}
                  transition={{duration: 0.2}}
                  className="flex items-center gap-3 rounded-2xl border border-red-200 dark:border-red-800/50
                    bg-red-50 dark:bg-red-900/20 px-4 py-3 text-sm text-red-600 dark:text-red-400 font-medium"
                >
                  <AlertCircle className="w-5 h-5 flex-shrink-0" />
                  <span>{error}</span>
                </motion.div>
              )}
            </AnimatePresence>

            <motion.button
              type="submit"
              disabled={submitting}
              className="w-full h-14 mt-6 rounded-2xl font-bold text-base text-white transition-all
                bg-gradient-to-r from-primary-500 to-primary-600
                hover:from-primary-600 hover:to-primary-700
                shadow-lg shadow-primary-500/25 hover:shadow-xl hover:shadow-primary-500/40
                disabled:opacity-60 disabled:cursor-not-allowed disabled:shadow-none"
              whileHover={{scale: submitting ? 1 : 1.015}}
              whileTap={{scale: submitting ? 1 : 0.985}}
            >
              {submitting ? '处理中...' : mode === 'login' ? '立即登录' : '注册并进入'}
            </motion.button>
          </form>

          {/* Mode switch */}
          <div className="mt-8 text-center">
            <button
              type="button"
              onClick={() => {setMode(mode === 'login' ? 'register' : 'login'); setError('');}}
              className="text-sm font-bold text-slate-500 dark:text-slate-400
                hover:text-primary-600 dark:hover:text-primary-400 transition-colors"
            >
              {mode === 'login' ? '没有账号？点击去注册 →' : '已有账号？点击去登录 →'}
            </button>
          </div>
        </motion.div>
      </main>
    </div>
  );
}