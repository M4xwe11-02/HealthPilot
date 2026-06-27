import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import Layout from './components/Layout';
import { useEffect, useState, Suspense, lazy } from 'react';
import { historyApi } from './api/history';
import type { UploadKnowledgeBaseResponse } from './api/knowledgebase';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';

// Lazy load components
const UploadPage = lazy(() => import('./pages/UploadPage'));
const HistoryList = lazy(() => import('./pages/HistoryPage'));
const HealthReportDetailPage = lazy(() => import('./pages/HealthReportDetailPage'));
const Consultation = lazy(() => import('./pages/ConsultationPage'));
const ConsultationHistoryPage = lazy(() => import('./pages/ConsultationHistoryPage'));
const KnowledgeBaseQueryPage = lazy(() => import('./pages/KnowledgeBaseQueryPage'));
const KnowledgeBaseUploadPage = lazy(() => import('./pages/KnowledgeBaseUploadPage'));
const KnowledgeBaseManagePage = lazy(() => import('./pages/KnowledgeBaseManagePage'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const PublicDocsPage = lazy(() => import('./pages/PublicDocsPage'));
const AdminPublicDocsPage = lazy(() => import('./pages/AdminPublicDocsPage'));

// Loading component
const Loading = () => (
  <div className="flex items-center justify-center min-h-[50vh]">
    <div className="w-10 h-10 border-3 border-slate-200 border-t-primary-500 rounded-full animate-spin" />
  </div>
);

// 上传页面包装器
function UploadPageWrapper() {
  const navigate = useNavigate();

  const handleUploadComplete = (healthReportId: number) => {
    // 异步模式：上传成功后跳转到报告库，让用户在列表中查看分析状态
    navigate('/history', { state: { newHealthReportId: healthReportId } });
  };

  return <UploadPage onUploadComplete={handleUploadComplete} />;
}

// 报告库列表包装器
function HistoryListWrapper() {
  const navigate = useNavigate();

  const handleSelectHealthReport = (id: number) => {
    navigate(`/history/${id}`);
  };

  return <HistoryList onSelectHealthReport={handleSelectHealthReport} />;
}

// 报告详情包装器
function HealthReportDetailWrapper() {
  const { healthReportId } = useParams<{ healthReportId: string }>();
  const navigate = useNavigate();

  if (!healthReportId) {
    return <Navigate to="/history" replace />;
  }

  const handleBack = () => {
    navigate('/history');
  };

  const handleStartConsultation = (reportText: string, healthReportId: number) => {
    navigate(`/consultation/${healthReportId}`, { state: { reportText } });
  };

  return (
    <HealthReportDetailPage
      healthReportId={parseInt(healthReportId, 10)}
      onBack={handleBack}
      onStartConsultation={handleStartConsultation}
    />
  );
}

// AI问诊包装器
function ConsultationWrapper() {
  const { healthReportId } = useParams<{ healthReportId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const [reportText, setReportText] = useState<string>('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 优先从location state获取reportText
    const stateText = (location.state as { reportText?: string })?.reportText;
    if (stateText) {
      setReportText(stateText);
      setLoading(false);
    } else if (healthReportId) {
      // 如果没有，从API获取报告详情
      historyApi.getHealthReportDetail(parseInt(healthReportId, 10))
        .then(healthReport => {
          setReportText(healthReport.reportText);
          setLoading(false);
        })
        .catch(err => {
          console.error('获取报告文本失败', err);
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, [healthReportId, location.state]);

  if (!healthReportId) {
    return <Navigate to="/history" replace />;
  }

  const handleBack = () => {
    // 尝试返回详情页，如果失败则返回历史列表
    navigate(`/history/${healthReportId}`, { replace: false });
  };

  const handleConsultationComplete = () => {
    // 问诊完成后跳转到问诊记录页
    navigate('/consultations');
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-10 h-10 border-3 border-slate-200 border-t-primary-500 rounded-full mx-auto mb-4 animate-spin" />
          <p className="text-slate-500">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <Consultation
      reportText={reportText}
      healthReportId={parseInt(healthReportId, 10)}
      onBack={handleBack}
      onConsultationComplete={handleConsultationComplete}
    />
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
      <Suspense fallback={<Loading />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
            {/* 默认重定向到上传页面 */}
            <Route index element={<Navigate to="/upload" replace />} />

            {/* 上传页面 */}
            <Route path="upload" element={<UploadPageWrapper />} />

            {/* 报告库 */}
            <Route path="history" element={<HistoryListWrapper />} />

            {/* 报告详情 */}
            <Route path="history/:healthReportId" element={<HealthReportDetailWrapper />} />

            {/* 问诊记录列表 */}
            <Route path="consultations" element={<ConsultationHistoryWrapper />} />

            {/* AI问诊 */}
            <Route path="consultation/:healthReportId" element={<ConsultationWrapper />} />

            {/* 知识库管理 */}
            <Route path="knowledgebase" element={<KnowledgeBaseManagePageWrapper />} />

            {/* 知识库上传 */}
            <Route path="knowledgebase/upload" element={<KnowledgeBaseUploadPageWrapper />} />

            {/* 问答助手（知识库聊天） */}
            <Route path="knowledgebase/chat" element={<KnowledgeBaseQueryPageWrapper />} />

            {/* 公共文档库 */}
            <Route path="public-docs" element={<PublicDocsPage />} />

            {/* 管理员文档管理 */}
            <Route path="admin/public-docs" element={<AdminPublicDocsPage />} />
          </Route>
        </Routes>
      </Suspense>
      </AuthProvider>
    </BrowserRouter>
  );
}

// 问诊记录页面包装器
function ConsultationHistoryWrapper() {
  const navigate = useNavigate();

  const handleBack = () => {
    navigate('/upload');
  };

  const handleViewConsultation = async (sessionId: string, healthReportId?: number) => {
    if (healthReportId) {
      // 如果有关联报告ID，跳转到报告详情页的问诊详情
      navigate(`/history/${healthReportId}`, {
        state: { viewConsultation: sessionId }
      });
    } else {
      // 否则尝试从问诊详情中获取关联报告ID
      try {
        await historyApi.getConsultationDetail(sessionId);
        // 问诊详情中没有关联报告ID，需要从其他地方获取
        // 暂时跳转到历史记录列表
        navigate('/history');
      } catch {
        navigate('/history');
      }
    }
  };

  return <ConsultationHistoryPage onBack={handleBack} onViewConsultation={handleViewConsultation} />;
}

// 知识库管理页面包装器
function KnowledgeBaseManagePageWrapper() {
  const navigate = useNavigate();

  const handleUpload = () => {
    navigate('/knowledgebase/upload');
  };

  const handleChat = () => {
    navigate('/knowledgebase/chat');
  };

  return <KnowledgeBaseManagePage onUpload={handleUpload} onChat={handleChat} />;
}

// 知识库问答页面包装器
function KnowledgeBaseQueryPageWrapper() {
  const navigate = useNavigate();
  const location = useLocation();
  const isChatMode = location.pathname === '/knowledgebase/chat';

  const handleBack = () => {
    if (isChatMode) {
      navigate('/knowledgebase');
    } else {
      navigate('/upload');
    }
  };

  const handleUpload = () => {
    navigate('/knowledgebase/upload');
  };

  return <KnowledgeBaseQueryPage onBack={handleBack} onUpload={handleUpload} />;
}

// 知识库上传页面包装器
function KnowledgeBaseUploadPageWrapper() {
  const navigate = useNavigate();

  const handleUploadComplete = (_result: UploadKnowledgeBaseResponse) => {
    // 上传完成后返回管理页面
    navigate('/knowledgebase');
  };

  const handleBack = () => {
    navigate('/knowledgebase');
  };

  return <KnowledgeBaseUploadPage onUploadComplete={handleUploadComplete} onBack={handleBack} />;
}

export default App;
