import { request } from './request';
import type {
  CreateConsultationRequest,
  CurrentQuestionResponse,
  ConsultationReport,
  ConsultationSession,
  SubmitAnswerRequest,
  SubmitAnswerResponse
} from '../types/consultation';

export const consultationApi = {
  /**
   * 创建问诊会话
   */
  async createSession(req: CreateConsultationRequest): Promise<ConsultationSession> {
    return request.post<ConsultationSession>('/api/consultation/sessions', req, {
      timeout: 180000, // 3分钟超时，AI生成问题需要时间
    });
  },

  /**
   * 获取会话信息
   */
  async getSession(sessionId: string): Promise<ConsultationSession> {
    return request.get<ConsultationSession>(`/api/consultation/sessions/${sessionId}`);
  },

  /**
   * 获取当前问题
   */
  async getCurrentQuestion(sessionId: string): Promise<CurrentQuestionResponse> {
    return request.get<CurrentQuestionResponse>(`/api/consultation/sessions/${sessionId}/question`);
  },

  /**
   * 提交答案
   */
  async submitAnswer(req: SubmitAnswerRequest): Promise<SubmitAnswerResponse> {
    return request.post<SubmitAnswerResponse>(
      `/api/consultation/sessions/${req.sessionId}/answers`,
      { questionIndex: req.questionIndex, answer: req.answer },
      {
        timeout: 180000, // 3分钟超时
      }
    );
  },

  /**
   * 获取问诊报告
   */
  async getReport(sessionId: string): Promise<ConsultationReport> {
    return request.get<ConsultationReport>(`/api/consultation/sessions/${sessionId}/report`, {
      timeout: 180000, // 3分钟超时，AI评估需要时间
    });
  },

  /**
   * 查找未完成的问诊会话
   */
  async findUnfinishedSession(healthReportId: number): Promise<ConsultationSession | null> {
    try {
      return await request.get<ConsultationSession>(`/api/consultation/sessions/unfinished/${healthReportId}`);
    } catch {
      // 如果没有未完成的会话，返回null
      return null;
    }
  },

  /**
   * 暂存答案（不进入下一题）
   */
  async saveAnswer(req: SubmitAnswerRequest): Promise<void> {
    return request.put<void>(
      `/api/consultation/sessions/${req.sessionId}/answers`,
      { questionIndex: req.questionIndex, answer: req.answer }
    );
  },

  /**
   * 提前交卷
   */
  async completeConsultation(sessionId: string): Promise<void> {
    return request.post<void>(`/api/consultation/sessions/${sessionId}/complete`);
  },
};
