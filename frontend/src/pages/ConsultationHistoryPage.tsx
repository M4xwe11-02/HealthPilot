import {useCallback, useEffect, useRef, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {EvaluateStatus, historyApi, ConsultationItem} from '../api/history';
import {formatDate} from '../utils/date';
import DeleteConfirmDialog from '../components/DeleteConfirmDialog';
import {
  AlertCircle,
  CheckCircle,
  ChevronRight,
  Clock,
  Download,
  FileText,
  Loader2,
  PlayCircle,
  RefreshCw,
  Search,
  Trash2,
  TrendingUp,
  Users,
} from 'lucide-react';
import PageHeader from '../components/PageHeader';

interface ConsultationHistoryPageProps {
  onBack: () => void;
  onViewConsultation: (sessionId: string, healthReportId?: number) => void;
}

interface ConsultationWithHealthReport extends ConsultationItem {
  healthReportId: number;
  reportFilename: string;
  evaluateStatus?: EvaluateStatus;
  evaluateError?: string;
}

interface InterviewStats {
  totalCount: number;
  completedCount: number;
  averageScore: number;
}

// 统计卡片组件
function StatCard({
  icon: Icon,
  label,
  value,
  suffix,
  accentColor,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: number | string;
  suffix?: string;
  accentColor: string;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="relative bg-white dark:bg-forest-800 rounded-2xl p-5 shadow-sm border border-slate-100 dark:border-forest-600 overflow-hidden"
    >
      {/* Corner diamond */}
      <div className="absolute top-4 right-4 w-[5px] h-[5px] rotate-45 bg-primary-300/40 dark:bg-primary-700/50" />
      {/* Bottom accent line */}
      <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-gradient-to-r from-primary-400/40 via-primary-300/20 to-transparent" />

      <div className="flex flex-col gap-3">
        <div className="flex items-center gap-2.5">
          <div className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 ${accentColor}`}>
            <Icon className="w-3.5 h-3.5 text-white" />
          </div>
          <span className="text-sm font-normal text-slate-500 dark:text-slate-400 select-none">
            {label}
          </span>
        </div>
        <div>
          <span
            className="font-display font-bold text-slate-900 dark:text-white leading-none tracking-tight"
            style={{ fontSize: '2.25rem' }}
          >
            {value}
          </span>
          {suffix && (
            <span className="text-sm font-normal text-slate-400 dark:text-slate-500 ml-1.5">{suffix}</span>
          )}
        </div>
      </div>
    </motion.div>
  );
}

// 判断是否为已完成状态（包括 COMPLETED 和 EVALUATED）
function isCompletedStatus(status: string): boolean {
  return status === 'COMPLETED' || status === 'EVALUATED';
}

// 判断评估是否完成
function isEvaluateCompleted(consultation: ConsultationWithHealthReport): boolean {
  // 如果 evaluateStatus 存在且为 COMPLETED，则评估已完成
  if (consultation.evaluateStatus === 'COMPLETED') return true;
  // 向后兼容：如果 status 为 EVALUATED，也认为评估已完成
  if (consultation.status === 'EVALUATED') return true;
  return false;
}

// 判断是否正在评估中
function isEvaluating(consultation: ConsultationWithHealthReport): boolean {
  return consultation.evaluateStatus === 'PENDING' || consultation.evaluateStatus === 'PROCESSING';
}

// 判断评估是否失败
function isEvaluateFailed(consultation: ConsultationWithHealthReport): boolean {
  return consultation.evaluateStatus === 'FAILED';
}

// 状态图标
function StatusIcon({ consultation }: { consultation: ConsultationWithHealthReport }) {
  // 评估失败
  if (isEvaluateFailed(consultation)) {
      return <AlertCircle className="w-4 h-4 text-red-500 dark:text-red-400"/>;
  }
  // 正在评估
  if (isEvaluating(consultation)) {
      return <RefreshCw className="w-4 h-4 text-blue-500 dark:text-blue-400 animate-spin"/>;
  }
  // 评估完成
  if (isEvaluateCompleted(consultation)) {
      return <CheckCircle className="w-4 h-4 text-green-500 dark:text-green-400"/>;
  }
  // 问诊进行中
  if (consultation.status === 'IN_PROGRESS') {
      return <PlayCircle className="w-4 h-4 text-blue-500 dark:text-blue-400"/>;
  }
  // 问诊已完成但评估未开始
  if (isCompletedStatus(consultation.status)) {
      return <Clock className="w-4 h-4 text-yellow-500 dark:text-yellow-400"/>;
  }
  // 已创建
    return <Clock className="w-4 h-4 text-yellow-500 dark:text-yellow-400"/>;
}

// 状态文本
function getStatusText(consultation: ConsultationWithHealthReport): string {
  // 评估失败
  if (isEvaluateFailed(consultation)) {
    return '评估失败';
  }
  // 正在评估
  if (isEvaluating(consultation)) {
    return consultation.evaluateStatus === 'PROCESSING' ? '评估中' : '等待评估';
  }
  // 评估完成
  if (isEvaluateCompleted(consultation)) {
    return '已完成';
  }
  // 问诊进行中
  if (consultation.status === 'IN_PROGRESS') {
    return '进行中';
  }
  // 问诊已完成但评估未开始
  if (isCompletedStatus(consultation.status)) {
    return '已提交';
  }
  return '已创建';
}

// 获取分数颜色
function getScoreColor(score: number): string {
  if (score >= 80) return 'bg-green-500';
  if (score >= 60) return 'bg-yellow-500';
  return 'bg-red-500';
}

export default function ConsultationHistoryPage({ onBack: _onBack, onViewConsultation }: ConsultationHistoryPageProps) {
  const [consultations, setConsultations] = useState<ConsultationWithHealthReport[]>([]);
  const [stats, setStats] = useState<InterviewStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [deletingSessionId, setDeletingSessionId] = useState<string | null>(null);
  const [deleteItem, setDeleteItem] = useState<ConsultationWithHealthReport | null>(null);
  const [exporting, setExporting] = useState<string | null>(null);
  const pollingRef = useRef<number | null>(null);

  const loadAllInterviews = useCallback(async (isPolling = false) => {
    if (!isPolling) {
      setLoading(true);
    }
    try {
      const healthReports = await historyApi.getHealthReports();
      const allConsultations: ConsultationWithHealthReport[] = [];

      for (const healthReport of healthReports) {
        const detail = await historyApi.getHealthReportDetail(healthReport.id);
        if (detail.consultations && detail.consultations.length > 0) {
          detail.consultations.forEach(consultation => {
            allConsultations.push({
              ...consultation,
              healthReportId: healthReport.id,
              reportFilename: healthReport.filename
            });
          });
        }
      }

      // 按创建时间倒序排序
      allConsultations.sort((a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );

      setConsultations(allConsultations);

      // 计算统计信息（只统计评估已完成的问诊）
      const evaluated = allConsultations.filter(i => isEvaluateCompleted(i));
      const totalScore = evaluated.reduce((sum, consultation) => sum + (consultation.overallScore || 0), 0);
      setStats({
        totalCount: allConsultations.length,
        completedCount: evaluated.length,
        averageScore: evaluated.length > 0 ? Math.round(totalScore / evaluated.length) : 0,
      });
    } catch (err) {
      console.error('加载问诊记录失败', err);
    } finally {
      if (!isPolling) {
        setLoading(false);
      }
    }
  }, []);

  // 初始加载
  useEffect(() => {
    loadAllInterviews();
  }, [loadAllInterviews]);

  // 轮询检查评估状态
  useEffect(() => {
    // 检查是否有正在评估的问诊
    const hasEvaluating = consultations.some(i => isEvaluating(i));

    if (hasEvaluating) {
      // 启动轮询
      pollingRef.current = window.setInterval(() => {
        loadAllInterviews(true);
      }, 3000); // 每3秒轮询一次
    } else {
      // 停止轮询
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    }

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, [consultations, loadAllInterviews]);

  const handleDeleteClick = (consultation: ConsultationWithHealthReport, e: React.MouseEvent) => {
    e.stopPropagation();
    setDeleteItem(consultation);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteItem) return;

    setDeletingSessionId(deleteItem.sessionId);
    try {
      await historyApi.deleteConsultation(deleteItem.sessionId);
      await loadAllInterviews();
      setDeleteItem(null);
    } catch (err) {
      alert(err instanceof Error ? err.message : '删除失败，请稍后重试');
    } finally {
      setDeletingSessionId(null);
    }
  };

  const handleExport = async (sessionId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setExporting(sessionId);
    try {
      const blob = await historyApi.exportConsultationPdf(sessionId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `问诊报告_${sessionId.slice(-8)}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('导出失败，请重试');
    } finally {
      setExporting(null);
    }
  };

  const filteredConsultations = consultations.filter(consultation =>
    consultation.reportFilename.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <motion.div
      className="w-full"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
    >
      {/* 头部 */}
      <PageHeader
        label="Consultation Records"
        title="问诊记录"
        subtitle="查看和管理所有 AI 健康问诊记录"
        action={
          <motion.div
            className="flex items-center gap-3 bg-white dark:bg-forest-800 border border-slate-200 dark:border-forest-600 rounded-xl px-4 py-2.5 min-w-[260px] focus-within:border-primary-400 focus-within:ring-2 focus-within:ring-primary-100 dark:focus-within:ring-primary-900/30 transition-all"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
          >
            <Search className="w-5 h-5 text-slate-400" />
            <input
              type="text"
              placeholder="搜索报告名称..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="flex-1 outline-none text-slate-700 dark:text-slate-200 placeholder:text-slate-400 bg-transparent"
            />
          </motion.div>
        }
      />

      {/* 统计卡片 */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-5">
          <StatCard
            icon={Users}
            label="问诊总次数"
            value={stats.totalCount}
            accentColor="bg-primary-500"
          />
          <StatCard
            icon={CheckCircle}
            label="已完成评估"
            value={stats.completedCount}
            accentColor="bg-emerald-500"
          />
          <StatCard
            icon={TrendingUp}
            label="综合健康评分"
            value={stats.averageScore}
            suffix="分"
            accentColor="bg-teal-600"
          />
        </div>
      )}

      {/* 加载状态 */}
      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
        </div>
      )}

      {/* 空状态 */}
      {!loading && filteredConsultations.length === 0 && (
        <motion.div
            className="text-center py-20 bg-white dark:bg-forest-800 rounded-2xl shadow-sm border border-slate-100 dark:border-forest-600"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
        >
            <Users className="w-16 h-16 text-slate-300 dark:text-slate-600 mx-auto mb-4"/>
            <h3 className="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">暂无问诊记录</h3>
            <p className="text-slate-500 dark:text-slate-400">开始一次AI健康问诊后，记录将显示在这里</p>
        </motion.div>
      )}

      {/* 表格 */}
      {!loading && filteredConsultations.length > 0 && (
        <motion.div
            className="bg-white dark:bg-forest-800 rounded-2xl shadow-sm border border-slate-100 dark:border-forest-600 overflow-hidden min-h-[340px]"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <table className="w-full">
              <thead className="bg-slate-50 dark:bg-forest-700/40 border-b border-slate-100 dark:border-forest-600">
              <tr>
                  <th className="text-left px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">关联报告</th>
                  <th className="text-left px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">问题数</th>
                  <th className="text-left px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">状态</th>
                  <th className="text-left px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">健康评分</th>
                  <th className="text-left px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">创建时间</th>
                  <th className="text-right px-6 py-4 text-xs font-semibold text-slate-500 dark:text-forest-200 uppercase tracking-wide">操作</th>
              </tr>
            </thead>
            <tbody>
              <AnimatePresence>
                {filteredConsultations.map((consultation, index) => (
                  <motion.tr
                    key={consultation.sessionId}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.05 }}
                    onClick={() => onViewConsultation(consultation.sessionId, consultation.healthReportId)}
                    className="border-b border-slate-100 dark:border-forest-700 hover:bg-slate-50 dark:hover:bg-forest-700/40 cursor-pointer transition-colors group"
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <FileText className="w-5 h-5 text-slate-400" />
                        <div>
                            <p className="font-medium text-slate-800 dark:text-white">{consultation.reportFilename}</p>
                            <p className="text-xs text-slate-400 dark:text-slate-500">问诊 #{consultation.sessionId.slice(-8)}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span
                          className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 rounded-lg text-sm">
                        {consultation.totalQuestions} 问
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <StatusIcon consultation={consultation} />
                          <span className="text-sm text-slate-600 dark:text-slate-300">
                          {getStatusText(consultation)}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {isEvaluateCompleted(consultation) && consultation.overallScore !== null ? (
                        <div className="flex items-center gap-3">
                            <div className="w-16 h-2 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden">
                            <motion.div
                              className={`h-full ${getScoreColor(consultation.overallScore)} rounded-full`}
                              initial={{ width: 0 }}
                              animate={{ width: `${consultation.overallScore}%` }}
                              transition={{ duration: 0.8, delay: index * 0.05 }}
                            />
                          </div>
                            <span className="font-bold text-slate-800 dark:text-white">{consultation.overallScore}</span>
                        </div>
                      ) : isEvaluating(consultation) ? (
                          <span className="text-blue-500 dark:text-blue-400 text-sm">评估中...</span>
                      ) : isEvaluateFailed(consultation) ? (
                          <span className="text-red-500 dark:text-red-400 text-sm"
                                title={consultation.evaluateError}>失败</span>
                      ) : (
                          <span className="text-slate-400 dark:text-slate-500">-</span>
                      )}
                    </td>
                      <td className="px-6 py-4 text-sm text-slate-500 dark:text-slate-400">
                      {formatDate(consultation.createdAt)}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        {/* 导出按钮 */}
                        {isEvaluateCompleted(consultation) && (
                          <button
                            onClick={(e) => handleExport(consultation.sessionId, e)}
                            disabled={exporting === consultation.sessionId}
                            className="p-2 text-slate-400 hover:text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/30 rounded-lg transition-colors disabled:opacity-50"
                            title="导出PDF"
                          >
                            {exporting === consultation.sessionId ? (
                              <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                              <Download className="w-4 h-4" />
                            )}
                          </button>
                        )}
                        {/* 删除按钮 */}
                        <button
                          onClick={(e) => handleDeleteClick(consultation, e)}
                          disabled={deletingSessionId === consultation.sessionId}
                          className="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors disabled:opacity-50"
                          title="删除"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                          <ChevronRight
                              className="w-5 h-5 text-slate-300 dark:text-slate-600 group-hover:text-primary-500 group-hover:translate-x-1 transition-all"/>
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
        open={deleteItem !== null}
        item={deleteItem ? { id: deleteItem.id, sessionId: deleteItem.sessionId } : null}
        itemType="问诊记录"
        loading={deletingSessionId !== null}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteItem(null)}
      />
    </motion.div>
  );
}
