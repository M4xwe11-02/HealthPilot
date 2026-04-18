import {useEffect, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {historyApi, HealthReportListItem} from '../api/history';
import DeleteConfirmDialog from '../components/DeleteConfirmDialog';
import PageHeader from '../components/PageHeader';
import {formatDateOnly} from '../utils/date';
import {getScoreProgressColor} from '../utils/score';

interface HistoryListProps {
  onSelectHealthReport: (id: number) => void;
}

export default function HistoryList({ onSelectHealthReport }: HistoryListProps) {
  const [healthReports, setHealthReports] = useState<HealthReportListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<{ id: number; filename: string } | null>(null);

  useEffect(() => {
    loadHealthReports();
  }, []);

  const loadHealthReports = async () => {
    setLoading(true);
    try {
      const data = await historyApi.getHealthReports();
      setHealthReports(data);
    } catch (err) {
      console.error('加载历史记录失败', err);
    } finally {
      setLoading(false);
    }
  };



  const handleDeleteClick = (id: number, filename: string, e: React.MouseEvent) => {
    e.stopPropagation(); // 阻止触发行点击事件
    setDeleteConfirm({ id, filename });
  };

  const handleDeleteConfirm = async () => {
    if (!deleteConfirm) return;

    const { id } = deleteConfirm;
    setDeletingId(id);
    try {
      await historyApi.deleteHealthReport(id);
      // 重新加载列表
      await loadHealthReports();
      setDeleteConfirm(null);
    } catch (err) {
      alert(err instanceof Error ? err.message : '删除失败，请稍后重试');
    } finally {
      setDeletingId(null);
    }
  };

  const filteredHealthReports = healthReports.filter(healthReport =>
    healthReport.filename.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <motion.div
      className="h-full flex flex-col"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
    >
      {/* 头部 */}
      <PageHeader
        label="Health Records"
        title="报告库"
        subtitle="管理您已分析过的所有体检报告及问诊记录"
        action={
          <motion.div
            className="flex items-center gap-3 bg-white dark:bg-forest-800 border border-slate-200 dark:border-forest-600 rounded-xl px-4 py-2.5 min-w-[260px] focus-within:border-primary-400 focus-within:ring-2 focus-within:ring-primary-100 dark:focus-within:ring-primary-900/30 transition-all"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
          >
            <svg className="w-5 h-5 text-slate-400" viewBox="0 0 24 24" fill="none">
              <circle cx="11" cy="11" r="8" stroke="currentColor" strokeWidth="2"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
            </svg>
            <input
              type="text"
              placeholder="搜索报告..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="flex-1 outline-none text-slate-700 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-forest-400 bg-transparent"
            />
          </motion.div>
        }
      />

      {/* 加载状态 */}
      {loading && (
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <motion.div
              className="w-10 h-10 border-[3px] border-slate-200 dark:border-forest-600 border-t-primary-500 rounded-full mx-auto mb-4"
              animate={{ rotate: 360 }}
              transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
            />
            <p className="text-slate-500 dark:text-slate-400">加载中...</p>
          </div>
        </div>
      )}

      {/* 空状态 */}
      {!loading && filteredHealthReports.length === 0 && (
        <motion.div
          className="flex-1 flex flex-col items-center justify-center bg-white dark:bg-forest-800 rounded-2xl border border-slate-100 dark:border-forest-600 shadow-sm relative overflow-hidden min-h-[260px]"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
        >
          {/* Precision corner marks */}
          <div className="absolute top-4 left-4"><div className="w-4 h-px bg-primary-400/40"/><div className="w-px h-4 bg-primary-400/40"/></div>
          <div className="absolute top-4 right-4 flex flex-col items-end"><div className="w-4 h-px bg-primary-400/40"/><div className="w-px h-4 bg-primary-400/40 ml-auto"/></div>
          <div className="absolute bottom-4 left-4 flex flex-col justify-end"><div className="w-px h-4 bg-primary-400/40"/><div className="w-4 h-px bg-primary-400/40"/></div>
          <div className="absolute bottom-4 right-4 flex flex-col items-end justify-end"><div className="w-px h-4 bg-primary-400/40 ml-auto"/><div className="w-4 h-px bg-primary-400/40"/></div>
          {/* Subtle bg gradient */}
          <div className="absolute inset-0 bg-gradient-to-b from-primary-50/30 via-transparent to-transparent dark:from-primary-900/10 pointer-events-none" />
          <div className="relative text-center px-6">
            <svg className="w-16 h-16 mx-auto mb-5 text-primary-300 dark:text-primary-800" viewBox="0 0 24 24" fill="none">
              <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              <polyline points="14,2 14,8 20,8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              <line x1="8" y1="13" x2="16" y2="13" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
              <line x1="8" y1="17" x2="13" y2="17" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
            <h3 className="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">暂无体检报告</h3>
            <p className="text-sm text-slate-500 dark:text-slate-400">上传体检报告开始您的第一次 AI 健康分析</p>
          </div>
        </motion.div>
      )}

      {/* 表格 */}
      {!loading && filteredHealthReports.length > 0 && (
          <motion.div
              className="bg-white dark:bg-forest-800 rounded-2xl shadow-sm overflow-hidden border border-slate-100 dark:border-forest-600 min-h-[340px]"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <table className="w-full">
            <thead>
            <tr className="bg-slate-50 dark:bg-forest-700/40 border-b border-slate-100 dark:border-forest-600">
                <th className="text-left px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">报告名称</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">上传日期</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">健康
                    评分
                </th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">问诊状态</th>
                <th className="w-20"></th>
              </tr>
            </thead>
            <tbody>
              <AnimatePresence>
                {filteredHealthReports.map((healthReport, index) => (
                  <motion.tr
                    key={healthReport.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.05 }}
                    onClick={() => onSelectHealthReport(healthReport.id)}
                    className="border-b border-slate-100 dark:border-forest-700 last:border-0 hover:bg-slate-50 dark:hover:bg-forest-700/40 cursor-pointer transition-colors group"
                  >
                    <td className="px-6 py-5">
                      <div className="flex items-center gap-4">
                          <div
                              className="w-10 h-10 bg-primary-50 dark:bg-primary-900/30 rounded-xl flex items-center justify-center text-primary-500 dark:text-primary-400">
                          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none">
                            <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                            <polyline points="14,2 14,8 20,8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                          </svg>
                        </div>
                          <span className="font-medium text-slate-800 dark:text-white">{healthReport.filename}</span>
                      </div>
                    </td>
                      <td className="px-6 py-5 text-slate-500 dark:text-slate-400">{formatDateOnly(healthReport.uploadedAt)}</td>
                    <td className="px-6 py-5">
                      {healthReport.latestScore !== undefined ? (
                        <div className="flex items-center gap-3">
                            <div className="w-20 h-2 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden">
                                <motion.div
                              className={`h-full ${getScoreProgressColor(healthReport.latestScore)} rounded-full`}
                              initial={{ width: 0 }}
                              animate={{ width: `${healthReport.latestScore}%` }}
                              transition={{ duration: 0.8, delay: index * 0.05 }}
                            />
                          </div>
                            <span className="font-bold text-slate-800 dark:text-white">{healthReport.latestScore}</span>
                        </div>
                      ) : (
                          <span className="text-slate-400 dark:text-slate-500">-</span>
                      )}
                    </td>
                    <td className="px-6 py-5">
                      {healthReport.consultationCount > 0 ? (
                          <span
                              className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 dark:bg-emerald-900 text-emerald-600 rounded-full text-sm font-medium">
                          <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none">
                            <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2"/>
                            <polyline points="9,12 11,14 15,10" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                          </svg>
                          已问诊
                        </span>
                      ) : (
                          <span
                              className="inline-flex px-3 py-1 bg-slate-100 dark:bg-slate-700 text-slate-500 dark:text-slate-300 rounded-full text-sm">待问诊</span>
                      )}
                    </td>
                    <td className="px-4">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={(e) => handleDeleteClick(healthReport.id, healthReport.filename, e)}
                          disabled={deletingId === healthReport.id}
                          className="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                          title="删除报告"
                        >
                          {deletingId === healthReport.id ? (
                            <motion.div
                              className="w-5 h-5 border-2 border-red-500 border-t-transparent rounded-full"
                              animate={{ rotate: 360 }}
                              transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                            />
                          ) : (
                            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none">
                              <path d="M3 6H5H21M8 6V4C8 3.46957 8.21071 2.96086 8.58579 2.58579C8.96086 2.21071 9.46957 2 10 2H14C14.5304 2 15.0391 2.21071 15.4142 2.58579C15.7893 2.96086 16 3.46957 16 4V6M19 6V20C19 20.5304 18.7893 21.0391 18.4142 21.4142C18.0391 21.7893 17.5304 22 17 22H7C6.46957 22 5.96086 21.7893 5.58579 21.4142C5.21071 21.0391 5 20.5304 5 20V6H19Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                              <path d="M10 11V17M14 11V17" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                            </svg>
                          )}
                        </button>
                          <svg
                              className="w-5 h-5 text-slate-300 dark:text-slate-600 group-hover:text-primary-500 group-hover:translate-x-1 transition-all"
                              viewBox="0 0 24 24" fill="none">
                          <polyline points="9,18 15,12 9,6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                      </div>
                    </td>
                  </motion.tr>
                ))}
              </AnimatePresence>
            </tbody>
          </table>
        </motion.div>
      )}

          {/* 删除确认对话框 */}
      <DeleteConfirmDialog
        open={deleteConfirm !== null}
        item={deleteConfirm}
        itemType="体检报告"
        loading={deletingId !== null}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteConfirm(null)}
        customMessage={
          deleteConfirm ? (
            <>
              <p className="mb-2">确定要删除报告 <strong>"{deleteConfirm.filename}"</strong> 吗？</p>
                <p className="text-sm text-slate-500 dark:text-slate-400 mb-2">删除后将同时删除：</p>
                <ul className="text-sm text-slate-500 dark:text-red-400 list-disc list-inside mb-2">
                <li>健康分析记录</li>
                <li>所有AI问诊记录</li>
              </ul>
              <p className="text-sm font-semibold text-red-600">此操作不可恢复！</p>
            </>
          ) : undefined
        }
      />
    </motion.div>
  );
}
