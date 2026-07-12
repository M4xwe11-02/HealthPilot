import {Link, Outlet, useLocation} from 'react-router-dom';
import {motion, AnimatePresence} from 'framer-motion';
import {
  BookOpen,
  Database,
  FileStack,
  LogOut,
  Menu,
  MessageSquare,
  Moon,
  ShieldAlert,
  ShieldCheck,
  Sun,
  Upload,
  UserCircle,
  Users,
} from 'lucide-react';
import {useTheme} from '../hooks/useTheme';
import {useAuth} from '../auth/AuthContext';
import {useState} from 'react';
import LogoMark from './LogoMark';
import bgImage from '../background.png';

interface NavItem {
  id: string;
  path: string;
  label: string;
  icon: React.ComponentType<{className?: string}>;
}

interface NavGroup {
  id: string;
  label: string;
  items: NavItem[];
}

const BASE_NAV_GROUPS: NavGroup[] = [
  {
    id: 'health',
    label: '健康管理',
    items: [
      {id: 'upload',     path: '/upload',     label: '上传报告',   icon: Upload},
      {id: 'healthreports', path: '/history',    label: '报告库',     icon: FileStack},
      {id: 'consultations', path: '/consultations', label: '问诊记录',   icon: Users},
    ],
  },
  {
    id: 'knowledge',
    label: '健康知识库',
    items: [
      {id: 'kb-manage', path: '/knowledgebase',      label: '知识库管理', icon: Database},
      {id: 'chat',      path: '/knowledgebase/chat', label: '健康助手',   icon: MessageSquare},
    ],
  },
  {
    id: 'public-library',
    label: '公共资料库',
    items: [
      {id: 'public-docs', path: '/public-docs', label: '公共文档', icon: BookOpen},
    ],
  },
];

const MOBILE_PRIMARY_IDS = new Set(['upload', 'healthreports', 'consultations', 'kb-manage', 'chat']);

