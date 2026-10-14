import api from './axiosInstance';

export type ComplaintStatus = 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'CLOSED';

export interface ComplaintSummary {
  id: string;
  subject: string;
  customerName: string;
  status: ComplaintStatus;
  createdAt: string;
}

export interface ComplaintDetail {
  id: string;
  subject: string;
  description: string;
  status: ComplaintStatus;
  resolutionNotes: string | null;
  resolvedAt: string | null;
  customer: {
    id: string;
    fullName: string;
    email: string;
  } | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminComplaintItem {
  id: string;
  customerId: string | null;
  customerName: string;
  subject: string;
  description: string;
  status: ComplaintStatus;
  resolution: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export const complaintsApi = {
  // ── Customer V1 endpoints ──
  submitComplaint: async (payload: { subject: string; description: string }): Promise<ApiResponse<ComplaintDetail>> => {
    const res = await api.post('/api/v1/complaints', payload);
    return res.data;
  },

  getMyComplaints: async (params?: { page?: number; size?: number }): Promise<ApiResponse<PageResponse<ComplaintDetail>>> => {
    const res = await api.get('/api/v1/complaints', { params });
    return res.data;
  },

  getMyComplaintDetail: async (id: string): Promise<ApiResponse<ComplaintDetail>> => {
    const res = await api.get(`/api/v1/complaints/${id}`);
    return res.data;
  },

  // ── Legacy customer endpoints (backward compat) ──
  submitComplaintLegacy: async (payload: { subject: string; description: string }): Promise<ApiResponse<ComplaintDetail>> => {
    const res = await api.post('/api/complaints', payload);
    return res.data;
  },

  getMyComplaintsLegacy: async (): Promise<ApiResponse<ComplaintDetail[]>> => {
    const res = await api.get('/api/complaints');
    return res.data;
  },

  // ── Admin V1 endpoints ──
  getAdminComplaints: async (params?: { page?: number; size?: number; status?: string; keyword?: string }): Promise<ApiResponse<PageResponse<AdminComplaintItem>>> => {
    const res = await api.get('/api/v1/admin/complaints', { params });
    return res.data;
  },

  getAdminComplaintDetail: async (id: string): Promise<ApiResponse<ComplaintDetail>> => {
    const res = await api.get(`/api/v1/admin/complaints/${id}`);
    return res.data;
  },

  updateComplaintStatus: async (id: string, payload: { status: ComplaintStatus; resolution?: string; resolutionNotes?: string }): Promise<ApiResponse<AdminComplaintItem>> => {
    const res = await api.put(`/api/v1/admin/complaints/${id}/status`, payload);
    return res.data;
  },

  resolveComplaint: async (id: string, resolution: string): Promise<ApiResponse<AdminComplaintItem>> => {
    const res = await api.patch(`/api/v1/admin/complaints/${id}/resolve`, { resolution });
    return res.data;
  },

  // ── Manager (read-only, kept for compat) ──
  getComplaints: async (params?: { page?: number; size?: number; status?: string; search?: string }): Promise<ApiResponse<PageResponse<ComplaintSummary>>> => {
    const res = await api.get('/api/manager/complaints', { params });
    return res.data;
  },

  getComplaintDetail: async (id: string): Promise<ApiResponse<ComplaintDetail>> => {
    const res = await api.get(`/api/manager/complaints/${id}`);
    return res.data;
  },
};
