import { useState } from 'react';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { healthReportApi } from '../api/healthreport';
import { getErrorMessage } from '../api/request';
import FileUploadCard from '../components/FileUploadCard';
import PageHeader from '../components/PageHeader';
import ConfirmDialog from '../components/ConfirmDialog';

interface UploadPageProps {
  onUploadComplete: (healthReportId: number) => void;
}

export default function UploadPage({ onUploadComplete }: UploadPageProps) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [duplicateReportId, setDuplicateReportId] = useState<number | null>(null);
  const navigate = useNavigate();

  const handleUpload = async (file: File) => {
    setUploading(true);
    setError('');

    try {
      const data = await healthReportApi.uploadAndAnalyze(file);

      if (!data.storage || !data.storage.healthReportId) {
        throw new Error('上传失败，请重试');
      }

      // Duplicate detected — show warning dialog instead of silently navigating
      if (data.duplicate) {
        setDuplicateReportId(data.storage.healthReportId);
        setUploading(false);
        return;
      }

      onUploadComplete(data.storage.healthReportId);
    } catch (err) {
      setError(getErrorMessage(err));
      setUploading(false);
    }
  };

  const handleViewExistingReport = () => {
    if (duplicateReportId !== null) {
      navigate(`/history/${duplicateReportId}`);
    }
    setDuplicateReportId(null);
  };

  const handleDismissDuplicate = () => {
    setDuplicateReportId(null);
  };

  return (
    <motion.div className="w-full" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <PageHeader
        label="Health Report"
        title="上传报告"
        subtitle="上传 PDF 或 Word 体检报告，AI 将为您生成个性化健康分析方案"
      />
      <div className="bg-white dark:bg-forest-800 rounded-2xl border border-slate-100 dark:border-forest-600 shadow-sm">
        <FileUploadCard
          showHeader={false}
          title="开始您的 AI 健康管理"
          subtitle="上传 PDF 或 Word 体检报告，AI 将为您生成个性化健康分析方案"
          accept=".pdf,.doc,.docx,.txt"
          formatHint="支持 PDF, DOCX, TXT"
          maxSizeHint="最大 10MB"
          uploading={uploading}
          uploadButtonText="开始上传"
          selectButtonText="选择体检报告文件"
          error={error}
          onUpload={handleUpload}
        />
      </div>

      <ConfirmDialog
        open={duplicateReportId !== null}
        title="检测到重复报告"
        message="您上传的报告与已有记录内容相同。是否前往查看已有的分析结果？"
        confirmText="查看已有报告"
        cancelText="返回重新选择"
        confirmVariant="warning"
        onConfirm={handleViewExistingReport}
        onCancel={handleDismissDuplicate}
      />
    </motion.div>
  );
}
