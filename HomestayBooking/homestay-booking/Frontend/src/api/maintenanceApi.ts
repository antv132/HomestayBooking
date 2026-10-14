import api from './axiosInstance';

export interface MaintenanceTicket {
  id: string;
  customerId: string;
  customerName: string;
  customerEmail: string;
  roomId: string;
  roomNumber: string;
  propertyId: string;
  propertyName: string;
  title: string;
  description: string;
  photoUrls: string[];
  status: string;
  resolutionNote: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

export const maintenanceApi = {
  createTicket: async (formData: FormData): Promise<{ success: boolean; data: MaintenanceTicket }> => {
    const res = await api.post('/api/v1/maintenance-tickets', formData);
    return res.data;
  },

  getCustomerTickets: async (params: { page?: number; size?: number; status?: string }): Promise<{ success: boolean; data: PageResponse<MaintenanceTicket> }> => {
    const res = await api.get('/api/v1/maintenance-tickets/me', { params });
    return res.data;
  },

  getTicketDetail: async (id: string): Promise<{ success: boolean; data: MaintenanceTicket }> => {
    const res = await api.get(`/api/v1/maintenance-tickets/${id}`);
    return res.data;
  },

  updateTicket: async (id: string, formData: FormData): Promise<{ success: boolean; data: MaintenanceTicket }> => {
    const res = await api.put(`/api/v1/maintenance-tickets/${id}`, formData);
    return res.data;
  },

  deleteTicket: async (id: string): Promise<{ success: boolean; message: string }> => {
    const res = await api.delete(`/api/v1/maintenance-tickets/${id}`);
    return res.data;
  },

  /** @deprecated Use managerMaintenanceApi (property-scoped). */
  getAllTickets: async (params?: { status?: string; page?: number; size?: number }): Promise<{ success: boolean; data: PageResponse<MaintenanceTicket> }> => {
    const res = await api.get('/api/maintenance-tickets/all', { params });
    return res.data;
  },

  /** Legacy close-only — prefer managerMaintenanceApi.updateStatus. */
  updateTicketStatus: async (id: string, payload: { status: string; resolutionNote?: string }): Promise<{ success: boolean; data: MaintenanceTicket }> => {
    const res = await api.put(`/api/maintenance-tickets/${id}/status`, payload);
    return res.data;
  },
};
