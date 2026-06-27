// 体检报告健康分析响应类型
export interface HealthReportAnalysisResponse {
  overallScore: number;
  scoreDetail: ScoreDetail;
  summary: string;
  strengths: string[];
  suggestions: Suggestion[];
  originalText: string;
}

// 存储信息
export interface StorageInfo {
  fileKey: string;
  fileUrl: string;
  healthReportId?: number;
}

// 上传API完整响应（异步模式：analysis 可能为空）
export interface UploadResponse {
  analysis?: HealthReportAnalysisResponse;
  storage: StorageInfo;
  duplicate?: boolean;
  message?: string;
}

export interface ScoreDetail {
  lifestyleScore: number;
  nutritionScore: number;
  physicalFitnessScore: number;
  mentalHealthScore: number;
  preventiveScore: number;
}

export interface Suggestion {
  category: string;         // 建议类别
  priority: '高' | '中' | '低';
  issue: string;            // 问题描述
  recommendation: string;   // 具体建议
}

export interface ApiError {
  error: string;
  detectedType?: string;
  allowedTypes?: string[];
}
