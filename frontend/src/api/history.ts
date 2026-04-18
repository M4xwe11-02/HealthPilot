import { request } from './request';

export type AnalyzeStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type EvaluateStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface HealthReportListItem {
  id: number;
  filename: string;
  fileSize: number;
  uploadedAt: string;
  accessCount: number;
  latestScore?: number;
  lastAnalyzedAt?: string;
  consultationCount: number;
  analyzeStatus?: AnalyzeStatus;
  analyzeError?: string;
  storageUrl?: string;
}

export interface HealthReportStats {
  totalCount: number;
  totalConsultationCount: number;
  totalAccessCount: number;
}

export interface AnalysisItem {
  id: number;
  overallScore: number;
  lifestyleScore: number;
  nutritionScore: number;
  physicalFitnessScore: number;
  mentalHealthScore: number;
  preventiveScore: number;
  summary: string;
  analyzedAt: string;
  strengths: string[];
  suggestions: unknown[];
}

export interface ConsultationItem {
  id: number;
  sessionId: string;
  totalQuestions: number;
  status: string;
  evaluateStatus?: EvaluateStatus;
  evaluateError?: string;
  overallScore: number | null;
  overallFeedback: string | null;
  createdAt: string;
  completedAt: string | null;
  questions?: unknown[];
  strengths?: string[];
  improvements?: string[];
  referenceAnswers?: unknown[];
}

export interface AnswerItem {
  questionIndex: number;
  question: string;
  category: string;
  userAnswer: string;
  score: number;
  feedback: string;
  referenceAnswer?: string;
  keyPoints?: string[];
  answeredAt: string;
}

export interface HealthReportDetail {
  id: number;
  filename: string;
  fileSize: number;
  contentType: string;
  storageUrl: string;
  uploadedAt: string;
  accessCount: number;
  reportText: string;
  analyzeStatus?: AnalyzeStatus;
  analyzeError?: string;
  analyses: AnalysisItem[];
  consultations: ConsultationItem[];
}

export interface ConsultationDetail extends ConsultationItem {
  evaluateStatus?: EvaluateStatus;
  evaluateError?: string;
  answers: AnswerItem[];
}

export const historyApi = {
  /**
   * 获取所有体检报告列表
   */
  async getHealthReports(): Promise<HealthReportListItem[]> {
    return request.get<HealthReportListItem[]>('/api/healthreports');
  },

  /**
   * 获取体检报告详情
   */
  async getHealthReportDetail(id: number): Promise<HealthReportDetail> {
    return request.get<HealthReportDetail>(`/api/healthreports/${id}/detail`);
  },

  /**
   * 获取问诊详情
   */
  async getConsultationDetail(sessionId: string): Promise<ConsultationDetail> {
    return request.get<ConsultationDetail>(`/api/consultation/sessions/${sessionId}/details`);
  },

  /**
   * 导出健康分析报告PDF
   */
  async exportAnalysisPdf(healthReportId: number): Promise<Blob> {
    const response = await request.getInstance().get(`/api/healthreports/${healthReportId}/export`, {
      responseType: 'blob',
      skipResultTransform: true,
    } as never);
    return response.data;
  },

  /**
   * 导出问诊报告PDF
   */
  async exportConsultationPdf(sessionId: string): Promise<Blob> {
    const response = await request.getInstance().get(`/api/consultation/sessions/${sessionId}/export`, {
      responseType: 'blob',
      skipResultTransform: true,
    } as never);
    return response.data;
  },

  /**
   * 删除体检报告
   */
  async deleteHealthReport(id: number): Promise<void> {
    return request.delete(`/api/healthreports/${id}`);
  },

  /**
   * 删除问诊记录
   */
  async deleteConsultation(sessionId: string): Promise<void> {
    return request.delete(`/api/consultation/sessions/${sessionId}`);
  },

  /**
   * 获取体检报告统计信息
   */
  async getStatistics(): Promise<HealthReportStats> {
    return request.get<HealthReportStats>('/api/healthreports/statistics');
  },

  /**
   * 重新分析体检报告
   */
  async reanalyze(id: number): Promise<void> {
    return request.post(`/api/healthreports/${id}/reanalyze`);
  },
};
