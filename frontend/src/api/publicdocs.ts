import { request } from './request';

export interface PublicDocItem {
  id: number;
  title: string;
  category: string | null;
  source: string | null;
  description: string | null;
  fileSize: number;
  uploadedAt: string;
  analyzeStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  downloadCount: number;
}

export interface PublicDocAnalysis {
  summary: string;
  keyPoints: string[];
  applicablePopulation: string;
  mainRecommendations: string[];
}

export interface PublicDocDetail extends PublicDocItem {
  textPreview: string | null;
  analysis: PublicDocAnalysis | null;
}

export const publicDocsApi = {
  list(): Promise<PublicDocItem[]> {
    return request.get<PublicDocItem[]>('/api/public-docs');
  },

  getDetail(id: number): Promise<PublicDocDetail> {
    return request.get<PublicDocDetail>(`/api/public-docs/${id}/preview`);
  },

  downloadUrl(id: number): string {
    return `/api/public-docs/${id}/download`;
  },

  adminUpload(
    file: File,
    title: string,
    category?: string,
    source?: string,
    description?: string,
  ): Promise<PublicDocItem> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    if (category) formData.append('category', category);
    if (source) formData.append('source', source);
    if (description) formData.append('description', description);
    return request.upload<PublicDocItem>('/api/admin/public-docs/upload', formData);
  },

  adminDelete(id: number): Promise<void> {
    return request.delete<void>(`/api/admin/public-docs/${id}`);
  },
};
