import api from './axiosInstance';

export interface SystemSettings {
  depositPercentage: number;
  cancelTimeoutHours: number;
  systemName: string;
  supportEmail: string;
  bankAccountNumber: string;
  bankAccountName: string;
  bankName: string;
  updatedAt: string | null;
}

export interface UpdateSystemSettingsPayload {
  depositPercentage?: number;
  cancelTimeoutHours?: number;
  systemName?: string;
  supportEmail?: string;
  bankAccountNumber?: string;
  bankAccountName?: string;
  bankName?: string;
}

export const settingsApi = {
  getSettings: async (): Promise<{ success: boolean; data: SystemSettings }> => {
    const res = await api.get('/api/v1/admin/settings');
    return res.data;
  },

  updateSettings: async (payload: UpdateSystemSettingsPayload): Promise<{ success: boolean; message: string; data: SystemSettings }> => {
    const res = await api.put('/api/v1/admin/settings', payload);
    return res.data;
  },
};
