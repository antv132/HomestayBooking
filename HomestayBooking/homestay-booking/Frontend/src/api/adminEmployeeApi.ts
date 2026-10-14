import api from './axiosInstance';
import type { EmployeeSummary, PageResponse } from './managerEmployeeApi';

export type { EmployeeSummary, PageResponse };

export interface AdminEmployeesParams {
  propertyId?: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function fetchAdminEmployeesV1(
  params: AdminEmployeesParams,
): Promise<PageResponse<EmployeeSummary>> {
  const res = await api.get('/api/v1/admin/employees', { params });
  return res.data.data;
}

export async function fetchAdminUnassignedEmployeesV1(params: {
  search?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<EmployeeSummary>> {
  const res = await api.get('/api/v1/admin/employees/unassigned', { params });
  return res.data.data;
}

export async function assignAdminEmployeeV1(
  employeeId: string,
  propertyId: string,
): Promise<EmployeeSummary> {
  const res = await api.post('/api/v1/admin/employees/assign', { employeeId, propertyId });
  return res.data.data;
}

export async function createAdminEmployeeV1(payload: {
  fullName: string;
  email: string;
  phone?: string;
  propertyId: string;
}): Promise<EmployeeSummary> {
  const res = await api.post('/api/v1/admin/employees', payload);
  return res.data.data;
}

export async function updateAdminEmployeeV1(
  id: string,
  payload: { fullName: string; phone?: string },
): Promise<EmployeeSummary> {
  const res = await api.put(`/api/v1/admin/employees/${id}`, payload);
  return res.data.data;
}

export async function updateAdminEmployeeStatusV1(
  id: string,
  status: 'ACTIVE' | 'SUSPENDED',
): Promise<EmployeeSummary> {
  const res = await api.patch(`/api/v1/admin/employees/${id}/status`, { status });
  return res.data.data;
}

export async function reassignAdminEmployeeV1(
  id: string,
  propertyId: string,
): Promise<EmployeeSummary> {
  const res = await api.patch(`/api/v1/admin/employees/${id}/reassign`, { propertyId });
  return res.data.data;
}
