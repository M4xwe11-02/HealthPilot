import {useCallback, useEffect, useState} from 'react';
import {useLocation} from 'react-router-dom';
import {AnimatePresence, motion} from 'framer-motion';
import {historyApi, ConsultationDetail, HealthReportDetail} from '../api/history';
import AnalysisPanel from '../components/AnalysisPanel';
import ConsultationPanel from '../components/ConsultationPanel';
import ConsultationDetailPanel from '../components/ConsultationDetailPanel';
import {formatDateOnly} from '../utils/date';
import {CheckSquare, ChevronLeft, Clock, Download, MessageSquare, Mic} from 'lucide-react';

interface HealthReportDetailPageProps {
  healthReportId: number;
  onBack: () => void;
  onStartConsultation: (reportText: string, healthReportId: number) => void;
}

type TabType = 'analysis' | 'consultation';
type DetailViewType = 'list' | 'consultationDetail';

export default function HealthReportDetailPage({ healthReportId, onBack, onStartConsultation }: HealthReportDetailPageProps) {
  const location = useLocation();
  const [healthReport, setHealthReport] = useState<HealthReportDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<TabType>('analysis');
  const [exporting, setExporting] = useState<string | null>(null);
  const [[page, direction], setPage] = useState([0, 0]);
  const [detailView, setDetailView] = useState<DetailViewType>('list');
  const [selectedConsultation, setSelectedConsultation] = useState<ConsultationDetail | null>(null);
  const [loadingConsultation, setLoadingConsultation] = useState(false);
  const [reanalyzing, setReanalyzing] = useState(false);

  // 静默加载数据（用于轮询）
  const loadHealthReportDetailSilent = useCallback(async () => {
    try {
      const data = await historyApi.getHealthReportDetail(healthReportId);
      setHealthReport(data);
    } catch (err) {
      console.error('加载报告详情失败', err);
    }
  }, [healthReportId]);

  const loadHealthReportDetail = useCallback(async () => {
    setLoading(true);
    try {
      const data = await historyApi.getHealthReportDetail(healthReportId);
      setHealthReport(data);
    } catch (err) {
      console.error('加载报告详情失败', err);
    } finally {
      setLoading(false);
    }
  }, [healthReportId]);

  useEffect(() => {
    loadHealthReportDetail();
  }, [loadHealthReportDetail]);

  // 轮询：当分析状态为待处理时，每5秒刷新一次
  // 待处理判断：显式的 PENDING/PROCESSING 状态，或状态未定义且无分析结果
  useEffect(() => {
    const isProcessing = healthReport && (
      healthReport.analyzeStatus === 'PENDING' ||
      healthReport.analyzeStatus === 'PROCESSING' ||
      (healthReport.analyzeStatus === undefined && (!healthReport.analyses || healthReport.analyses.length === 0))
    );

    if (isProcessing && !loading) {
      const timer = setInterval(() => {
        loadHealthReportDetailSilent();
      }, 5000);

      return () => clearInterval(timer);
    }
  }, [healthReport, loading, loadHealthReportDetailSilent]);

  // 重新分析
  const handleReanalyze = async () => {
    try {
      setReanalyzing(true);
      await historyApi.reanalyze(healthReportId);
      await loadHealthReportDetailSilent();
    } catch (err) {
      console.error('重新分析失败', err);
    } finally {
      setReanalyzing(false);
    }
  };

  // 检查是否需要自动打开问诊详情
  useEffect(() => {
    const viewConsultationSession = (location.state as { viewConsultation?: string })?.viewConsultation;
    if (viewConsultationSession && healthReport) {
      // 切换到问诊标签页
      setActiveTab('consultation');
      // 加载并显示问诊详情
      const loadAndViewInterview = async () => {
        setLoadingConsultation(true);
        try {
          const detail = await historyApi.getConsultationDetail(viewConsultationSession);
          setSelectedConsultation(detail);
          setDetailView('consultationDetail');
        } catch (err) {
          console.error('加载问诊详情失败', err);
        } finally {
          setLoadingConsultation(false);
        }
      };
      loadAndViewInterview();
    }
  }, [location.state, healthReport]);

  const handleExportAnalysisPdf = async () => {
    setExporting('analysis');
    try {
      const blob = await historyApi.exportAnalysisPdf(healthReportId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `健康分析报告_${healthReport?.filename || healthReportId}.pdf`;
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

  const handleExportInterviewPdf = async (sessionId: string) => {
    setExporting(sessionId);
    try {
      const blob = await historyApi.exportConsultationPdf(sessionId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `问诊报告_${sessionId}.pdf`;
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

  const handleViewConsultation = async (sessionId: string) => {
    setLoadingConsultation(true);
    try {
      const detail = await historyApi.getConsultationDetail(sessionId);
      setSelectedConsultation(detail);
      setDetailView('consultationDetail');
    } catch (err) {
      alert('加载问诊详情失败');
    } finally {
      setLoadingConsultation(false);
    }
  };

  const handleBackToConsultationList = () => {
    setDetailView('list');
    setSelectedConsultation(null);
  };

  const handleDeleteConsultation = async (sessionId: string) => {
    // 删除后重新加载报告详情
    await loadHealthReportDetail();
    // 如果删除的是当前查看的问诊，返回列表
    if (selectedConsultation?.sessionId === sessionId) {
      setDetailView('list');
      setSelectedConsultation(null);
    }
  };

  const handleTabChange = (tab: TabType) => {
    const newPage = tab === 'analysis' ? 0 : 1;
    setPage([newPage, newPage > page ? 1 : -1]);
    setActiveTab(tab);
    setDetailView('list');
    setSelectedConsultation(null);
  };

  const slideVariants = {
    enter: (direction: number) => ({
      x: direction > 0 ? 300 : -300,
      opacity: 0,
    }),
    center: {
      x: 0,
      opacity: 1,
    },
    exit: (direction: number) => ({
      x: direction < 0 ? 300 : -300,
      opacity: 0,
    }),
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
          <motion.div
              className="w-12 h-12 border-4 border-slate-200 dark:border-slate-600 border-t-primary-500 rounded-full"
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
        />
      </div>
    );
  }

  if (!healthReport) {
    return (
      <div className="text-center py-20">
        <p className="text-red-500 mb-4">加载失败，请返回重试</p>
        <button onClick={onBack} className="px-6 py-2 bg-primary-500 text-white rounded-lg">返回列表</button>
      </div>
    );
  }

  const latestAnalysis = healthReport.analyses?.[0];
  const tabs = [
    { id: 'analysis' as const, label: '健康分析', icon: CheckSquare },
    { id: 'consultation' as const, label: '问诊记录', icon: MessageSquare, count: healthReport.consultations?.length || 0 },
  ];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="w-full"
    >
      {/* 顶部导航栏 */}
      <div className="flex justify-between items-center mb-5 flex-wrap gap-4">
        <div className="flex items-center gap-4">
          <motion.button
            onClick={detailView === 'consultationDetail' ? handleBackToConsultationList : onBack}
            className="w-10 h-10 bg-white dark:bg-forest-800 rounded-xl flex items-center justify-center
              text-slate-500 hover:bg-slate-50 dark:hover:bg-forest-700
              hover:text-primary-600 dark:hover:text-primary-400
              border border-slate-100 dark:border-forest-600
              transition-all shadow-sm"
            whileHover={{ scale: 1.05, x: -1 }}
            whileTap={{ scale: 0.95 }}
          >
            <ChevronLeft className="w-5 h-5" />
          </motion.button>
          <div>
            {/* Label above */}
            <div className="flex items-center gap-2 mb-[3px]">
              <span
                className="font-display italic font-normal uppercase text-primary-500 dark:text-primary-400 select-none"
                style={{ fontSize: '9px', letterSpacing: '0.35em' }}
              >
                {detailView === 'consultationDetail' ? 'Consultation Detail' : 'Health Report'}
              </span>
              <div className="h-px w-8 bg-gradient-to-r from-primary-400/40 to-transparent" />
              <div className="w-[4px] h-[4px] rotate-45 bg-primary-400 shadow-[0_0_4px_rgba(52,211,153,0.7)]" />
            </div>
            <h2 className="font-display font-bold text-[1.3rem] text-slate-900 dark:text-white leading-none tracking-tight">
              {detailView === 'consultationDetail' ? `问诊详情 #${selectedConsultation?.sessionId?.slice(-6) || ''}` : healthReport.filename}
            </h2>
            <p className="text-[12px] text-slate-500 dark:text-slate-400 flex items-center gap-1.5 mt-1">
              <Clock className="w-3.5 h-3.5" />
              {detailView === 'consultationDetail'
                ? `完成于 ${formatDateOnly(selectedConsultation?.completedAt || selectedConsultation?.createdAt || '')}`
                : `上传于 ${formatDateOnly(healthReport.uploadedAt)}`
              }
            </p>
          </div>
        </div>

        <div className="flex gap-3">
          {detailView === 'consultationDetail' && selectedConsultation && (
            <motion.button
              onClick={() => handleExportInterviewPdf(selectedConsultation?.sessionId)}
              disabled={exporting === selectedConsultation?.sessionId}
              className="px-5 py-2.5 border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-800 rounded-xl text-slate-600 dark:text-slate-300 font-medium hover:bg-slate-50 transition-all disabled:opacity-50 flex items-center gap-2"
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <Download className="w-4 h-4" />
              {exporting === selectedConsultation?.sessionId ? '导出中...' : '导出 PDF'}
            </motion.button>
          )}
          {detailView !== 'consultationDetail' && (
            <motion.button
              onClick={() => onStartConsultation(healthReport.reportText, healthReportId)}
              className="px-5 py-2.5 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-xl font-medium shadow-lg shadow-primary-500/30 hover:shadow-xl transition-all flex items-center gap-2"
              whileHover={{ scale: 1.02, y: -1 }}
              whileTap={{ scale: 0.98 }}
            >
              <Mic className="w-4 h-4" />
              开始 AI 问诊
            </motion.button>
          )}
        </div>
      </div>

      {/* 标签页切换 - 仅在非问诊详情时显示 */}
      {detailView !== 'consultationDetail' && (
          <div className="bg-white dark:bg-slate-800 rounded-2xl p-2 mb-4 inline-flex gap-1">
          {tabs.map((tab) => (
            <motion.button
              key={tab.id}
              onClick={() => handleTabChange(tab.id)}
              className={`relative px-6 py-3 rounded-xl font-medium flex items-center gap-2 transition-colors
                ${activeTab === tab.id ? 'text-primary-600 dark:text-primary-400' : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'}`}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              {activeTab === tab.id && (
                <motion.div
                  layoutId="activeTab"
                  className="absolute inset-0 bg-primary-50 dark:bg-primary-900 rounded-xl"
                  transition={{ type: "spring", bounce: 0.2, duration: 0.6 }}
                />
              )}
              <span className="relative z-10 flex items-center gap-2">
                <tab.icon className="w-5 h-5" />
                {tab.label}
                {tab.count !== undefined && tab.count > 0 && (
                    <span
                        className="px-2 py-0.5 bg-primary-100 dark:bg-primary-900 text-primary-600 dark:text-primary-400 text-xs rounded-full">{tab.count}</span>
                )}
              </span>
            </motion.button>
          ))}
        </div>
      )}

      {/* 内容区域 */}
      <div className="relative overflow-hidden">
        {detailView === 'consultationDetail' && selectedConsultation ? (
          <ConsultationDetailPanel consultation={selectedConsultation} />
        ) : (
          <AnimatePresence initial={false} custom={direction} mode="wait">
            <motion.div
              key={activeTab}
              custom={direction}
              variants={slideVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ type: "spring", stiffness: 300, damping: 30 }}
            >
              {activeTab === 'analysis' ? (
                <AnalysisPanel
                  analysis={latestAnalysis}
                  analyzeStatus={healthReport.analyzeStatus}
                  analyzeError={healthReport.analyzeError}
                  onExport={handleExportAnalysisPdf}
                  exporting={exporting === 'analysis'}
                  onReanalyze={handleReanalyze}
                  reanalyzing={reanalyzing}
                />
              ) : (
                  <ConsultationPanel
                      consultations={healthReport.consultations || []}
                  onStartConsultation={() => onStartConsultation(healthReport.reportText, healthReportId)}
                  onViewConsultation={handleViewConsultation}
                  onExportConsultation={handleExportInterviewPdf}
                  onDeleteConsultation={handleDeleteConsultation}
                  exporting={exporting}
                  loadingConsultation={loadingConsultation}
                />
              )}
            </motion.div>
          </AnimatePresence>
        )}
      </div>
    </motion.div>
  );
}
