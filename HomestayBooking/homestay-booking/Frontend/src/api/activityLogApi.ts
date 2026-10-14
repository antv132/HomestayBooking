import api from './axiosInstance';

export interface ActivityLogEntry {
  id: string;
  userId: string | null;
  userEmail: string | null;
  userFullName: string;
  action: string;
  entityType: string | null;
  entityId: string | null;
  description: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ActivityLogParams {
  page?: number;
  size?: number;
  from?: string;
  to?: string;
  action?: string;
  userId?: string;
  search?: string;
}

export const activityLogApi = {
  getLogs: async (params?: ActivityLogParams): Promise<{ success: boolean; data: PageResponse<ActivityLogEntry> }> => {
    const res = await api.get('/api/v1/admin/activity-logs', { params });
    return res.data;
  },
};
