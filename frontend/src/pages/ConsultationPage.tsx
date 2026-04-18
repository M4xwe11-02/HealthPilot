import {useEffect, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {consultationApi} from '../api/consultation';
import ConfirmDialog from '../components/ConfirmDialog';
import ConsultationConfigPanel from '../components/ConsultationConfigPanel';
import ConsultationChatPanel from '../components/ConsultationChatPanel';
import type {ConsultationQuestion, ConsultationSession} from '../types/consultation';
import PageHeader from '../components/PageHeader';

type ConsultationStage = 'config' | 'active';

interface Message {
  type: 'consultant' | 'user';
  content: string;
  category?: string;
  questionIndex?: number;
}

interface ConsultationProps {
  reportText: string;
  healthReportId?: number;
  onBack: () => void;
  onConsultationComplete: () => void;
}

export default function Consultation({ reportText, healthReportId, onBack, onConsultationComplete }: ConsultationProps) {
  const [stage, setStage] = useState<ConsultationStage>('config');
  const [questionCount, setQuestionCount] = useState(8);
  const [session, setSession] = useState<ConsultationSession | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState<ConsultationQuestion | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [answer, setAnswer] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [checkingUnfinished, setCheckingUnfinished] = useState(false);
  const [unfinishedSession, setUnfinishedSession] = useState<ConsultationSession | null>(null);
  const [showCompleteConfirm, setShowCompleteConfirm] = useState(false);
  const [forceCreateNew, setForceCreateNew] = useState(false);

  // 检查是否有未完成的问诊（组件挂载时和健康报告ID变化时）
  useEffect(() => {
    if (healthReportId) {
      checkUnfinishedSession();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [healthReportId]);

  const checkUnfinishedSession = async () => {
    if (!healthReportId) return;

    setCheckingUnfinished(true);
    try {
      const foundSession = await consultationApi.findUnfinishedSession(healthReportId);
      if (foundSession) {
        setUnfinishedSession(foundSession);
      }
    } catch (err) {
      console.error('检查未完成问诊失败', err);
    } finally {
      setCheckingUnfinished(false);
    }
  };

  const handleContinueUnfinished = () => {
    if (!unfinishedSession) return;
    setForceCreateNew(false);  // 重置强制创建标志
    restoreSession(unfinishedSession);
    setUnfinishedSession(null);
  };

    const handleStartNew = () => {
    setUnfinishedSession(null);
    setForceCreateNew(true);  // 标记需要强制创建新会话
  };

    const restoreSession = (sessionToRestore: ConsultationSession) => {
    setSession(sessionToRestore);

        // 恢复当前问题
    const currentQ = sessionToRestore.questions[sessionToRestore.currentQuestionIndex];
    if (currentQ) {
      setCurrentQuestion(currentQ);

        // 如果当前问题已有答案，显示在输入框中
      if (currentQ.userAnswer) {
        setAnswer(currentQ.userAnswer);
      }

        // 恢复消息历史
      const restoredMessages: Message[] = [];
      for (let i = 0; i <= sessionToRestore.currentQuestionIndex; i++) {
        const q = sessionToRestore.questions[i];
        restoredMessages.push({
          type: 'consultant',
          content: q.question,
          category: q.category,
          questionIndex: i
        });
        if (q.userAnswer) {
          restoredMessages.push({
            type: 'user',
            content: q.userAnswer
          });
        }
      }
      setMessages(restoredMessages);
    }

        setStage('active');
  };

    const startInterview = async () => {
    setIsCreating(true);
    setError('');

        try {
      // 创建新问诊（如果 forceCreateNew 为 true，则强制创建新会话）
      const newSession = await consultationApi.createSession({
        reportText,
        questionCount,
        healthReportId,
        forceCreate: forceCreateNew
      });

            // 重置强制创建标志
      setForceCreateNew(false);

            // 如果返回的是未完成的会话（currentQuestionIndex > 0 或已有答案），恢复它
            const hasProgress = newSession.currentQuestionIndex > 0 ||
                          newSession.questions.some(q => q.userAnswer) ||
                          newSession.status === 'IN_PROGRESS';

            if (hasProgress) {
        // 这是恢复的会话
        restoreSession(newSession);
      } else {
        // 全新的会话
        setSession(newSession);

                if (newSession.questions.length > 0) {
          const firstQuestion = newSession.questions[0];
          setCurrentQuestion(firstQuestion);
          setMessages([{
            type: 'consultant',
            content: firstQuestion.question,
            category: firstQuestion.category,
            questionIndex: 0
          }]);
        }

                setStage('active');
      }
    } catch (err) {
      setError('创建问诊失败，请重试');
      console.error(err);
      setForceCreateNew(false);  // 出错时也重置标志
    } finally {
      setIsCreating(false);
    }
  };

    const handleSubmitAnswer = async () => {
    if (!answer.trim() || !session || !currentQuestion) return;

    setIsSubmitting(true);

    const userMessage: Message = {
      type: 'user',
      content: answer
    };
    setMessages(prev => [...prev, userMessage]);

    try {
      const response = await consultationApi.submitAnswer({
        sessionId: session.sessionId,
        questionIndex: currentQuestion.questionIndex,
        answer: answer.trim()
      });

      setAnswer('');

      if (response.hasNextQuestion && response.nextQuestion) {
        setCurrentQuestion(response.nextQuestion);
        setMessages(prev => [...prev, {
          type: 'consultant',
          content: response.nextQuestion!.question,
          category: response.nextQuestion!.category,
          questionIndex: response.nextQuestion!.questionIndex
        }]);
      } else {
        // 问诊已完成，评估将在后台进行，跳转到问诊记录页
        onConsultationComplete();
      }
    } catch (err) {
      setError('提交回答失败，请重试');
      console.error(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCompleteEarly = async () => {
    if (!session) return;

    setIsSubmitting(true);
    try {
      await consultationApi.completeConsultation(session.sessionId);
      setShowCompleteConfirm(false);
      // 问诊已完成，评估将在后台进行，跳转到问诊记录页
      onConsultationComplete();
    } catch (err) {
      setError('结束问诊失败，请重试');
      console.error(err);
    } finally {
      setIsSubmitting(false);
    }
  };

    // 配置界面
  const renderConfig = () => {
    return (
      <ConsultationConfigPanel
        questionCount={questionCount}
        onQuestionCountChange={setQuestionCount}
        onStart={startInterview}
        isCreating={isCreating}
        checkingUnfinished={checkingUnfinished}
        unfinishedSession={unfinishedSession}
        onContinueUnfinished={handleContinueUnfinished}
        onStartNew={handleStartNew}
        reportText={reportText}
        onBack={onBack}
        error={error}
      />
    );
  };

    // 问诊对话界面
  const renderInterview = () => {
    if (!session || !currentQuestion) return null;

    return (
      <ConsultationChatPanel
        session={session}
        currentQuestion={currentQuestion}
        messages={messages}
        answer={answer}
        onAnswerChange={setAnswer}
        onSubmit={handleSubmitAnswer}
        onCompleteEarly={handleCompleteEarly}
        isSubmitting={isSubmitting}
        showCompleteConfirm={showCompleteConfirm}
        onShowCompleteConfirm={setShowCompleteConfirm}
      />
    );
  };

  const stageSubtitles = {
    config: '配置您的问诊参数',
    active: '如实回答每个问题，帮助AI全面了解您的健康状况'
  };

    return (
    <div>
      {/* 页面头部 */}
      <PageHeader
        label="AI Consultation"
        title="AI 健康问诊"
        subtitle={stageSubtitles[stage]}
      />

        <AnimatePresence mode="wait" initial={false}>
        {stage === 'config' && (
          <motion.div
            key="config"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.3 }}
          >
            {renderConfig()}
          </motion.div>
        )}
        {stage === 'active' && (
          <motion.div
            key="active"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
          >
            {renderInterview()}
          </motion.div>
        )}
      </AnimatePresence>

        {/* 提前交卷确认对话框 */}
      <ConfirmDialog
        open={showCompleteConfirm}
        title="提前结束问诊"
        message="确定要提前结束本次AI问诊吗？未回答的问题将视为跳过。"
        confirmText="确定结束"
        cancelText="取消"
        confirmVariant="warning"
        loading={isSubmitting}
        onConfirm={handleCompleteEarly}
        onCancel={() => setShowCompleteConfirm(false)}
      />
    </div>
  );
}
