import api from './axiosInstance';

/** FR-18 Promotion banner (SCR-57/58) — Admin only. */
export interface PromotionItem {
  id: string;
  subtitle: string;
  title: string;
  description?: string;
  ctaText: string;
  ctaUrl: string;
  imageUrl?: string;
  colorTheme: string;
  isActive: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface PromotionPayload {
  subtitle: string;
  title: string;
  description?: string;
  ctaText: string;
  ctaUrl: string;
  imageUrl?: string;
  colorTheme: string;
  isActive: boolean;
  sortOrder: number;
}

const BASE = '/api/v1/admin/promotions';

export const promotionApi = {
  getAll: async (): Promise<PromotionItem[]> => {
    const res = await api.get(BASE, { params: { page: 0, size: 100 } });
    const data = res.data?.data;
    if (Array.isArray(data)) return data;
    return data?.content ?? [];
  },
  create: async (payload: PromotionPayload): Promise<PromotionItem> => {
    const res = await api.post(BASE, payload);
    return res.data.data;
  },
  update: async (id: string, payload: PromotionPayload): Promise<PromotionItem> => {
    const res = await api.put(`${BASE}/${id}`, payload);
    return res.data.data;
  },
  delete: async (id: string): Promise<void> => {
    await api.delete(`${BASE}/${id}`);
  },
  patchActive: async (id: string, isActive: boolean): Promise<PromotionItem> => {
    const res = await api.patch(`${BASE}/${id}/active`, { isActive });
    return res.data.data;
  },
  /** Upload banner image → `/uploads/promotions/...` */
  uploadImage: async (file: File): Promise<string> => {
    const form = new FormData();
    form.append('file', file);
    const res = await api.post(`${BASE}/upload`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data.url as string;
  },
};
