// 健康问诊相关类型定义

export interface ConsultationSession {
  sessionId: string;
  reportText: string;
  totalQuestions: number;
  currentQuestionIndex: number;
  questions: ConsultationQuestion[];
  status: 'CREATED' | 'IN_PROGRESS' | 'COMPLETED' | 'EVALUATED';
}

export interface ConsultationQuestion {
  questionIndex: number;
  question: string;
  type: QuestionType;
  category: string;
  userAnswer: string | null;
  score: number | null;
  feedback: string | null;
}

export type QuestionType = 
  | 'LIFESTYLE'
  | 'SYMPTOM'
  | 'MEDICATION'
  | 'MENTAL_HEALTH'
  | 'NUTRITION'
  | 'EXERCISE'
  | 'SLEEP'
  | 'CHRONIC_DISEASE';

export interface CreateConsultationRequest {
  reportText: string;
  questionCount: number;
  healthReportId?: number;
  forceCreate?: boolean;  // 是否强制创建新会话（忽略未完成的会话）
}

export interface SubmitAnswerRequest {
  sessionId: string;
  questionIndex: number;
  answer: string;
}

export interface SubmitAnswerResponse {
  hasNextQuestion: boolean;
  nextQuestion: ConsultationQuestion | null;
  currentIndex: number;
  totalQuestions: number;
}

export interface CurrentQuestionResponse {
  completed: boolean;
  question?: ConsultationQuestion;
  message?: string;
}

export interface ConsultationReport {
  sessionId: string;
  totalQuestions: number;
  overallScore: number;
  categoryScores: CategoryScore[];
  questionDetails: QuestionEvaluation[];
  overallFeedback: string;
  strengths: string[];
  improvements: string[];
  referenceAnswers: ReferenceAnswer[];
}

export interface CategoryScore {
  category: string;
  score: number;
  questionCount: number;
}

export interface QuestionEvaluation {
  questionIndex: number;
  question: string;
  category: string;
  userAnswer: string;
  score: number;
  feedback: string;
}

export interface ReferenceAnswer {
  questionIndex: number;
  question: string;
  referenceAnswer: string;
  keyPoints: string[];
}
