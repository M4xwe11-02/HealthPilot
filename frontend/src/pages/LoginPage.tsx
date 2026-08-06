import {FormEvent, useEffect, useState} from 'react';
import {Navigate, useLocation, useNavigate} from 'react-router-dom';
import {AlertCircle, Activity, CheckCircle2, KeyRound, Loader2, Mail, Send, ShieldCheck, UserRound} from 'lucide-react';
import {motion, AnimatePresence} from 'framer-motion';
import {useAuth} from '../auth/AuthContext';
import {authApi} from '../api/auth';
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
  const {isAuthenticated, login, loginWithEmail, register} = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as {from?: {pathname?: string}} | null)?.from?.pathname || '/upload';

  const [mode, setMode] = useState<'email' | 'login' | 'register'>('email');
  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [sendingCode, setSendingCode] = useState(false);
  const [resendSeconds, setResendSeconds] = useState(0);
  const [notice, setNotice] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (resendSeconds <= 0) return;
    const timer = window.setInterval(() => {
      setResendSeconds((current) => Math.max(0, current - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [resendSeconds]);

  if (isAuthenticated) return <Navigate to={from} replace />;

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (mode === 'email') {
        await loginWithEmail({email, code});
      } else if (mode === 'login') {
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

  const handleSendCode = async () => {
    if (!email.trim()) {
      setError('请先输入邮箱');
      return;
    }
    setError('');
    setNotice('');
    setSendingCode(true);
    try {
      await authApi.sendEmailCode(email);
      setResendSeconds(60);
      setNotice('验证码已发送，请检查邮箱');
    } catch (err) {
      setError(err instanceof Error ? err.message : '验证码发送失败');
    } finally {
      setSendingCode(false);
    }
  };

  return (
    <div className="min-h-dvh flex bg-forest-950 font-sans">

      {/* ── Left panel ── */}
      <section className="hidden lg:flex w-[46%] min-h-dvh flex-col justify-between relative overflow-hidden
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
      <main className="flex-1 min-h-dvh flex items-center justify-center
        bg-slate-50 dark:bg-forest-950 px-3 sm:px-8 py-5 sm:py-12 relative overflow-hidden">
        
        {/* Subtle background glow for right side */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[40rem] h-[40rem] bg-primary-400/5 dark:bg-primary-900/20 rounded-full blur-[120px] pointer-events-none" />

        {/* The prominent floating card */}
        <motion.div
          initial={{opacity: 0, scale: 0.96, y: 10}}
          animate={{opacity: 1, scale: 1, y: 0}}
          transition={{duration: 0.5, ease: [0.21, 0.47, 0.32, 0.98]}}
          className="w-full max-w-[480px] bg-white dark:bg-forest-900/80 p-5 sm:p-12 rounded-[1.75rem] sm:rounded-[2.5rem] shadow-[0_20px_60px_-15px_rgba(0,0,0,0.05)] dark:shadow-[0_20px_60px_-15px_rgba(0,0,0,0.5)] border border-slate-100 dark:border-forest-700 relative z-10 backdrop-blur-2xl"
        >
          {/* Mobile brand (hidden on lg+) */}
          <div className="flex lg:hidden items-start gap-4 mb-6 sm:mb-10">
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
          <div className="mb-6 flex items-center gap-3 sm:gap-4">
            <motion.div
              animate={{ scale: [1, 1.15, 1, 1.15, 1] }}
              transition={{ duration: 1.5, repeat: Infinity, repeatDelay: 2, ease: "easeInOut" }}
              className="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-primary-50 dark:bg-primary-900/30 flex items-center justify-center text-primary-500 shadow-inner flex-shrink-0"
            >
              <Activity className="w-7 h-7" />
            </motion.div>
            <div>
              <h1 className="text-[1.65rem] sm:text-[2rem] font-extrabold text-primary-600 dark:text-primary-400 tracking-tight leading-none">
                {mode === 'email' ? '邮箱登录' : mode === 'login' ? '欢迎回来' : '加入我们'}
              </h1>
              <p className="text-slate-500 dark:text-forest-300 mt-2 text-sm font-medium">
                {mode === 'email'
                  ? '验证邮箱以继续您的健康管理之旅'
                  : mode === 'login'
                    ? '登录以继续您的健康管理之旅'
                    : '注册开启您的智能健康管理'}
              </p>
            </div>
          </div>

          <div className="mb-6 grid h-11 grid-cols-2 rounded-xl bg-slate-100 p-1 dark:bg-forest-800">
            <button
              type="button"
              onClick={() => {setMode('email'); setError(''); setNotice('');}}
              className={`flex min-w-0 items-center justify-center gap-2 rounded-lg text-sm font-bold transition-all ${
                mode === 'email'
                  ? 'bg-white text-primary-600 shadow-sm dark:bg-forest-700 dark:text-primary-400'
                  : 'text-slate-500 hover:text-slate-700 dark:text-forest-300 dark:hover:text-white'
              }`}
            >
              <Mail className="h-4 w-4 flex-shrink-0" />
              <span>邮箱验证码</span>
            </button>
            <button
              type="button"
              onClick={() => {setMode('login'); setError(''); setNotice('');}}
              className={`flex min-w-0 items-center justify-center gap-2 rounded-lg text-sm font-bold transition-all ${
                mode !== 'email'
                  ? 'bg-white text-primary-600 shadow-sm dark:bg-forest-700 dark:text-primary-400'
                  : 'text-slate-500 hover:text-slate-700 dark:text-forest-300 dark:hover:text-white'
              }`}
            >
              <KeyRound className="h-4 w-4 flex-shrink-0" />
              <span>密码登录</span>
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4 sm:space-y-5">
            <AnimatePresence mode="wait" initial={false}>
              {mode === 'email' ? (
                <motion.div
                  key="email-form"
                  initial={{opacity: 0, x: -8}}
                  animate={{opacity: 1, x: 0}}
                  exit={{opacity: 0, x: 8}}
                  transition={{duration: 0.2}}
                  className="space-y-4 sm:space-y-5"
                >
                  <label className="block">
                    <span className="ml-1 text-sm font-bold text-slate-700 dark:text-slate-300">邮箱</span>
                    <div className="mt-2 flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50/50 px-4 transition-all duration-300 focus-within:border-primary-400 focus-within:bg-white focus-within:ring-4 focus-within:ring-primary-100 dark:border-forest-600 dark:bg-forest-800/50 dark:focus-within:border-primary-500 dark:focus-within:bg-forest-800 dark:focus-within:ring-primary-900/20">
                      <Mail className="h-5 w-5 flex-shrink-0 text-slate-400 dark:text-forest-400" />
                      <input
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        className="h-14 min-w-0 flex-1 bg-transparent text-base font-medium text-slate-900 outline-none placeholder:text-slate-400/80 dark:text-white dark:placeholder:text-forest-400"
                        type="email"
                        placeholder="name@example.com"
                        autoComplete="email"
                        required
                      />
                    </div>
                  </label>

                  <label className="block">
                    <span className="ml-1 text-sm font-bold text-slate-700 dark:text-slate-300">验证码</span>
                    <div className="mt-2 flex gap-2">
                      <div className="flex min-w-0 flex-1 items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50/50 px-4 transition-all duration-300 focus-within:border-primary-400 focus-within:bg-white focus-within:ring-4 focus-within:ring-primary-100 dark:border-forest-600 dark:bg-forest-800/50 dark:focus-within:border-primary-500 dark:focus-within:bg-forest-800 dark:focus-within:ring-primary-900/20">
                        <ShieldCheck className="h-5 w-5 flex-shrink-0 text-slate-400 dark:text-forest-400" />
                        <input
                          value={code}
                          onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                          className="h-14 min-w-0 flex-1 bg-transparent text-base font-semibold text-slate-900 outline-none placeholder:text-slate-400/80 dark:text-white dark:placeholder:text-forest-400"
                          inputMode="numeric"
                          autoComplete="one-time-code"
                          placeholder="6位验证码"
                          pattern="\d{6}"
                          required
                        />
                      </div>
                      <button
                        type="button"
                        onClick={handleSendCode}
                        disabled={sendingCode || resendSeconds > 0}
                        className="flex h-14 w-[108px] flex-shrink-0 items-center justify-center gap-1.5 rounded-2xl border border-primary-200 bg-primary-50 px-2 text-sm font-bold text-primary-700 transition-colors hover:bg-primary-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-primary-800 dark:bg-primary-900/30 dark:text-primary-300 dark:hover:bg-primary-900/50"
                      >
                        {sendingCode
                          ? <Loader2 className="h-4 w-4 animate-spin" />
                          : <Send className="h-4 w-4" />}
                        <span>{sendingCode ? '发送中' : resendSeconds > 0 ? `${resendSeconds}s` : '发送'}</span>
                      </button>
                    </div>
                  </label>
                </motion.div>
              ) : (
                <motion.div
                  key="password-form"
                  initial={{opacity: 0, x: 8}}
                  animate={{opacity: 1, x: 0}}
                  exit={{opacity: 0, x: -8}}
                  transition={{duration: 0.2}}
                  className="space-y-4 sm:space-y-5"
                >
                  <label className="block">
                    <span className="ml-1 text-sm font-bold text-slate-700 dark:text-slate-300">用户名</span>
                    <div className="mt-2 flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50/50 px-4 transition-all duration-300 focus-within:border-primary-400 focus-within:bg-white focus-within:ring-4 focus-within:ring-primary-100 dark:border-forest-600 dark:bg-forest-800/50 dark:focus-within:border-primary-500 dark:focus-within:bg-forest-800 dark:focus-within:ring-primary-900/20">
                      <UserRound className="h-5 w-5 flex-shrink-0 text-slate-400 dark:text-forest-400" />
                      <input
                        value={username}
                        onChange={(event) => setUsername(event.target.value)}
                        className="h-14 min-w-0 flex-1 bg-transparent text-base font-medium text-slate-900 outline-none placeholder:text-slate-400/80 dark:text-white dark:placeholder:text-forest-400"
                        placeholder="输入您的用户名"
                        autoComplete="username"
                        required
                      />
                    </div>
                  </label>

                  {mode === 'register' && (
                    <label className="block">
                      <span className="ml-1 text-sm font-bold text-slate-700 dark:text-slate-300">昵称</span>
                      <input
                        value={displayName}
                        onChange={(event) => setDisplayName(event.target.value)}
                        className="mt-2 h-14 w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 text-base font-medium text-slate-900 outline-none transition-all duration-300 placeholder:text-slate-400/80 focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-100 dark:border-forest-600 dark:bg-forest-800/50 dark:text-white dark:placeholder:text-forest-400 dark:focus:border-primary-500 dark:focus:bg-forest-800 dark:focus:ring-primary-900/20"
                        placeholder="显示名称（可选）"
                        autoComplete="name"
                      />
                    </label>
                  )}

                  <label className="block">
                    <span className="ml-1 text-sm font-bold text-slate-700 dark:text-slate-300">密码</span>
                    <div className="mt-2 flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50/50 px-4 transition-all duration-300 focus-within:border-primary-400 focus-within:bg-white focus-within:ring-4 focus-within:ring-primary-100 dark:border-forest-600 dark:bg-forest-800/50 dark:focus-within:border-primary-500 dark:focus-within:bg-forest-800 dark:focus-within:ring-primary-900/20">
                      <KeyRound className="h-5 w-5 flex-shrink-0 text-slate-400 dark:text-forest-400" />
                      <input
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        className="h-14 min-w-0 flex-1 bg-transparent text-base font-medium text-slate-900 outline-none placeholder:text-slate-400/80 dark:text-white dark:placeholder:text-forest-400"
                        type="password"
                        placeholder="输入密码 (至少4位)"
                        autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                        required
                        minLength={4}
                      />
                    </div>
                  </label>
                </motion.div>
              )}
            </AnimatePresence>

            <AnimatePresence>
              {notice && (
                <motion.div
                  initial={{opacity: 0, y: -6}}
                  animate={{opacity: 1, y: 0}}
                  exit={{opacity: 0, y: -6}}
                  className="flex items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-300"
                >
                  <CheckCircle2 className="h-5 w-5 flex-shrink-0" />
                  <span>{notice}</span>
                </motion.div>
              )}
            </AnimatePresence>

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
              <span className="flex items-center justify-center gap-2">
                {submitting && <Loader2 className="h-5 w-5 animate-spin" />}
                {submitting ? '处理中...' : mode === 'register' ? '注册并进入' : '立即登录'}
              </span>
            </motion.button>
          </form>

          {/* Mode switch */}
          {mode !== 'email' && (
            <div className="mt-8 text-center">
              <button
                type="button"
                onClick={() => {setMode(mode === 'login' ? 'register' : 'login'); setError(''); setNotice('');}}
                className="text-sm font-bold text-slate-500 transition-colors hover:text-primary-600 dark:text-slate-400 dark:hover:text-primary-400"
              >
                {mode === 'login' ? '没有账号？点击去注册 →' : '已有账号？点击去登录 →'}
              </button>
            </div>
          )}
        </motion.div>
      </main>
    </div>
  );
}
