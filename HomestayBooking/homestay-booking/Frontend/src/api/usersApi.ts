import api from './axiosInstance';

export interface UserProfile {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  avatarUrl: string | null;
  role: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfilePayload {
  fullName?: string;
  phone?: string | null;
  avatarUrl?: string | null;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export async function fetchMyProfile(): Promise<UserProfile> {
  const res = await api.get('/api/v1/users/me');
  return res.data.data;
}

export async function updateMyProfile(payload: UpdateProfilePayload): Promise<UserProfile> {
  const res = await api.put('/api/v1/users/me', payload);
  return res.data.data;
}

export async function changeMyPassword(payload: ChangePasswordPayload): Promise<void> {
  await api.put('/api/v1/users/me/password', payload);
}

// --- MANAGER API ---
export interface CustomerSummaryResponse {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  status: string;
  createdAt: string;
  bookingCount: number;
}

export interface CustomerDetailResponse {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  status: string;
  createdAt: string;
  bookingCount: number;
  recentBookings?: Array<{
    id: string;
    roomNumber: string;
    propertyName: string;
    checkInDate: string;
    checkOutDate: string;
    totalAmount: number;
    status: string;
  }>;
}

export const usersApi = {
  getAllCustomers: async (params: { page?: number; size?: number; status?: string; search?: string }): Promise<{ success: boolean; data: any }> => {
    const res = await api.get('/api/users/customers', { params });
    return res.data;
  },

  getCustomerDetail: async (id: string): Promise<{ success: boolean; data: CustomerDetailResponse }> => {
    const res = await api.get(`/api/users/customers/${id}`);
    return res.data;
  },

  updateCustomerStatus: async (id: string, status: string): Promise<{ success: boolean; message: string }> => {
    const res = await api.patch(`/api/users/customers/${id}/status`, { status });
    return res.data;
  }
};
