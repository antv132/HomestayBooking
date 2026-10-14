import api from './axiosInstance';

export interface Review {
  id: string;
  bookingId: string;
  roomNumber: string;
  propertyName: string;
  rating: number;
  comment: string | null;
  status: string;
  createdAt: string;
  roomImageUrl?: string;
}

export interface ModerationReview {
  id: string;
  bookingId: string;
  roomId: string;
  roomNumber: string;
  propertyName: string;
  propertyId: string;
  customerName: string;
  rating: number;
  comment: string | null;
  status: 'PUBLISHED' | 'HIDDEN' | string;
  createdAt: string;
  moderatedAt?: string | null;
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

export const reviewApi = {
  createReview: async (request: { bookingId: string; rating: number; comment: string }): Promise<ApiResponse<Review>> => {
    const res = await api.post('/api/v1/reviews', request);
    return res.data;
  },

  getReviewById: async (id: string): Promise<ApiResponse<Review>> => {
    const res = await api.get(`/api/v1/reviews/${id}`);
    return res.data;
  },

  getMyReviews: async (params?: { page?: number; size?: number }): Promise<ApiResponse<PageResponse<Review>>> => {
    const res = await api.get('/api/v1/customers/me/reviews', { params });
    return res.data;
  },

  updateReview: async (id: string, request: { rating: number; comment: string }): Promise<ApiResponse<Review>> => {
    const res = await api.put(`/api/v1/reviews/${id}`, request);
    return res.data;
  },

  deleteReview: async (id: string): Promise<ApiResponse<void>> => {
    const res = await api.delete(`/api/v1/reviews/${id}`);
    return res.data;
  },

  /** SCR-65 — Manager property-scoped list */
  getManagerReviews: async (params: {
    propertyId: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<ApiResponse<PageResponse<ModerationReview>>> => {
    const res = await api.get('/api/v1/manager/reviews', { params });
    return res.data;
  },

  moderateManagerReview: async (
    id: string,
    status: 'PUBLISHED' | 'HIDDEN',
  ): Promise<ApiResponse<ModerationReview>> => {
    const res = await api.patch(`/api/v1/manager/reviews/${id}/status`, { status });
    return res.data;
  },

  /** SCR-56 — Admin global list */
  getAdminReviews: async (params?: {
    status?: string;
    page?: number;
    size?: number;
  }): Promise<ApiResponse<PageResponse<ModerationReview>>> => {
    const res = await api.get('/api/v1/admin/reviews', { params });
    return res.data;
  },

  moderateAdminReview: async (
    id: string,
    status: 'PUBLISHED' | 'HIDDEN',
  ): Promise<ApiResponse<ModerationReview>> => {
    const res = await api.patch(`/api/v1/admin/reviews/${id}/status`, { status });
    return res.data;
  },
};