export default function Layout() {
  const location  = useLocation();
  const path      = location.pathname;
  const {theme, toggleTheme} = useTheme();
  const {user, logout}       = useAuth();
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [mobileMoreOpen, setMobileMoreOpen] = useState(false);

  const NAV_GROUPS: NavGroup[] = user?.isAdmin
    ? BASE_NAV_GROUPS.map(g =>
        g.id === 'public-library'
          ? { ...g, items: [...g.items, { id: 'admin-docs', path: '/admin/public-docs', label: '文档管理', icon: ShieldCheck }] }
          : g
      )
    : BASE_NAV_GROUPS;

  const isActive = (p: string) => {
    if (p === '/upload')        return path === '/upload' || path === '/';
    if (p === '/knowledgebase') return path === '/knowledgebase' || path === '/knowledgebase/upload';
    return path.startsWith(p);
  };

  const allNavItems = NAV_GROUPS.flatMap(group => group.items);
  const mobilePrimaryItems = allNavItems.filter(item => MOBILE_PRIMARY_IDS.has(item.id));
  const mobileMoreItems = allNavItems.filter(item => !MOBILE_PRIMARY_IDS.has(item.id));
  const currentPageLabel = allNavItems.find(item => isActive(item.path))?.label || '健康守护';

  return (
    <div
      className="app-shell h-dvh overflow-hidden flex flex-col relative"
      style={{
        backgroundImage: `url(${bgImage})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
      }}
    >
      {/* Dimming overlay — lightens in light mode, darkens in dark mode */}
      <div className="absolute inset-0 pointer-events-none z-0
        bg-white/66 dark:bg-forest-950/88" />

      {/* ═══════════════════════════════════════════════
          Top Navigation Bar  (flow, full-width, ~100px)
      ═══════════════════════════════════════════════ */}
      <header className="relative z-50 flex-shrink-0 flex flex-col
        bg-white/95 dark:bg-forest-900/98
        backdrop-blur-md
        border-b border-slate-100 dark:border-forest-700
        shadow-sm shadow-slate-100/60 dark:shadow-none">

        {/* Jade accent stripe */}
        <div className="h-[3px] bg-gradient-to-r from-primary-300 via-primary-500 to-primary-400 flex-shrink-0" />

        {/* Main bar */}
        <div className="hidden xl:flex h-[97px] items-center px-8 gap-6">

          {/* ── Logo — Health / GUARD wordmark (matching login page) ── */}
          <Link to="/upload" className="flex items-start gap-5 flex-shrink-0 group select-none">
            {/* Shield mark: 36px → 60px */}
            <div className="mt-[5px] flex-shrink-0 drop-shadow-[0_0_18px_rgba(52,211,153,0.28)]">
              <LogoMark size={60} animate={false} />
            </div>
            {/* Wordmark stack */}
            <div className="flex flex-col select-none mt-1">
              {/* "HEALTH" ghost italic */}
              <span
                className="font-display italic font-normal uppercase leading-none
                  tracking-[0.44em] text-slate-400 dark:text-white/45"
                style={{ fontSize: '11px' }}
              >
                Health
              </span>
              {/* Decorative rule: ────◈──── */}
              <div className="my-[7px] flex items-center gap-0">
                <div className="h-px flex-1 bg-gradient-to-r from-primary-400/60 to-primary-400/20" />
                <div className="mx-[7px] w-[6px] h-[6px] rotate-45 flex-shrink-0
                  bg-primary-400 shadow-[0_0_8px_rgba(52,211,153,0.9)]" />
                <div className="h-px w-4 bg-primary-400/20" />
              </div>
              {/* "GUARD" jade gradient logotype */}
              <span
                className="font-display font-bold leading-none"
                style={{
                  fontSize: '2.25rem',
                  letterSpacing: '-0.03em',
                  background: 'linear-gradient(128deg, #a7f3d0 0%, #34d399 30%, #059669 75%, #064e3b 100%)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  filter: 'drop-shadow(0 2px 14px rgba(16,185,129,0.45))',
                }}
              >
                GUARD
              </span>
              {/* Subtitle */}
              <span
                className="font-sans font-normal uppercase mt-[7px] tracking-[0.32em] leading-none
                  text-primary-600/50 dark:text-primary-400/50"
                style={{ fontSize: '7.5px' }}
              >
                Intelligent Health Platform
              </span>
            </div>
          </Link>

          {/* Divider */}
          <div className="w-px h-10 bg-slate-200 dark:bg-forest-600 flex-shrink-0 mx-1" />

          {/* ── Navigation pills ── */}
          <nav className="flex items-center gap-1">
            {NAV_GROUPS.map((group, gi) => (
              <div key={group.id} className="flex items-center gap-1">
                {/* Group separator (not before first group) */}
                {gi > 0 && (
                  <div className="w-px h-6 bg-slate-200 dark:bg-forest-600 mx-2 flex-shrink-0" />
                )}
                {group.items.map(item => {
                  const active = isActive(item.path);
                  return (
                    <Link
                      key={item.id}
                      to={item.path}
                      className={`
                        relative flex items-center gap-2 px-4 py-3 rounded-xl text-[14px] font-medium
                        select-none transition-all duration-150 whitespace-nowrap
                        ${active
                          ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-400'
                          : 'text-slate-500 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-white/[0.05] hover:text-slate-800 dark:hover:text-slate-200'
                        }
                      `}
                    >
                      <item.icon className={`w-5 h-5 flex-shrink-0 ${active ? 'text-primary-500 dark:text-primary-400' : ''}`} />
                      {item.label}
                      {/* Active underline */}
                      {active && (
                        <span className="absolute bottom-[5px] left-1/2 -translate-x-1/2
                          w-[22px] h-[2.5px] rounded-full bg-primary-400 dark:bg-primary-500" />
                      )}
                    </Link>
                  );
                })}
              </div>
            ))}
          </nav>

          {/* Spacer */}
          <div className="flex-1 min-w-0" />

          {/* ── Right controls ── */}
          <div className="flex items-center gap-2 flex-shrink-0">

            {/* Theme toggle */}
            <button
              onClick={toggleTheme}
              title={theme === 'dark' ? '切换浅色' : '切换深色'}
              className="w-9 h-9 rounded-lg flex items-center justify-center
                text-slate-400 dark:text-slate-500
                hover:bg-slate-100 dark:hover:bg-forest-800
                hover:text-slate-600 dark:hover:text-slate-300
                transition-colors"
            >
              {theme === 'dark'
                ? <Sun className="w-[18px] h-[18px]" />
                : <Moon className="w-[18px] h-[18px]" />}
            </button>

            {/* Divider */}
            <div className="w-px h-6 bg-slate-200 dark:bg-forest-600 mx-0.5" />

            {/* User pill + dropdown */}
            <div className="relative">
              <button
                onClick={() => setUserMenuOpen(v => !v)}
                className="flex items-center gap-2 px-3 py-2 rounded-lg
                  hover:bg-slate-50 dark:hover:bg-forest-800
                  transition-colors select-none"
              >
                <div className="w-7 h-7 rounded-full flex-shrink-0 flex items-center justify-center
                  bg-primary-100 dark:bg-primary-900/30
                  ring-[1.5px] ring-primary-200 dark:ring-primary-700/60">
                  <UserCircle className="w-[18px] h-[18px] text-primary-600 dark:text-primary-400" />
                </div>
                <span className="text-[14px] font-medium text-slate-700 dark:text-slate-300
                  max-w-[100px] truncate leading-none">
                  {user?.displayName || user?.username}
                </span>
              </button>

              {/* Dropdown */}
              <AnimatePresence>
                {userMenuOpen && (
                  <>
                    {/* Backdrop */}
                    <div
                      className="fixed inset-0 z-40"
                      onClick={() => setUserMenuOpen(false)}
                    />
                    <motion.div
                      initial={{opacity: 0, y: -6, scale: 0.97}}
                      animate={{opacity: 1, y: 0, scale: 1}}
                      exit={{opacity: 0, y: -4, scale: 0.97}}
                      transition={{duration: 0.12}}
                      className="absolute right-0 top-[calc(100%+6px)] z-50
                        w-[180px] rounded-xl
                        bg-white dark:bg-forest-800
                        border border-slate-100 dark:border-forest-600
                        shadow-lg shadow-slate-200/60 dark:shadow-black/30
                        py-1.5 overflow-hidden"
                    >
                      <div className="px-3.5 py-2 border-b border-slate-100 dark:border-forest-700 mb-1">
                        <p className="text-[13px] font-semibold text-slate-800 dark:text-slate-100 truncate">
                          {user?.displayName || user?.username}
                        </p>
                        <p className="text-[11px] text-slate-400 dark:text-forest-300 truncate mt-px">
                          @{user?.username}
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => { void logout(); setUserMenuOpen(false); }}
                        className="w-full flex items-center gap-2 px-3.5 py-2 text-[13px]
                          text-slate-600 dark:text-slate-400
                          hover:bg-red-50 dark:hover:bg-red-900/15
                          hover:text-red-600 dark:hover:text-red-400
                          transition-colors"
                      >
                        <LogOut className="w-3.5 h-3.5" />
                        退出登录
                      </button>
                    </motion.div>
                  </>
                )}
              </AnimatePresence>
            </div>
          </div>
        </div>

        <div className="xl:hidden h-[57px] flex items-center gap-3 px-4">
          <Link to="/upload" className="flex items-center gap-2.5 min-w-0" aria-label="返回上传报告">
            <div className="drop-shadow-[0_0_10px_rgba(52,211,153,0.28)]">
              <LogoMark size={34} animate={false} />
            </div>
            <div className="min-w-0">
              <p className="font-display text-[10px] italic uppercase tracking-[0.28em] text-primary-500 leading-none">Health Guard</p>
              <p className="mt-1 text-sm font-semibold text-slate-800 dark:text-white truncate">{currentPageLabel}</p>
            </div>
          </Link>
          <div className="flex-1" />
          <button
            type="button"
            onClick={toggleTheme}
            title={theme === 'dark' ? '切换浅色' : '切换深色'}
            className="w-10 h-10 rounded-xl flex items-center justify-center text-slate-500 dark:text-forest-200 active:bg-slate-100 dark:active:bg-forest-800"
          >
            {theme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>
          <button
            type="button"
            onClick={() => setMobileMoreOpen(true)}
            className="w-10 h-10 rounded-xl flex items-center justify-center bg-primary-50 dark:bg-primary-900/25 text-primary-600 dark:text-primary-400"
            aria-label="打开更多菜单"
          >
            <Menu className="w-5 h-5" />
          </button>
        </div>
      </header>

      {/* ═══════════════════════════════════════════════
          Main Content  (flex-1, pinned disclaimer)
      ═══════════════════════════════════════════════ */}
      <main className="relative z-10 flex-1 overflow-hidden flex flex-col">
        <motion.div
          key={path}
          initial={{opacity: 0, y: 12}}
          animate={{opacity: 1, y: 0}}
          exit={{opacity: 0}}
          transition={{duration: 0.2, ease: 'easeOut'}}
          className="flex-1 flex flex-col min-h-0 px-3 py-3 sm:px-5 xl:px-12 max-w-[1400px] w-full mx-auto"
        >
          {/* Page content — scrollable if it overflows */}
          <div className="flex-1 min-h-0 overflow-auto">
            <Outlet />
          </div>

          {/* ── AI 免责声明 footer (always visible) ── */}
          <div className="hidden sm:flex flex-shrink-0 pt-2 mt-1 border-t border-slate-100 dark:border-forest-700
            flex items-start gap-2.5">
            <ShieldAlert className="w-3.5 h-3.5 text-slate-300 dark:text-forest-400 flex-shrink-0 mt-px" />
            <p className="text-[11px] leading-relaxed text-slate-400 dark:text-forest-300">
              <span className="font-medium text-slate-500 dark:text-forest-200">免责声明：</span>
              本平台 AI 健康分析及问诊结果仅供参考，不构成医疗诊断、处方或治疗建议，亦不能替代专业医疗机构的诊断意见。
              如您存在健康问题或症状，请及时前往正规医疗机构就诊，遵从执业医师的专业建议。
            </p>
          </div>
        </motion.div>
      </main>

      <nav
        className="xl:hidden relative z-40 flex-shrink-0 grid grid-cols-5 border-t border-slate-200/80 dark:border-forest-700 bg-white/95 dark:bg-forest-900/98 backdrop-blur-xl px-1 pt-1.5"
        style={{paddingBottom: 'max(0.35rem, env(safe-area-inset-bottom))'}}
        aria-label="主导航"
      >
        {mobilePrimaryItems.map(item => {
          const active = isActive(item.path);
          return (
            <Link
              key={item.id}
              to={item.path}
              onClick={() => setMobileMoreOpen(false)}
              className={`relative min-w-0 h-12 flex flex-col items-center justify-center gap-1 rounded-xl transition-colors ${
                active ? 'text-primary-600 dark:text-primary-400' : 'text-slate-400 dark:text-forest-300 active:bg-slate-100 dark:active:bg-forest-800'
              }`}
            >
              {active && <span className="absolute top-0 w-5 h-0.5 rounded-full bg-primary-500 shadow-[0_0_7px_rgba(16,185,129,0.65)]" />}
              <item.icon className="w-[19px] h-[19px]" />
              <span className="text-[10px] font-medium truncate max-w-full px-1">
                {item.label === '知识库管理' ? '知识库' : item.label}
              </span>
            </Link>
          );
        })}
      </nav>

      <AnimatePresence>
        {mobileMoreOpen && (
          <>
            <motion.button
              type="button"
              aria-label="关闭更多菜单"
              className="xl:hidden fixed inset-0 z-[60] bg-slate-950/35 backdrop-blur-[2px]"
              initial={{opacity: 0}}
              animate={{opacity: 1}}
              exit={{opacity: 0}}
              onClick={() => setMobileMoreOpen(false)}
            />
            <motion.aside
              initial={{y: '100%'}}
              animate={{y: 0}}
              exit={{y: '100%'}}
              transition={{type: 'spring', stiffness: 420, damping: 38}}
              className="xl:hidden fixed inset-x-0 bottom-0 z-[70] rounded-t-[28px] bg-white dark:bg-forest-800 border-t border-slate-200 dark:border-forest-600 shadow-2xl px-5 pt-3"
              style={{paddingBottom: 'max(1.25rem, env(safe-area-inset-bottom))'}}
            >
              <div className="w-10 h-1 rounded-full bg-slate-200 dark:bg-forest-600 mx-auto mb-5" />
              <div className="flex items-center gap-3 pb-4 border-b border-slate-100 dark:border-forest-700">
                <div className="w-10 h-10 rounded-full flex items-center justify-center bg-primary-100 dark:bg-primary-900/30 ring-1 ring-primary-200 dark:ring-primary-700/60">
                  <UserCircle className="w-6 h-6 text-primary-600 dark:text-primary-400" />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-slate-800 dark:text-white truncate">{user?.displayName || user?.username}</p>
                  <p className="text-xs text-slate-400 truncate">@{user?.username}</p>
                </div>
              </div>
              <div className="py-3 space-y-1">
                {mobileMoreItems.map(item => (
                  <Link
                    key={item.id}
                    to={item.path}
                    onClick={() => setMobileMoreOpen(false)}
                    className={`flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium ${
                      isActive(item.path) ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-400' : 'text-slate-600 dark:text-forest-100 active:bg-slate-100 dark:active:bg-forest-700'
                    }`}
                  >
                    <item.icon className="w-5 h-5" />
                    {item.label}
                  </Link>
                ))}
                <button
                  type="button"
                  onClick={() => { void logout(); setMobileMoreOpen(false); }}
                  className="w-full flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium text-red-600 dark:text-red-400 active:bg-red-50 dark:active:bg-red-900/15"
                >
                  <LogOut className="w-5 h-5" />
                  退出登录
                </button>
              </div>
              <p className="text-[10px] leading-relaxed text-slate-400 dark:text-forest-300 px-3">AI 健康分析仅供参考，不替代专业医疗诊断。</p>
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
