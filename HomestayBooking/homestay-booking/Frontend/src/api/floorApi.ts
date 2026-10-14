import api from './axiosInstance';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface FloorSummary {
  id: string;
  propertyId: string;
  floorNumber: number;
  description: string | null;
  roomCount: number;
}

export interface CreateFloorPayload {
  propertyId: string;
  floorNumber: number;
  description?: string;
}

export interface UpdateFloorPayload {
  floorNumber?: number;
  description?: string;
}

// ── API ───────────────────────────────────────────────────────────────────────

export const floorApi = {

  /** SCR-28: List floors by property (v1) */
  getByProperty: async (propertyId: string): Promise<{ success: boolean; data: FloorSummary[] }> => {
    const res = await api.get(`/api/v1/managers/properties/${propertyId}/floors`);
    return res.data;
  },

  /** SCR-28: Add floor to a property (v1) */
  create: async (payload: CreateFloorPayload): Promise<{ success: boolean; data: FloorSummary }> => {
    const res = await api.post(`/api/v1/managers/properties/${payload.propertyId}/floors`, payload);
    return res.data;
  },

  /** SCR-28: Update floor info (v1) */
  update: async (propertyId: string, id: string, payload: UpdateFloorPayload): Promise<{ success: boolean; data: FloorSummary }> => {
    const res = await api.put(`/api/v1/managers/properties/${propertyId}/floors/${id}`, payload);
    return res.data;
  },

  /** SCR-28: Delete floor — only if no rooms exist */
  remove: async (propertyId: string, id: string): Promise<{ success: boolean }> => {
    const res = await api.delete(`/api/v1/managers/properties/${propertyId}/floors/${id}`);
    return res.data;
  },
};
