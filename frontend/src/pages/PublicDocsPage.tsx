import { useEffect, useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BookOpen, Download, Eye, FileText, Loader2,
  Search, Tag, X, CheckCircle2, Clock, AlertCircle,
  RefreshCw, Globe, Users,
} from 'lucide-react';
import PageHeader from '../components/PageHeader';
import { publicDocsApi, type PublicDocItem, type PublicDocDetail } from '../api/publicdocs';
import { getErrorMessage } from '../api/request';

// ── Helpers ─────────────────────────────────────────────────────────────

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'short', day: 'numeric',
  });
}

// ── Status Badge ─────────────────────────────────────────────────────────

function StatusBadge({ status }: { status: PublicDocItem['analyzeStatus'] }) {
  const map = {
    PENDING:    { icon: Clock,        label: '待分析',   cls: 'bg-slate-100 text-slate-500 dark:bg-forest-700 dark:text-forest-200' },
    PROCESSING: { icon: RefreshCw,    label: '分析中',   cls: 'bg-amber-50 text-amber-600 dark:bg-amber-900/20 dark:text-amber-400 animate-pulse' },
    COMPLETED:  { icon: CheckCircle2, label: 'AI摘要',   cls: 'bg-primary-50 text-primary-600 dark:bg-primary-900/20 dark:text-primary-400' },
    FAILED:     { icon: AlertCircle,  label: '分析失败', cls: 'bg-red-50 text-red-500 dark:bg-red-900/15 dark:text-red-400' },
  } as const;
  const { icon: Icon, label, cls } = map[status];
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium ${cls}`}>
      <Icon className="w-3 h-3" />
      {label}
    </span>
  );
}

// ── Preview Modal ─────────────────────────────────────────────────────────

function PreviewModal({ id, onClose }: { id: number; onClose: () => void }) {
  const [detail, setDetail] = useState<PublicDocDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]   = useState<string | null>(null);

  useEffect(() => {
    setLoading(true); setError(null);
    publicDocsApi.getDetail(id)
      .then(setDetail)
      .catch(e => setError(getErrorMessage(e)))
      .finally(() => setLoading(false));
  }, [id]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <motion.div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        onClick={onClose}
      />

      {/* Panel */}
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 16 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 8 }}
        transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
        className="relative z-10 w-full max-w-2xl max-h-[85vh] flex flex-col
          bg-white dark:bg-forest-800
          rounded-2xl shadow-2xl shadow-black/20
          border border-slate-100 dark:border-forest-600 overflow-hidden"
      >
        {/* Header stripe */}
        <div className="h-[3px] bg-gradient-to-r from-primary-300 via-primary-500 to-primary-400 flex-shrink-0" />

        {/* Top bar */}
        <div className="flex items-start justify-between gap-4 px-6 py-4 border-b border-slate-100 dark:border-forest-700 flex-shrink-0">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-primary-50 dark:bg-primary-900/20 flex items-center justify-center flex-shrink-0">
              <FileText className="w-4 h-4 text-primary-500" />
            </div>
            <div className="min-w-0">
              <h2 className="font-display font-bold text-slate-900 dark:text-white text-[17px] leading-snug truncate">
                {detail?.title ?? '加载中…'}
              </h2>
              {detail && (
                <div className="flex items-center gap-2 mt-0.5 flex-wrap">
                  {detail.source && (
                    <span className="inline-flex items-center gap-1 text-[11px] text-slate-400 dark:text-forest-300">
                      <Globe className="w-3 h-3" />{detail.source}
                    </span>
                  )}
                  {detail.category && (
                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md text-[10px] font-medium
                      bg-primary-50 text-primary-600 dark:bg-primary-900/20 dark:text-primary-400">
                      <Tag className="w-2.5 h-2.5" />{detail.category}
                    </span>
                  )}
                </div>
              )}
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5
              text-slate-400 hover:text-slate-600 hover:bg-slate-100
              dark:text-forest-300 dark:hover:text-white dark:hover:bg-forest-700
              transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Scrollable body */}
        <div className="flex-1 overflow-y-auto px-6 py-5 space-y-5">
          {loading && (
            <div className="flex items-center justify-center py-16">
              <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
            </div>
          )}
          {error && (
            <div className="flex items-center gap-2 px-4 py-3 rounded-xl bg-red-50 dark:bg-red-900/15 text-red-600 dark:text-red-400 text-sm">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />{error}
            </div>
          )}
          {detail && !loading && (
            <>
              {/* Applicable population */}
              {detail.analysis?.applicablePopulation && (
                <div className="flex items-center gap-2 text-[13px] text-slate-500 dark:text-forest-200">
                  <Users className="w-3.5 h-3.5 text-primary-400 flex-shrink-0" />
                  <span>适用人群：<span className="text-slate-700 dark:text-white font-medium">{detail.analysis.applicablePopulation}</span></span>
                </div>
              )}

              {/* Text preview */}
              {detail.textPreview && (
                <div>
                  <p className="text-[11px] uppercase tracking-widest text-slate-400 dark:text-forest-300 mb-2 font-display italic">原文节选</p>
                  <blockquote className="border-l-2 border-primary-300 pl-4 py-1
                    bg-slate-50 dark:bg-forest-700/50 rounded-r-lg
                    text-[13px] leading-relaxed text-slate-600 dark:text-forest-100 italic">
                    {detail.textPreview}
                  </blockquote>
                </div>
              )}

              {/* AI Analysis */}
              {detail.analyzeStatus === 'COMPLETED' && detail.analysis ? (
                <div className="rounded-xl border border-primary-100 dark:border-primary-900/40 overflow-hidden">
                  {/* Analysis header */}
                  <div className="px-4 py-3 bg-primary-50 dark:bg-primary-900/20 border-b border-primary-100 dark:border-primary-900/40 flex items-center gap-2">
                    <div className="w-[5px] h-[5px] rotate-45 bg-primary-400 shadow-[0_0_6px_rgba(52,211,153,0.75)] flex-shrink-0" />
                    <span className="text-[11px] font-display italic uppercase tracking-widest text-primary-600 dark:text-primary-400">AI 结构化摘要</span>
                  </div>

                  <div className="px-4 py-4 space-y-4 bg-white dark:bg-forest-800">
                    {/* Summary */}
                    <div>
                      <p className="text-[12px] font-semibold text-slate-500 dark:text-forest-300 uppercase tracking-wider mb-1.5">摘要</p>
                      <p className="text-[14px] text-slate-700 dark:text-slate-200 leading-relaxed">{detail.analysis.summary}</p>
                    </div>

                    {/* Key Points */}
                    {detail.analysis.keyPoints?.length > 0 && (
                      <div>
                        <p className="text-[12px] font-semibold text-slate-500 dark:text-forest-300 uppercase tracking-wider mb-2">关键要点</p>
                        <ul className="space-y-1.5">
                          {detail.analysis.keyPoints.map((pt, i) => (
                            <li key={i} className="flex items-start gap-2 text-[13px] text-slate-700 dark:text-slate-200">
                              <span className="text-primary-400 font-bold leading-5 flex-shrink-0 select-none">◈</span>
                              <span>{pt}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {/* Main Recommendations */}
                    {detail.analysis.mainRecommendations?.length > 0 && (
                      <div>
                        <p className="text-[12px] font-semibold text-slate-500 dark:text-forest-300 uppercase tracking-wider mb-2">主要建议</p>
                        <ol className="space-y-1.5">
                          {detail.analysis.mainRecommendations.map((rec, i) => (
                            <li key={i} className="flex items-start gap-2.5 text-[13px] text-slate-700 dark:text-slate-200">
                              <span className="w-5 h-5 rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 text-[11px] font-bold flex items-center justify-center flex-shrink-0 mt-px">{i + 1}</span>
                              <span>{rec}</span>
                            </li>
                          ))}
                        </ol>
                      </div>
                    )}
                  </div>
                </div>
              ) : detail.analyzeStatus === 'PROCESSING' ? (
                <div className="flex items-center gap-3 px-4 py-3 rounded-xl bg-amber-50 dark:bg-amber-900/10 text-amber-600 dark:text-amber-400 text-[13px]">
                  <RefreshCw className="w-4 h-4 animate-spin flex-shrink-0" />
                  AI 正在分析此文档，请稍后刷新查看摘要…
                </div>
              ) : detail.analyzeStatus === 'PENDING' ? (
                <div className="flex items-center gap-3 px-4 py-3 rounded-xl bg-slate-50 dark:bg-forest-700/50 text-slate-500 dark:text-forest-200 text-[13px]">
                  <Clock className="w-4 h-4 flex-shrink-0" />
                  AI 摘要生成中，稍后可查看结构化内容
                </div>
              ) : null}
            </>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between gap-3 px-6 py-4 border-t border-slate-100 dark:border-forest-700 flex-shrink-0">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg text-[13px] font-medium text-slate-500 dark:text-forest-300
              hover:bg-slate-100 dark:hover:bg-forest-700 transition-colors"
          >
            关闭
          </button>
          {detail && (
            <a
              href={publicDocsApi.downloadUrl(detail.id)}
              download
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-[13px] font-medium
                bg-primary-500 hover:bg-primary-600 text-white transition-colors shadow-sm"
            >
              <Download className="w-3.5 h-3.5" />
              下载文件
            </a>
          )}
        </div>
      </motion.div>
    </div>
  );
}

// ── Doc Card ──────────────────────────────────────────────────────────────

function DocCard({ doc, index, onPreview }: { doc: PublicDocItem; index: number; onPreview: () => void }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05, duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
      className="group relative bg-white dark:bg-forest-800 rounded-2xl p-5
        border border-slate-100 dark:border-forest-600
        shadow-sm hover:shadow-md hover:border-primary-200 dark:hover:border-primary-800/60
        transition-all duration-200 flex flex-col gap-3"
    >
      {/* Corner accent */}
      <div className="absolute top-3 right-3 w-[5px] h-[5px] rotate-45 bg-primary-200 dark:bg-primary-700/50
        group-hover:bg-primary-400 group-hover:shadow-[0_0_6px_rgba(52,211,153,0.75)] transition-all" />

      {/* Top row: icon + title */}
      <div className="flex items-start gap-3">
        <div className="w-10 h-10 rounded-xl bg-primary-50 dark:bg-primary-900/20 flex items-center justify-center flex-shrink-0 mt-0.5">
          <FileText className="w-5 h-5 text-primary-400" />
        </div>
        <div className="min-w-0 flex-1">
          <h3 className="font-display font-bold text-slate-800 dark:text-white text-[15px] leading-snug line-clamp-2">
            {doc.title}
          </h3>
          {doc.description && (
            <p className="text-[12px] text-slate-400 dark:text-forest-300 mt-0.5 line-clamp-1">{doc.description}</p>
          )}
        </div>
      </div>

      {/* Badges row */}
      <div className="flex items-center gap-1.5 flex-wrap">
        <StatusBadge status={doc.analyzeStatus} />
        {doc.category && (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium
            bg-slate-100 text-slate-500 dark:bg-forest-700 dark:text-forest-200">
            <Tag className="w-2.5 h-2.5" />{doc.category}
          </span>
        )}
        {doc.source && (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium
            bg-slate-50 text-slate-400 dark:bg-forest-700/50 dark:text-forest-300">
            <Globe className="w-2.5 h-2.5" />{doc.source}
          </span>
        )}
      </div>

      {/* Meta row */}
      <div className="flex items-center gap-3 text-[11px] text-slate-400 dark:text-forest-400">
        <span>{formatFileSize(doc.fileSize)}</span>
        <span className="w-px h-3 bg-slate-200 dark:bg-forest-600" />
        <span>{formatDate(doc.uploadedAt)}</span>
        <span className="w-px h-3 bg-slate-200 dark:bg-forest-600" />
        <span className="flex items-center gap-1"><Download className="w-3 h-3" />{doc.downloadCount}</span>
      </div>

      {/* Bottom gradient line */}
      <div className="absolute bottom-0 left-6 right-6 h-px
        bg-gradient-to-r from-transparent via-primary-200/60 to-transparent
        group-hover:via-primary-400/40 transition-colors" />

      {/* Actions */}
      <div className="flex items-center gap-2 pt-0.5">
        <button
          onClick={onPreview}
          className="flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-[13px] font-medium
            border border-primary-200 dark:border-primary-800/60 text-primary-600 dark:text-primary-400
            hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors"
        >
          <Eye className="w-3.5 h-3.5" />预览
        </button>
        <a
          href={publicDocsApi.downloadUrl(doc.id)}
          download
          className="flex-1 flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-[13px] font-medium
            bg-primary-500 hover:bg-primary-600 text-white transition-colors shadow-sm"
          onClick={(e) => e.stopPropagation()}
        >
          <Download className="w-3.5 h-3.5" />下载
        </a>
      </div>
    </motion.div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────

export default function PublicDocsPage() {
  const [docs, setDocs]           = useState<PublicDocItem[]>([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState<string | null>(null);
  const [search, setSearch]       = useState('');
  const [category, setCategory]   = useState('全部');
  const [previewId, setPreviewId] = useState<number | null>(null);

  useEffect(() => {
    setLoading(true); setError(null);
    publicDocsApi.list()
      .then(setDocs)
      .catch(e => setError(getErrorMessage(e)))
      .finally(() => setLoading(false));
  }, []);

  const categories = useMemo(() => {
    const cats = docs.map(d => d.category).filter(Boolean) as string[];
    return ['全部', ...Array.from(new Set(cats))];
  }, [docs]);

  const filtered = useMemo(() => {
    return docs.filter(d => {
      const matchCat  = category === '全部' || d.category === category;
      const q = search.trim().toLowerCase();
      const matchSearch = !q || d.title.toLowerCase().includes(q)
        || (d.source ?? '').toLowerCase().includes(q)
        || (d.description ?? '').toLowerCase().includes(q);
      return matchCat && matchSearch;
    });
  }, [docs, category, search]);

  return (
    <div className="flex flex-col gap-4 pb-2">
      <PageHeader
        label="Public Library"
        title="公共文档库"
        subtitle="权威健康指南与官方指导文件，供所有用户免费查阅与下载"
        action={
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl
            bg-primary-50 dark:bg-primary-900/20
            border border-primary-100 dark:border-primary-800/40 select-none">
            <BookOpen className="w-4 h-4 text-primary-500" />
            <span className="text-[13px] font-medium text-primary-600 dark:text-primary-400">
              {docs.length} 份文档
            </span>
          </div>
        }
      />

      {/* Filter bar */}
      <div className="bg-white dark:bg-forest-800 rounded-2xl px-4 py-3 shadow-sm border border-slate-100 dark:border-forest-600 flex items-center gap-4 flex-wrap">
        {/* Category chips */}
        <div className="flex items-center gap-1.5 flex-wrap flex-1">
          {categories.map(cat => (
            <button
              key={cat}
              onClick={() => setCategory(cat)}
              className={`px-3 py-1 rounded-full text-[12px] font-medium transition-all ${
                category === cat
                  ? 'bg-primary-500 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-500 dark:bg-forest-700 dark:text-forest-200 hover:bg-primary-50 dark:hover:bg-primary-900/20 hover:text-primary-600 dark:hover:text-primary-400'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
        {/* Search */}
        <div className="relative w-48 flex-shrink-0">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 pointer-events-none" />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="搜索文档…"
            className="w-full pl-8 pr-3 py-1.5 rounded-lg text-[13px]
              bg-slate-50 dark:bg-forest-700
              border border-slate-200 dark:border-forest-600
              text-slate-700 dark:text-slate-200 placeholder:text-slate-400
              focus:outline-none focus:ring-2 focus:ring-primary-400/50 focus:border-primary-300"
          />
        </div>
      </div>

      {/* Content */}
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
        </div>
      ) : error ? (
        <div className="flex items-center gap-2 px-5 py-4 rounded-2xl bg-red-50 dark:bg-red-900/15 border border-red-100 dark:border-red-900/30 text-red-600 dark:text-red-400 text-sm">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />{error}
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 bg-white dark:bg-forest-800 rounded-2xl border border-slate-100 dark:border-forest-600 shadow-sm">
          <BookOpen className="w-14 h-14 text-slate-200 dark:text-forest-600 mb-3" />
          <p className="text-[15px] font-medium text-slate-400 dark:text-forest-300">
            {docs.length === 0 ? '暂无公共文档' : '没有匹配的文档'}
          </p>
          {(search || category !== '全部') && (
            <button
              onClick={() => { setSearch(''); setCategory('全部'); }}
              className="mt-3 text-[13px] text-primary-500 hover:text-primary-600 dark:hover:text-primary-400"
            >
              清除筛选
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map((doc, i) => (
            <DocCard
              key={doc.id}
              doc={doc}
              index={i}
              onPreview={() => setPreviewId(doc.id)}
            />
          ))}
        </div>
      )}

      {/* Preview Modal */}
      <AnimatePresence>
        {previewId !== null && (
          <PreviewModal key={previewId} id={previewId} onClose={() => setPreviewId(null)} />
        )}
      </AnimatePresence>
    </div>
  );
}
