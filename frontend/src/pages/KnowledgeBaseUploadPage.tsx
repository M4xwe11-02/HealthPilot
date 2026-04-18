import { useState } from 'react';
import { motion } from 'framer-motion';
import { knowledgeBaseApi } from '../api/knowledgebase';
import type { UploadKnowledgeBaseResponse } from '../api/knowledgebase';
import FileUploadCard from '../components/FileUploadCard';
import PageHeader from '../components/PageHeader';

interface KnowledgeBaseUploadPageProps {
  onUploadComplete: (result: UploadKnowledgeBaseResponse) => void;
  onBack: () => void;
}

export default function KnowledgeBaseUploadPage({ onUploadComplete, onBack }: KnowledgeBaseUploadPageProps) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const handleUpload = async (file: File, name?: string) => {
    setUploading(true);
    setError('');

    try {
      const data = await knowledgeBaseApi.uploadKnowledgeBase(file, name);
      onUploadComplete(data);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : '上传失败，请重试';
      setError(errorMessage);
      setUploading(false);
    }
  };

  return (
    <motion.div className="w-full" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <PageHeader
        label="Knowledge Base"
        title="上传知识文档"
        subtitle="上传药品说明书、健康指南或医疗文档，AI 将基于内容提供专业解答"
      />
      <div className="bg-white dark:bg-forest-800 rounded-2xl border border-slate-100 dark:border-forest-600 shadow-sm">
        <FileUploadCard
          showHeader={false}
          title="上传健康知识文档"
          subtitle="上传药品说明书、健康指南或医疗文档，AI 将基于内容提供专业解答"
          accept=".pdf,.doc,.docx,.txt,.md"
          formatHint="支持 PDF、DOCX、DOC、TXT、MD"
          maxSizeHint="最大 50MB"
          uploading={uploading}
          uploadButtonText="开始上传"
          selectButtonText="选择文件"
          showNameInput={true}
          nameLabel="文档名称（可选）"
          namePlaceholder="留空则使用文件名"
          error={error}
          onUpload={handleUpload}
          onBack={onBack}
        />
      </div>
    </motion.div>
  );
}
