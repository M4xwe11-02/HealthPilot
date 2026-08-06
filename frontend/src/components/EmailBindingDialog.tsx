import {FormEvent, useEffect, useState} from 'react';
import {AlertCircle, CheckCircle2, Loader2, Mail, Send, ShieldCheck, X} from 'lucide-react';
import {AnimatePresence, motion} from 'framer-motion';
import {authApi} from '../api/auth';
import {useAuth} from '../auth/AuthContext';

interface EmailBindingDialogProps {
  open: boolean;
  onClose: () => void;
}

export default function EmailBindingDialog({open, onClose}: EmailBindingDialogProps) {
  const {user, bindEmail} = useAuth();
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [sendingCode, setSendingCode] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [resendSeconds, setResendSeconds] = useState(0);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (!open) return;
    setEmail(user?.email || '');
    setCode('');
    setError('');
    setSuccess('');
    setResendSeconds(0);
  }, [open, user?.email]);

  useEffect(() => {
    if (resendSeconds <= 0) return;
    const timer = window.setInterval(() => {
      setResendSeconds((current) => Math.max(0, current - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [resendSeconds]);

  const sendCode = async () => {
    if (!email.trim()) {
      setError('请先输入邮箱');
      return;
    }
    setError('');
    setSuccess('');
    setSendingCode(true);
    try {
      await authApi.sendEmailCode(email);
      setResendSeconds(60);
      setSuccess('验证码已发送');
    } catch (err) {
      setError(err instanceof Error ? err.message : '验证码发送失败');
    } finally {
      setSendingCode(false);
    }
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    setSubmitting(true);
    try {
      await bindEmail({email, code});
      setSuccess('邮箱绑定成功');
      setCode('');
    } catch (err) {
      setError(err instanceof Error ? err.message : '邮箱绑定失败');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center px-4">
          <motion.button
            type="button"
            aria-label="关闭邮箱绑定"
            className="absolute inset-0 bg-slate-950/45 backdrop-blur-[2px]"
            initial={{opacity: 0}}
            animate={{opacity: 1}}
            exit={{opacity: 0}}
            onClick={onClose}
          />
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-labelledby="email-binding-title"
            initial={{opacity: 0, y: 12, scale: 0.98}}
            animate={{opacity: 1, y: 0, scale: 1}}
            exit={{opacity: 0, y: 8, scale: 0.98}}
            className="relative z-10 w-full max-w-[440px] rounded-xl border border-slate-200 bg-white p-5 shadow-2xl dark:border-forest-600 dark:bg-forest-800 sm:p-6"
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 id="email-binding-title" className="text-lg font-bold text-slate-900 dark:text-white">
                  {user?.email ? '更换绑定邮箱' : '绑定邮箱'}
                </h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-forest-300">@{user?.username}</p>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="flex h-9 w-9 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-forest-700 dark:hover:text-white"
                aria-label="关闭"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={submit} className="mt-6 space-y-4">
              <label className="block">
                <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">邮箱</span>
                <div className="mt-2 flex h-12 items-center gap-3 rounded-lg border border-slate-200 px-3 focus-within:border-primary-400 focus-within:ring-2 focus-within:ring-primary-100 dark:border-forest-600 dark:focus-within:border-primary-500 dark:focus-within:ring-primary-900/30">
                  <Mail className="h-4.5 w-4.5 flex-shrink-0 text-slate-400" />
                  <input
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    type="email"
                    autoComplete="email"
                    placeholder="name@example.com"
                    className="min-w-0 flex-1 bg-transparent text-sm text-slate-900 outline-none dark:text-white"
                    required
                  />
                </div>
              </label>

              <label className="block">
                <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">验证码</span>
                <div className="mt-2 flex gap-2">
                  <div className="flex h-12 min-w-0 flex-1 items-center gap-3 rounded-lg border border-slate-200 px-3 focus-within:border-primary-400 focus-within:ring-2 focus-within:ring-primary-100 dark:border-forest-600 dark:focus-within:border-primary-500 dark:focus-within:ring-primary-900/30">
                    <ShieldCheck className="h-4.5 w-4.5 flex-shrink-0 text-slate-400" />
                    <input
                      value={code}
                      onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                      inputMode="numeric"
                      autoComplete="one-time-code"
                      pattern="\d{6}"
                      placeholder="6位验证码"
                      className="min-w-0 flex-1 bg-transparent text-sm font-semibold text-slate-900 outline-none dark:text-white"
                      required
                    />
                  </div>
                  <button
                    type="button"
                    onClick={sendCode}
                    disabled={sendingCode || resendSeconds > 0}
                    className="flex h-12 w-[104px] flex-shrink-0 items-center justify-center gap-1.5 rounded-lg border border-primary-200 bg-primary-50 px-2 text-sm font-semibold text-primary-700 transition-colors hover:bg-primary-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-primary-800 dark:bg-primary-900/30 dark:text-primary-300"
                  >
                    {sendingCode ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                    {sendingCode ? '发送中' : resendSeconds > 0 ? `${resendSeconds}s` : '发送'}
                  </button>
                </div>
              </label>

              {error && (
                <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-600 dark:border-red-800/50 dark:bg-red-900/20 dark:text-red-400">
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  <span>{error}</span>
                </div>
              )}
              {success && (
                <div className="flex items-center gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-sm text-emerald-700 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-300">
                  <CheckCircle2 className="h-4 w-4 flex-shrink-0" />
                  <span>{success}</span>
                </div>
              )}

              <button
                type="submit"
                disabled={submitting}
                className="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-primary-600 text-sm font-bold text-white transition-colors hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                {submitting ? '绑定中...' : user?.email ? '确认更换' : '确认绑定'}
              </button>
            </form>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
