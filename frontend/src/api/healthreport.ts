import { request } from './request';
import type { UploadResponse } from '../types/healthreport';

export const healthReportApi = {
  /**
   * 上传体检报告并获取健康分析结果
   */
  async uploadAndAnalyze(file: File): Promise<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return request.upload<UploadResponse>('/api/healthreports/upload', formData);
  },

  /**
   * 健康检查
   */
  async healthCheck(): Promise<{ status: string; service: string }> {
    return request.get('/api/healthreports/health');
  },
};
