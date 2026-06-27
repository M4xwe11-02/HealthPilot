import { useCallback, useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  AlertCircle,
  CheckCircle,
  Clock,
  FileText,
  Loader2,
  Shield,
  ShieldOff,
  Trash2,
  Upload,
  X,
} from 'lucide-react';
import { publicDocsApi, PublicDocItem } from '../api/publicdocs';
import DeleteConfirmDialog from '../components/DeleteConfirmDialog';
import PageHeader from '../components/PageHeader';
import { useAuth } from '../auth/AuthContext';

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  });
}

type AnalyzeStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

function StatusBadge({ status }: { status: AnalyzeStatus }) {
  const config: Record<AnalyzeStatus, { icon: React.ReactNode; label: string; cls: string }> = {
    PENDING:    { icon: <Clock className="w-3 h-3" />,              label: '待分析', cls: 'bg-slate-100 text-slate-500 dark:bg-slate-700 dark:text-slate-400' },
    PROCESSING: { icon: <Loader2 className="w-3 h-3 animate-spin" />, label: '分析中', cls: 'bg-amber-50 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400' },
    COMPLETED:  { icon: <CheckCircle className="w-3 h-3" />,        label: '已完成', cls: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400' },
    FAILED:     { icon: <AlertCircle className="w-3 h-3" />,        label: '失败',   cls: 'bg-red-50 text-red-600 dark:bg-red-900/30 dark:text-red-400' },
  };
  const { icon, label, cls } = config[status];
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium ${cls}`}>
      {icon}{label}
    </span>
  );
}

export default function AdminPublicDocsPage() {
  const { user } = useAuth();

  if (!user?.isAdmin) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <ShieldOff className="w-14 h-14 text-slate-300 dark:text-forest-500" />
        <p className="text-lg font-display font-bold text-slate-600 dark:text-slate-300">访问受限</p>
        <p className="text-sm text-slate-400 dark:text-forest-300">仅管理员可访问此页面</p>
      </div>
    );
  }

  return <AdminPublicDocsContent />;
}

function AdminPublicDocsContent() {
  const [docs, setDocs] = useState<PublicDocItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Upload form state
  const [file, setFile] = useState<File | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('');
  const [source, setSource] = useState('');
  const [description, setDescription] = useState('');
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Delete dialog
  const [deleteTarget, setDeleteTarget] = useState<PublicDocItem | null>(null);
  const [deleting, setDeleting] = useState(false);

  const loadDocs = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await publicDocsApi.list();
      setDocs(data);
    } catch {
      setError('加载文档列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void loadDocs(); }, [loadDocs]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const dropped = e.dataTransfer.files[0];
    if (dropped) { setFile(dropped); if (!title) setTitle(dropped.name.replace(/\.[^.]+$/, '')); }
  }, [title]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) { setFile(f); if (!title) setTitle(f.name.replace(/\.[^.]+$/, '')); }
  };

  const handleUpload = async () => {
    if (!file || !title.trim()) return;
    setUploading(true);
    setUploadError(null);
    setUploadSuccess(false);
    try {
      const doc = await publicDocsApi.adminUpload(
        file, title.trim(),
        category.trim() || undefined,
        source.trim() || undefined,
        description.trim() || undefined,
      );
      setDocs(prev => [doc, ...prev]);
      setFile(null); setTitle(''); setCategory(''); setSource(''); setDescription('');
      setUploadSuccess(true);
      setTimeout(() => setUploadSuccess(false), 3000);
    } catch (err: unknown) {
      setUploadError(err instanceof Error ? err.message : '上传失败，请重试');
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await publicDocsApi.adminDelete(deleteTarget.id);
      setDocs(prev => prev.filter(d => d.id !== deleteTarget.id));
      setDeleteTarget(null);
    } catch {
      // keep dialog open on error
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="flex flex-col gap-6 pt-2 pb-6">
      <PageHeader
        label="Admin"
        title="文档管理"
        subtitle="上传、管理公共健康文档库中的官方指导文件"
        action={
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[12px] font-semibold
            bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-400
            ring-1 ring-primary-200 dark:ring-primary-700/50">
            <Shield className="w-3.5 h-3.5" />管理员
          </span>
        }
      />

      {/* ── Upload card ── */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
        className="rounded-2xl border border-slate-200 dark:border-forest-700
          bg-white/80 dark:bg-forest-800/80 backdrop-blur-sm shadow-sm p-6"
      >
        <h2 className="font-display font-semibold text-[15px] text-slate-800 dark:text-white mb-4 flex items-center gap-2">
          <Upload className="w-4 h-4 text-primary-500" />上传新文档
        </h2>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {/* Drop zone */}
          <div
            onClick={() => fileInputRef.current?.click()}
            onDragOver={e => { e.preventDefault(); setIsDragging(true); }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleDrop}
            className={`
              relative flex flex-col items-center justify-center gap-3 cursor-pointer
              rounded-xl border-2 border-dashed transition-all duration-200 min-h-[140px]
              ${isDragging
                ? 'border-primary-400 bg-primary-50/50 dark:bg-primary-900/20'
                : file
                  ? 'border-emerald-300 dark:border-emerald-700 bg-emerald-50/50 dark:bg-emerald-900/20'
                  : 'border-slate-200 dark:border-forest-600 hover:border-primary-300 dark:hover:border-primary-600 bg-slate-50/50 dark:bg-forest-900/40'
              }
            `}
          >
            <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileChange}
              accept=".pdf,.doc,.docx,.txt" />
            {file ? (
              <>
                <FileText className="w-10 h-10 text-emerald-500" />
                <div className="text-center">
                  <p className="text-[13px] font-medium text-slate-700 dark:text-slate-200 truncate max-w-[200px]">{file.name}</p>
                  <p className="text-[11px] text-slate-400 mt-0.5">{formatFileSize(file.size)}</p>
                </div>
                <button
                  type="button"
                  onClick={e => { e.stopPropagation(); setFile(null); }}
                  className="absolute top-2 right-2 w-6 h-6 rounded-full flex items-center justify-center
                    bg-slate-200 dark:bg-forest-600 text-slate-500 dark:text-slate-300
                    hover:bg-red-100 dark:hover:bg-red-900/30 hover:text-red-500 transition-colors"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </>
            ) : (
              <>
                <Upload className="w-8 h-8 text-slate-300 dark:text-forest-400" />
                <div className="text-center">
                  <p className="text-[13px] font-medium text-slate-500 dark:text-forest-300">拖放文件或点击选择</p>
                  <p className="text-[11px] text-slate-400 dark:text-forest-400 mt-0.5">支持 PDF、Word、TXT</p>
                </div>
              </>
            )}
          </div>

          {/* Form fields */}
          <div className="flex flex-col gap-3">
            <div>
              <label className="text-[12px] font-medium text-slate-600 dark:text-forest-300 mb-1 block">
                标题 <span className="text-red-400">*</span>
              </label>
              <input
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="文档标题"
                className="w-full px-3 py-2 text-[13px] rounded-lg
                  bg-slate-50 dark:bg-forest-900/60
                  border border-slate-200 dark:border-forest-600
                  text-slate-800 dark:text-slate-100
                  placeholder-slate-400 dark:placeholder-forest-400
                  focus:outline-none focus:ring-2 focus:ring-primary-400/40 focus:border-primary-400
                  transition-all"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-[12px] font-medium text-slate-600 dark:text-forest-300 mb-1 block">分类</label>
                <input
                  value={category}
                  onChange={e => setCategory(e.target.value)}
                  placeholder="如：营养指南"
                  className="w-full px-3 py-2 text-[13px] rounded-lg
                    bg-slate-50 dark:bg-forest-900/60
                    border border-slate-200 dark:border-forest-600
                    text-slate-800 dark:text-slate-100
                    placeholder-slate-400 dark:placeholder-forest-400
                    focus:outline-none focus:ring-2 focus:ring-primary-400/40 focus:border-primary-400
                    transition-all"
                />
              </div>
              <div>
                <label className="text-[12px] font-medium text-slate-600 dark:text-forest-300 mb-1 block">来源</label>
                <input
                  value={source}
                  onChange={e => setSource(e.target.value)}
                  placeholder="如：国家卫健委"
                  className="w-full px-3 py-2 text-[13px] rounded-lg
                    bg-slate-50 dark:bg-forest-900/60
                    border border-slate-200 dark:border-forest-600
                    text-slate-800 dark:text-slate-100
                    placeholder-slate-400 dark:placeholder-forest-400
                    focus:outline-none focus:ring-2 focus:ring-primary-400/40 focus:border-primary-400
                    transition-all"
                />
              </div>
            </div>
            <div>
              <label className="text-[12px] font-medium text-slate-600 dark:text-forest-300 mb-1 block">描述</label>
              <textarea
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="简要描述文档内容（可选）"
                rows={2}
                className="w-full px-3 py-2 text-[13px] rounded-lg resize-none
                  bg-slate-50 dark:bg-forest-900/60
                  border border-slate-200 dark:border-forest-600
                  text-slate-800 dark:text-slate-100
                  placeholder-slate-400 dark:placeholder-forest-400
                  focus:outline-none focus:ring-2 focus:ring-primary-400/40 focus:border-primary-400
                  transition-all"
              />
            </div>

            <AnimatePresence mode="wait">
              {uploadError && (
                <motion.p key="err" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                  className="text-[12px] text-red-500 flex items-center gap-1.5">
                  <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />{uploadError}
                </motion.p>
              )}
              {uploadSuccess && (
                <motion.p key="ok" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                  className="text-[12px] text-emerald-500 flex items-center gap-1.5">
                  <CheckCircle className="w-3.5 h-3.5 flex-shrink-0" />上传成功，AI 正在后台分析…
                </motion.p>
              )}
            </AnimatePresence>

            <button
              onClick={() => void handleUpload()}
              disabled={!file || !title.trim() || uploading}
              className="mt-auto flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl
                text-[13px] font-semibold transition-all duration-150
                bg-primary-500 hover:bg-primary-600 text-white
                disabled:opacity-40 disabled:cursor-not-allowed
                shadow-sm shadow-primary-500/20"
            >
              {uploading
                ? <><Loader2 className="w-4 h-4 animate-spin" />上传中…</>
                : <><Upload className="w-4 h-4" />上传文档</>
              }
            </button>
          </div>
        </div>
      </motion.div>

      {/* ── Document list ── */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, delay: 0.1, ease: [0.22, 1, 0.36, 1] }}
        className="rounded-2xl border border-slate-200 dark:border-forest-700
          bg-white/80 dark:bg-forest-800/80 backdrop-blur-sm shadow-sm overflow-hidden"
      >
        <div className="px-6 py-4 border-b border-slate-100 dark:border-forest-700
          flex items-center justify-between">
          <h2 className="font-display font-semibold text-[15px] text-slate-800 dark:text-white flex items-center gap-2">
            <FileText className="w-4 h-4 text-primary-500" />
            文档列表
            <span className="ml-1 px-2 py-0.5 rounded-full text-[11px] font-medium
              bg-slate-100 dark:bg-forest-700 text-slate-500 dark:text-slate-400">
              {docs.length}
            </span>
          </h2>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-16 gap-3">
            <Loader2 className="w-5 h-5 animate-spin text-primary-400" />
            <span className="text-[13px] text-slate-400 dark:text-forest-300">加载中…</span>
          </div>
        ) : error ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <AlertCircle className="w-8 h-8 text-red-400" />
            <p className="text-[13px] text-slate-500 dark:text-forest-300">{error}</p>
            <button onClick={() => void loadDocs()}
              className="text-[12px] text-primary-500 hover:underline">重试</button>
          </div>
        ) : docs.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <FileText className="w-10 h-10 text-slate-200 dark:text-forest-600" />
            <p className="text-[13px] text-slate-400 dark:text-forest-300">暂无文档，请上传第一份文件</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-[13px]">
              <thead>
                <tr className="border-b border-slate-100 dark:border-forest-700">
                  {['标题', '分类', '来源', 'AI 状态', '下载量', '上传时间', '操作'].map(h => (
                    <th key={h} className="px-4 py-3 text-left text-[11px] font-semibold uppercase
                      tracking-wider text-slate-400 dark:text-forest-400 whitespace-nowrap">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {docs.map((doc, i) => (
                  <motion.tr
                    key={doc.id}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.25, delay: i * 0.04 }}
                    className="border-b border-slate-50 dark:border-forest-700/50 last:border-0
                      hover:bg-slate-50/60 dark:hover:bg-forest-700/30 transition-colors"
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-primary-400 flex-shrink-0" />
                        <span className="font-medium text-slate-800 dark:text-slate-100 max-w-[200px] truncate">
                          {doc.title}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-400 dark:text-forest-400 mt-0.5 ml-6">
                        {formatFileSize(doc.fileSize)}
                      </p>
                    </td>
                    <td className="px-4 py-3">
                      {doc.category
                        ? <span className="px-2 py-0.5 rounded-full text-[11px] bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400">{doc.category}</span>
                        : <span className="text-slate-300 dark:text-forest-500">—</span>
                      }
                    </td>
                    <td className="px-4 py-3 text-slate-500 dark:text-forest-300 max-w-[120px] truncate">
                      {doc.source || <span className="text-slate-300 dark:text-forest-500">—</span>}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={doc.analyzeStatus} />
                    </td>
                    <td className="px-4 py-3 text-slate-500 dark:text-forest-300">
                      {doc.downloadCount}
                    </td>
                    <td className="px-4 py-3 text-slate-400 dark:text-forest-400 whitespace-nowrap">
                      {formatDate(doc.uploadedAt)}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => setDeleteTarget(doc)}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[12px] font-medium
                          text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20
                          transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />下架
                      </button>
                    </td>
                  </motion.tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </motion.div>

      {/* Delete confirm dialog */}
      <DeleteConfirmDialog
        open={!!deleteTarget}
        item={deleteTarget}
        itemType="文档"
        loading={deleting}
        customMessage={`确定要下架「${deleteTarget?.title ?? ''}」吗？此操作不可撤销。`}
        onConfirm={() => void handleDelete()}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
