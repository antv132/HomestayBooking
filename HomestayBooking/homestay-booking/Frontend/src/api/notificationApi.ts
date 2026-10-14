import api from './axiosInstance';

export type NotificationType =
  | 'BOOKING_CONFIRMED'
  | 'CONTRACT_GENERATED'
  | 'PAYMENT_CONFIRMED'
  | 'PAYMENT_PENDING_VERIFICATION'
  | 'MAINTENANCE_UPDATED'
  | 'AVAILABILITY_UPDATED'
  | 'HOUSEKEEPING_TASK_UPDATED'
  | 'BOOKING_CANCELLED'
  | 'SYSTEM';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  content: string;
  isRead: boolean;
  relatedEntityId: string | null;
  relatedEntityType: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiResponse<T = null> {
  success: boolean;
  message: string;
  data: T;
}

export interface OutboxEventSummary {
  id: string;
  eventType: string;
  status: 'PENDING' | 'PROCESSED' | 'FAILED';
  retryCount: number;
  errorSummary: string | null;
  createdAt: string | null;
  processedAt: string | null;
}

export const notificationApi = {
  getNotifications: async (params?: {
    page?: number;
    size?: number;
    unreadOnly?: boolean;
  }): Promise<ApiResponse<PageResponse<Notification>>> => {
    const res = await api.get('/api/v1/notifications', { params });
    return res.data;
  },

  getUnreadCount: async (): Promise<ApiResponse<{ count: number }>> => {
    const res = await api.get('/api/v1/notifications/unread-count');
    return res.data;
  },

  markAllRead: async (): Promise<ApiResponse<{ updated: number }>> => {
    const res = await api.patch('/api/v1/notifications/mark-all-read');
    return res.data;
  },

  getNotificationDetail: async (id: string): Promise<ApiResponse<Notification>> => {
    const res = await api.get(`/api/v1/notifications/${id}`);
    return res.data;
  },

  markNotificationRead: async (id: string): Promise<ApiResponse<Notification>> => {
    const res = await api.patch(`/api/v1/notifications/${id}/read`);
    return res.data;
  },
};

export const outboxAdminApi = {
  list: async (params?: {
    status?: string;
    page?: number;
    size?: number;
  }): Promise<ApiResponse<PageResponse<OutboxEventSummary>>> => {
    const res = await api.get('/api/v1/admin/outbox-events', { params });
    return res.data;
  },

  retry: async (id: string): Promise<ApiResponse<OutboxEventSummary>> => {
    const res = await api.post(`/api/v1/admin/outbox-events/${id}/retry`);
    return res.data;
  },
};
