import api from './axiosInstance';

export interface MaintenanceTaskSummary {
  id: string;
  roomId: string;
  roomNumber: string;
  propertyId: string;
  propertyName: string;
  customerId: string;
  customerName: string;
  title: string;
  description?: string | null;
  photoUrls: string[];
  status: string;
  assignedEmployeeId?: string | null;
  assignedEmployeeName?: string | null;
  resolutionNote?: string | null;
  assignedAt?: string | null;
  verifiedAt?: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ManagerMaintenanceParams {
  propertyId: string;
  status?: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function fetchManagerMaintenanceTasksV1(
  params: ManagerMaintenanceParams,
): Promise<PageResponse<MaintenanceTaskSummary>> {
  const res = await api.get('/api/v1/manager/maintenance-tasks', { params });
  return res.data.data;
}

export async function assignMaintenanceTaskV1(
  taskId: string,
  assigneeId: string,
): Promise<MaintenanceTaskSummary> {
  const res = await api.patch(`/api/v1/manager/maintenance-tasks/${taskId}/assign`, {
    assigneeId,
  });
  return res.data.data;
}

export async function updateMaintenanceTaskStatusV1(
  taskId: string,
  payload: { status: string; resolutionNote?: string },
): Promise<MaintenanceTaskSummary> {
  const res = await api.patch(`/api/v1/manager/maintenance-tasks/${taskId}/status`, payload);
  return res.data.data;
}
