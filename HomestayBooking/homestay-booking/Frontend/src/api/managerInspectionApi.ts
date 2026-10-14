import api from './axiosInstance';

export interface InspectionSummary {
  id: string;
  roomId: string;
  roomNumber: string;
  propertyId: string;
  propertyName: string;
  bookingId: string;
  inspectorId?: string | null;
  inspectorName?: string | null;
  status: string;
  note?: string | null;
  inspectedAt?: string | null;
  createdAt: string;
  inspectionDurationMinutes?: number | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ManagerInspectionParams {
  propertyId: string;
  status?: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function fetchManagerInspectionsV1(
  params: ManagerInspectionParams,
): Promise<PageResponse<InspectionSummary>> {
  const res = await api.get('/api/v1/manager/inspections', { params });
  return res.data.data;
}

/** SCR-42 — Gán / đổi Employee kiểm tra phòng. */
export async function assignInspectorV1(
  inspectionId: string,
  employeeId: string,
): Promise<InspectionSummary> {
  const res = await api.patch(`/api/v1/manager/inspections/${inspectionId}/assign`, {
    employeeId,
  });
  return res.data.data;
}

export interface InspectionChecklistAnswer {
  id: string;
  itemId: string;
  code: string;
  label: string;
  icon?: string | null;
  passed: boolean;
}

export async function fetchInspectionChecklistAnswersV1(
  inspectionId: string,
): Promise<InspectionChecklistAnswer[]> {
  const res = await api.get(`/api/v1/manager/inspections/${inspectionId}/checklist-answers`);
  return res.data.data ?? [];
}
