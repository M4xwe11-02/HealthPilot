import {ChangeEvent, DragEvent, useCallback, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {AlertCircle, FileText, Loader2, Upload, X} from 'lucide-react';

export interface FileUploadCardProps {
  /** 标题 */
  title: string;
  /** 副标题 */
  subtitle: string;
  /** 接受的文件类型 */
  accept: string;
  /** 支持的格式说明 */
  formatHint: string;
  /** 最大文件大小说明 */
  maxSizeHint: string;
  /** 是否正在上传 */
  uploading?: boolean;
  /** 上传按钮文字 */
  uploadButtonText?: string;
  /** 选择按钮文字 */
  selectButtonText?: string;
  /** 是否显示名称输入框 */
  showNameInput?: boolean;
  /** 名称输入框占位符 */
  namePlaceholder?: string;
  /** 名称输入框标签 */
  nameLabel?: string;
  /** 错误信息 */
  error?: string;
  /** 是否显示内部标题区（false 时由父页面的 PageHeader 负责标题，组件本身不居中显示） */
  showHeader?: boolean;
  /** 文件选择回调 */
  onFileSelect?: (file: File) => void;
  /** 上传回调 */
  onUpload: (file: File, name?: string) => void;
  /** 返回回调 */
  onBack?: () => void;
}

export default function FileUploadCard({
  title,
  subtitle,
  accept,
  formatHint,
  maxSizeHint,
  uploading = false,
  uploadButtonText = '开始上传',
  selectButtonText = '选择文件',
  showNameInput = false,
  namePlaceholder = '留空则使用文件名',
  nameLabel = '名称（可选）',
  error,
  showHeader = true,
  onFileSelect,
  onUpload,
  onBack,
}: FileUploadCardProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [name, setName] = useState('');

  const handleDragOver = useCallback((e: DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: DragEvent) => {
    e.preventDefault();
    setDragOver(false);
  }, []);

  const handleDrop = useCallback((e: DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const files = e.dataTransfer.files;
    if (files.length > 0) {
      setSelectedFile(files[0]);
      onFileSelect?.(files[0]);
    }
  }, [onFileSelect]);

  const handleFileChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      setSelectedFile(files[0]);
      onFileSelect?.(files[0]);
    }
  }, [onFileSelect]);

  const handleUpload = () => {
    if (!selectedFile) return;
    onUpload(selectedFile, name.trim() || undefined);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  return (
    <motion.div
      className={showHeader
        ? "h-full flex flex-col items-center justify-center py-4 px-2"
        : "flex flex-col gap-4 py-5 px-6"}
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      <div className={showHeader ? "w-full max-w-2xl flex flex-col gap-4" : "w-full flex flex-col gap-4"}>

        {/* ── Compact header (only when showHeader) ── */}
        {showHeader && <motion.div
          className="text-center"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.05 }}
        >
          {/* Diamond label */}
          <div className="flex items-center justify-center gap-2 mb-2">
            <div className="h-px w-10 bg-gradient-to-r from-transparent to-primary-400/50" />
            <div className="w-[4px] h-[4px] rotate-45 bg-primary-400 shadow-[0_0_5px_rgba(52,211,153,0.75)]" />
            <span
              className="font-display italic font-normal uppercase text-primary-500 dark:text-primary-400 select-none"
              style={{ fontSize: '9px', letterSpacing: '0.38em' }}
            >
              Health Report
            </span>
            <div className="w-[4px] h-[4px] rotate-45 bg-primary-400 shadow-[0_0_5px_rgba(52,211,153,0.75)]" />
            <div className="h-px w-10 bg-gradient-to-r from-primary-400/50 to-transparent" />
          </div>
          <motion.h1
            className="font-display font-bold text-slate-900 dark:text-white leading-tight mb-1.5"
            style={{ fontSize: '1.7rem', letterSpacing: '-0.02em' }}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            {title}
          </motion.h1>
          <motion.p
            className="text-[13px] text-slate-500 dark:text-forest-200 leading-relaxed"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.15 }}
          >
            {subtitle}
          </motion.p>
        </motion.div>}

        {/* ── Drop zone ── */}
        <motion.div
          className={`relative bg-white dark:bg-forest-800 rounded-2xl p-7 cursor-pointer transition-all duration-300
            ${dragOver ? 'scale-[1.015] shadow-xl' : 'shadow-lg hover:shadow-xl dark:shadow-black/30'}`}
          style={{ minHeight: '190px' }}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => document.getElementById('file-upload-input')?.click()}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          {/* Gradient border */}
          <div
            className={`absolute inset-0 rounded-2xl p-[2px] -z-10 transition-all duration-300
              ${dragOver
                ? 'bg-gradient-to-r from-primary-400 via-primary-500 to-primary-400'
                : 'bg-gradient-to-r from-primary-100 via-primary-200 to-primary-100 dark:from-forest-600 dark:via-primary-800/40 dark:to-forest-600'
              }`}
          >
            <div className="w-full h-full bg-white dark:bg-forest-800 rounded-2xl" />
          </div>

          {/* Precision corner marks */}
          <div className="absolute top-3 left-3 pointer-events-none">
            <div className="w-3 h-px bg-primary-400/40" />
            <div className="w-px h-3 bg-primary-400/40" />
          </div>
          <div className="absolute top-3 right-3 pointer-events-none flex flex-col items-end">
            <div className="w-3 h-px bg-primary-400/40" />
            <div className="w-px h-3 bg-primary-400/40 ml-auto" />
          </div>
          <div className="absolute bottom-3 left-3 pointer-events-none flex flex-col justify-end">
            <div className="w-px h-3 bg-primary-400/40" />
            <div className="w-3 h-px bg-primary-400/40" />
          </div>
          <div className="absolute bottom-3 right-3 pointer-events-none flex flex-col items-end justify-end">
            <div className="w-px h-3 bg-primary-400/40 ml-auto" />
            <div className="w-3 h-px bg-primary-400/40" />
          </div>

          <input
            type="file"
            id="file-upload-input"
            className="hidden"
            accept={accept}
            onChange={handleFileChange}
            disabled={uploading}
          />

          <AnimatePresence mode="wait">
            {selectedFile ? (
              /* ── File selected state ── */
              <motion.div
                key="file-selected"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.95 }}
                className="flex items-center justify-center h-full"
                style={{ minHeight: '130px' }}
              >
                <div className="flex items-center gap-5 bg-slate-50 dark:bg-forest-700/50 px-6 py-4 rounded-xl border border-slate-100 dark:border-forest-600 w-full max-w-sm mx-auto">
                  <div className="w-12 h-12 flex-shrink-0 bg-primary-100 dark:bg-primary-900/50 rounded-xl flex items-center justify-center">
                    <FileText className="w-6 h-6 text-primary-600 dark:text-primary-400" />
                  </div>
                  <div className="text-left flex-1 min-w-0">
                    <p className="font-semibold text-slate-900 dark:text-white truncate text-sm">{selectedFile.name}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{formatFileSize(selectedFile.size)}</p>
                  </div>
                  <button
                    className="w-7 h-7 flex-shrink-0 bg-red-100 dark:bg-red-900/50 text-red-500 dark:text-red-400 rounded-lg hover:bg-red-200 dark:hover:bg-red-900/70 transition-colors flex items-center justify-center"
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedFile(null);
                    }}
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              </motion.div>
            ) : (
              /* ── Empty / drag state — horizontal compact layout ── */
              <motion.div
                key="no-file"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex items-center gap-5"
                style={{ minHeight: '130px' }}
              >
                {/* Icon */}
                <motion.div
                  className={`w-14 h-14 flex-shrink-0 rounded-xl flex items-center justify-center transition-colors
                    ${dragOver
                      ? 'bg-primary-100 dark:bg-primary-900/50 text-primary-600 dark:text-primary-400'
                      : 'bg-slate-100 dark:bg-forest-700 text-slate-400 dark:text-slate-500'}`}
                  animate={{ y: dragOver ? -4 : 0 }}
                >
                  <Upload className="w-7 h-7" />
                </motion.div>

                {/* Text */}
                <div className="flex-1 min-w-0">
                  <h3 className="text-base font-semibold text-slate-900 dark:text-white mb-1">
                    点击或拖拽文件至此处
                  </h3>
                  <p className="text-[13px] text-slate-400 dark:text-slate-500">
                    {formatHint} · {maxSizeHint}
                  </p>
                </div>

                {/* CTA button */}
                <motion.button
                  className="flex-shrink-0 bg-gradient-to-r from-primary-500 to-primary-600 text-white px-5 py-2.5 rounded-xl text-sm font-semibold shadow-md shadow-primary-500/25 hover:shadow-lg hover:shadow-primary-500/35 transition-all"
                  whileHover={{ scale: 1.03, y: -1 }}
                  whileTap={{ scale: 0.97 }}
                  onClick={(e) => {
                    e.stopPropagation();
                    document.getElementById('file-upload-input')?.click();
                  }}
                >
                  {selectButtonText}
                </motion.button>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>

        {/* ── Name input (conditional) ── */}
        {showNameInput && selectedFile && (
          <motion.div
            className="bg-white dark:bg-forest-800 rounded-xl px-5 py-4 shadow-sm dark:shadow-black/20 border border-slate-100 dark:border-forest-600"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            onClick={(e) => e.stopPropagation()}
          >
            <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1.5">{nameLabel}</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={namePlaceholder}
              className="w-full px-3 py-2 border border-slate-200 dark:border-forest-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-400 focus:border-transparent bg-white dark:bg-forest-700 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-forest-400 text-sm"
              disabled={uploading}
            />
          </motion.div>
        )}

        {/* ── Error ── */}
        <AnimatePresence>
          {error && (
            <motion.div
              initial={{ opacity: 0, y: -6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              className="px-4 py-3 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-xl text-red-600 dark:text-red-400 text-sm flex items-center gap-2"
            >
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              {error}
            </motion.div>
          )}
        </AnimatePresence>

        {/* ── Action buttons ── */}
        <div className="flex gap-3 justify-end">
          {onBack && (
            <motion.button
              onClick={onBack}
              className="px-5 py-2.5 border border-slate-200 dark:border-slate-600 rounded-xl text-slate-600 dark:text-slate-300 text-sm font-medium hover:bg-slate-50 dark:hover:bg-slate-700 transition-all"
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              返回
            </motion.button>
          )}
          {selectedFile && (
            <motion.button
              onClick={handleUpload}
              disabled={uploading}
              className="px-7 py-2.5 bg-gradient-to-r from-emerald-500 to-emerald-600 text-white rounded-xl text-sm font-semibold shadow-md shadow-emerald-500/25 hover:shadow-lg transition-all disabled:opacity-60 disabled:cursor-not-allowed flex items-center gap-2"
              whileHover={{ scale: uploading ? 1 : 1.02 }}
              whileTap={{ scale: uploading ? 1 : 0.98 }}
            >
              {uploading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  处理中...
                </>
              ) : (
                uploadButtonText
              )}
            </motion.button>
          )}
        </div>

      </div>
    </motion.div>
  );
}
