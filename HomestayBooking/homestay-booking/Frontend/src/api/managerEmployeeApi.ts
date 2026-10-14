import api from './axiosInstance';

export interface EmployeeSummary {
  id: string;
  fullName: string;
  email: string;
  phone?: string | null;
  avatarUrl?: string | null;
  status: string;
  propertyId?: string;
  propertyName?: string;
  assignedAt?: string;
  assignmentStatus?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ManagerEmployeesParams {
  propertyId: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function fetchManagerEmployeesV1(
  params: ManagerEmployeesParams,
): Promise<PageResponse<EmployeeSummary>> {
  const res = await api.get('/api/v1/manager/employees', { params });
  return res.data.data;
}

export async function fetchUnassignedEmployeesV1(
  params: ManagerEmployeesParams,
): Promise<PageResponse<EmployeeSummary>> {
  const res = await api.get('/api/v1/manager/employees/unassigned', { params });
  return res.data.data;
}

export async function assignEmployeeV1(
  employeeId: string,
  propertyId: string,
): Promise<EmployeeSummary> {
  const res = await api.post('/api/v1/manager/employees/assign', { employeeId, propertyId });
  return res.data.data;
}

export async function createEmployeeV1(payload: {
  fullName: string;
  email: string;
  phone?: string;
  propertyId: string;
}): Promise<EmployeeSummary> {
  const res = await api.post('/api/v1/manager/employees', payload);
  return res.data.data;
}

export async function updateEmployeeV1(
  id: string,
  propertyId: string,
  payload: { fullName: string; phone?: string },
): Promise<EmployeeSummary> {
  const res = await api.put(`/api/v1/manager/employees/${id}`, payload, {
    params: { propertyId },
  });
  return res.data.data;
}

export async function updateEmployeeStatusV1(
  id: string,
  propertyId: string,
  status: 'ACTIVE' | 'SUSPENDED',
): Promise<EmployeeSummary> {
  const res = await api.patch(
    `/api/v1/manager/employees/${id}/status`,
    { status },
    { params: { propertyId } },
  );
  return res.data.data;
}
