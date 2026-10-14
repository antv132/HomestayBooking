import { useState, useEffect, useCallback, useRef } from 'react';
import AdminLayout from '../../layouts/AdminLayout';
import Alert from '../../components/ui/Alert';
import Modal from '../../components/ui/Modal';
import { DataTable, StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchAdminEmployeesV1,
  fetchAdminUnassignedEmployeesV1,
  assignAdminEmployeeV1,
  createAdminEmployeeV1,
  updateAdminEmployeeV1,
  updateAdminEmployeeStatusV1,
  reassignAdminEmployeeV1,
  type EmployeeSummary,
} from '../../api/adminEmployeeApi';
import { getAdminProperties, type AdminProperty } from '../../api/adminApi';

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  ACTIVE: { label: 'Đang hoạt động', variant: 'success' },
  SUSPENDED: { label: 'Tạm khóa', variant: 'danger' },
  INACTIVE: { label: 'Chưa kích hoạt', variant: 'warning' },
};

const PAGE_SIZE = 20;

export default function AdminEmployeeMgmtPage() {
  const [properties, setProperties] = useState<AdminProperty[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  const [assignOpen, setAssignOpen] = useState(false);
  const [assignSearch, setAssignSearch] = useState('');
  const [unassigned, setUnassigned] = useState<EmployeeSummary[]>([]);
  const [assignError, setAssignError] = useState<string | null>(null);
  const [assignPropertyId, setAssignPropertyId] = useState('');
  const [assigningId, setAssigningId] = useState<string | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState({ fullName: '', email: '', phone: '', propertyId: '' });
  const [createError, setCreateError] = useState<string | null>(null);
  const [createSubmitting, setCreateSubmitting] = useState(false);

  const [editOpen, setEditOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<EmployeeSummary | null>(null);
  const [editForm, setEditForm] = useState({ fullName: '', phone: '' });
  const [editError, setEditError] = useState<string | null>(null);
  const [editSubmitting, setEditSubmitting] = useState(false);

  const [statusTarget, setStatusTarget] = useState<EmployeeSummary | null>(null);
  const [statusSubmitting, setStatusSubmitting] = useState(false);

  const [reassignTarget, setReassignTarget] = useState<EmployeeSummary | null>(null);
  const [reassignPropertyId, setReassignPropertyId] = useState('');
  const [reassignError, setReassignError] = useState<string | null>(null);
  const [reassignSubmitting, setReassignSubmitting] = useState(false);

  useEffect(() => {
    getAdminProperties({ page: 0, size: 100, status: 'ACTIVE' })
      .then(res => {
        if (res.success && res.data?.content) {
          setProperties(res.data.content);
        }
      })
      .catch(() => setError('Không thể tải danh sách homestay.'));
  }, []);

  const loadEmployees = useCallback((pg: number, srch: string, propId: string) => {
    setLoading(true);
    setError(null);
    fetchAdminEmployeesV1({
      propertyId: propId || undefined,
      search: srch || undefined,
      page: pg,
      size: PAGE_SIZE,
    })
      .then(data => {
        setEmployees(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err: unknown) => {
        const ax = err as { response?: { data?: { message?: string } } };
        setError(ax?.response?.data?.message ?? 'Không thể tải danh sách nhân viên.');
        setEmployees([]);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPage(0);
      loadEmployees(0, search, selectedPropertyId);
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [search, selectedPropertyId, loadEmployees]);

  useEffect(() => {
    loadEmployees(page, search, selectedPropertyId);
  }, [page]); // eslint-disable-line react-hooks/exhaustive-deps

  function reload() {
    loadEmployees(page, search, selectedPropertyId);
  }

  function openAssign() {
    setAssignOpen(true);
    setAssignError(null);
    setAssignPropertyId(selectedPropertyId || (properties[0]?.id ?? ''));
    setAssignSearch('');
    fetchAdminUnassignedEmployeesV1({ page: 0, size: 20 })
      .then(data => setUnassigned(data.content))
      .catch(() => setUnassigned([]));
  }

  async function handleAssign(empId: string) {
    if (!assignPropertyId) {
      setAssignError('Chọn homestay để gán.');
      return;
    }
    setAssigningId(empId);
    setAssignError(null);
    try {
      await assignAdminEmployeeV1(empId, assignPropertyId);
      setSuccessMsg('Gán nhân viên thành công.');
      setAssignOpen(false);
      reload();
    } catch (err: unknown) {
      const ax = err as { response?: { status?: number; data?: { message?: string } } };
      setAssignError(
        ax?.response?.status === 409
          ? (ax.response.data?.message ?? 'Nhân viên đã được gán.')
          : (ax?.response?.data?.message ?? 'Gán thất bại.'),
      );
    } finally {
      setAssigningId(null);
    }
  }

  async function handleCreate() {
    if (!createForm.propertyId) {
      setCreateError('Chọn homestay.');
      return;
    }
    setCreateSubmitting(true);
    setCreateError(null);
    try {
      await createAdminEmployeeV1({
        fullName: createForm.fullName.trim(),
        email: createForm.email.trim(),
        phone: createForm.phone.trim() || undefined,
        propertyId: createForm.propertyId,
      });
      setSuccessMsg('Tạo nhân viên thành công.');
      setCreateOpen(false);
      setCreateForm({ fullName: '', email: '', phone: '', propertyId: '' });
      reload();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setCreateError(ax?.response?.data?.message ?? 'Tạo nhân viên thất bại.');
    } finally {
      setCreateSubmitting(false);
    }
  }

  async function handleEdit() {
    if (!editTarget) return;
    setEditSubmitting(true);
    setEditError(null);
    try {
      await updateAdminEmployeeV1(editTarget.id, {
        fullName: editForm.fullName.trim(),
        phone: editForm.phone.trim() || undefined,
      });
      setSuccessMsg('Cập nhật thành công.');
      setEditOpen(false);
      reload();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setEditError(ax?.response?.data?.message ?? 'Cập nhật thất bại.');
    } finally {
      setEditSubmitting(false);
    }
  }

  async function handleStatusConfirm() {
    if (!statusTarget) return;
    const next = statusTarget.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED';
    setStatusSubmitting(true);
    try {
      await updateAdminEmployeeStatusV1(statusTarget.id, next);
      setSuccessMsg(next === 'SUSPENDED' ? 'Đã tạm khóa nhân viên.' : 'Đã kích hoạt nhân viên.');
      setStatusTarget(null);
      reload();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setError(ax?.response?.data?.message ?? 'Đổi trạng thái thất bại.');
      setStatusTarget(null);
    } finally {
      setStatusSubmitting(false);
    }
  }

  async function handleReassign() {
    if (!reassignTarget || !reassignPropertyId) {
      setReassignError('Chọn homestay mới.');
      return;
    }
    setReassignSubmitting(true);
    setReassignError(null);
    try {
      await reassignAdminEmployeeV1(reassignTarget.id, reassignPropertyId);
      setSuccessMsg('Đã chuyển nhân viên sang homestay mới.');
      setReassignTarget(null);
      reload();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setReassignError(ax?.response?.data?.message ?? 'Chuyển homestay thất bại.');
    } finally {
      setReassignSubmitting(false);
    }
  }

  const columns = [
    {
      header: 'Họ tên',
      accessor: (e: EmployeeSummary) => (
        <span className="font-medium text-[#1E293B]">{e.fullName}</span>
      ),
    },
    {
      header: 'Email',
      accessor: (e: EmployeeSummary) => e.email,
    },
    {
      header: 'Homestay',
      accessor: (e: EmployeeSummary) => e.propertyName ?? '—',
    },
    {
      header: 'Trạng thái',
      accessor: (e: EmployeeSummary) => {
        const meta = STATUS_VI[e.status] ?? { label: e.status, variant: 'neutral' as StatusVariant };
        return <StatusBadge status={meta.label} variant={meta.variant} />;
      },
    },
  ];

  return (
    <AdminLayout>
      <div className="space-y-4 p-4 md:p-6">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-xl font-semibold text-[#1E293B]">Nhân viên</h1>
            <p className="text-sm text-[#64748B]">SCR-39 — Quản lý Employee toàn hệ thống</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button type="button" className="btn-secondary" onClick={openAssign}>
              Gán nhân viên
            </button>
            <button
              type="button"
              className="btn-primary"
              onClick={() => {
                setCreateOpen(true);
                setCreateError(null);
                setCreateForm({
                  fullName: '',
                  email: '',
                  phone: '',
                  propertyId: selectedPropertyId || (properties[0]?.id ?? ''),
                });
              }}
            >
              Tạo nhân viên
            </button>
          </div>
        </div>

        {successMsg && (
          <Alert variant="success" message={successMsg} closeable onClose={() => setSuccessMsg(null)} />
        )}
        {error && (
          <Alert variant="error" message={error} closeable onClose={() => setError(null)} />
        )}

        <div className="flex flex-col gap-3 md:flex-row">
          <select
            className="input-field"
            value={selectedPropertyId}
            onChange={e => setSelectedPropertyId(e.target.value)}
          >
            <option value="">Tất cả homestay</option>
            {properties.map(p => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
          <input
            className="input-field flex-1"
            placeholder="Tìm theo tên, email, SĐT…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        {loading ? (
          <p className="text-center py-12 text-[#64748B]">Đang tải…</p>
        ) : employees.length === 0 ? (
          <div className="text-center py-16 bg-white rounded-xl border border-[#E2E8F0]">
            <p className="text-[#64748B] mb-4">Chưa có nhân viên nào được gán.</p>
            <button type="button" className="btn-primary" onClick={openAssign}>
              Gán nhân viên
            </button>
          </div>
        ) : (
          <>
            <DataTable
              columns={columns}
              data={employees}
              keyExtractor={e => e.id}
              actions={[
                {
                  label: 'Sửa',
                  onClick: e => {
                    setEditTarget(e);
                    setEditForm({ fullName: e.fullName, phone: e.phone ?? '' });
                    setEditError(null);
                    setEditOpen(true);
                  },
                },
                {
                  label: 'Trạng thái',
                  onClick: e => setStatusTarget(e),
                },
                {
                  label: 'Chuyển HS',
                  onClick: e => {
                    setReassignTarget(e);
                    setReassignPropertyId('');
                    setReassignError(null);
                  },
                },
              ]}
            />
            {totalPages > 1 && (
              <div className="flex items-center justify-between text-sm text-[#64748B]">
                <span>{totalElements} nhân viên</span>
                <div className="flex gap-2">
                  <button type="button" className="btn-secondary" disabled={page <= 0}
                    onClick={() => setPage(p => p - 1)}>Trước</button>
                  <span>Trang {page + 1}/{totalPages}</span>
                  <button type="button" className="btn-secondary" disabled={page >= totalPages - 1}
                    onClick={() => setPage(p => p + 1)}>Sau</button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <Modal isOpen={assignOpen} onClose={() => setAssignOpen(false)} title="Gán nhân viên" size="lg">
        {assignError && <Alert variant="error" message={assignError} />}
        <select
          className="input-field w-full mb-3"
          value={assignPropertyId}
          onChange={e => setAssignPropertyId(e.target.value)}
        >
          <option value="">Chọn homestay</option>
          {properties.map(p => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>
        <input
          type="search"
          className="input-field w-full mb-4"
          placeholder="Tìm nhân viên chưa gán…"
          value={assignSearch}
          onChange={e => {
            setAssignSearch(e.target.value);
            fetchAdminUnassignedEmployeesV1({
              search: e.target.value || undefined,
              page: 0,
              size: 20,
            })
              .then(data => setUnassigned(data.content))
              .catch(() => setUnassigned([]));
          }}
        />
        <ul className="divide-y divide-[#F1F5F9] max-h-80 overflow-y-auto">
          {unassigned.map(u => (
            <li key={u.id} className="flex items-center justify-between py-3 gap-4">
              <div>
                <p className="font-semibold text-sm m-0">{u.fullName}</p>
                <p className="text-xs text-[#64748B] m-0 mt-0.5">{u.email}</p>
              </div>
              <button
                type="button"
                className="btn-primary text-sm px-3 py-1.5"
                disabled={!!assigningId}
                onClick={() => handleAssign(u.id)}
              >
                {assigningId === u.id ? 'Đang gán…' : 'Gán'}
              </button>
            </li>
          ))}
          {unassigned.length === 0 && (
            <li className="text-center text-[#64748B] py-8">Không có nhân viên chưa gán.</li>
          )}
        </ul>
      </Modal>

      <Modal
        isOpen={createOpen}
        onClose={() => setCreateOpen(false)}
        title="Tạo nhân viên"
        actions={[
          { label: 'Hủy', onClick: () => setCreateOpen(false), variant: 'ghost' },
          {
            label: createSubmitting ? 'Đang lưu…' : 'Tạo',
            onClick: handleCreate,
            variant: 'primary',
          },
        ]}
      >
        {createError && <Alert variant="error" message={createError} />}
        <div className="space-y-4">
          <input className="input-field w-full" placeholder="Họ tên"
            value={createForm.fullName}
            onChange={e => setCreateForm(f => ({ ...f, fullName: e.target.value }))} />
          <input className="input-field w-full" placeholder="Email"
            value={createForm.email}
            onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))} />
          <input className="input-field w-full" placeholder="SĐT"
            value={createForm.phone}
            onChange={e => setCreateForm(f => ({ ...f, phone: e.target.value }))} />
          <select
            className="input-field w-full"
            value={createForm.propertyId}
            onChange={e => setCreateForm(f => ({ ...f, propertyId: e.target.value }))}
          >
            <option value="">Chọn homestay</option>
            {properties.map(p => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
        </div>
      </Modal>

      <Modal
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        title="Sửa nhân viên"
        actions={[
          { label: 'Hủy', onClick: () => setEditOpen(false), variant: 'ghost' },
          {
            label: editSubmitting ? 'Đang lưu…' : 'Lưu',
            onClick: handleEdit,
            variant: 'primary',
          },
        ]}
      >
        {editError && <Alert variant="error" message={editError} />}
        <div className="space-y-4">
          <input className="input-field w-full bg-[#F8FAFC]" value={editTarget?.email ?? ''} readOnly />
          <input className="input-field w-full" placeholder="Họ tên"
            value={editForm.fullName}
            onChange={e => setEditForm(f => ({ ...f, fullName: e.target.value }))} />
          <input className="input-field w-full" placeholder="SĐT"
            value={editForm.phone}
            onChange={e => setEditForm(f => ({ ...f, phone: e.target.value }))} />
        </div>
      </Modal>

      <Modal
        isOpen={!!statusTarget}
        onClose={() => setStatusTarget(null)}
        title={statusTarget?.status === 'SUSPENDED' ? 'Kích hoạt nhân viên?' : 'Tạm khóa nhân viên?'}
        actions={[
          { label: 'Hủy', onClick: () => setStatusTarget(null), variant: 'ghost' },
          {
            label: statusSubmitting ? 'Đang xử lý…' : 'Xác nhận',
            onClick: handleStatusConfirm,
            variant: 'primary',
          },
        ]}
      >
        <p className="text-sm text-[#64748B]">
          {statusTarget?.fullName} ({statusTarget?.email})
        </p>
      </Modal>

      <Modal
        isOpen={!!reassignTarget}
        onClose={() => setReassignTarget(null)}
        title="Chuyển homestay"
        actions={[
          { label: 'Hủy', onClick: () => setReassignTarget(null), variant: 'ghost' },
          {
            label: reassignSubmitting ? 'Đang chuyển…' : 'Xác nhận chuyển',
            onClick: handleReassign,
            variant: 'primary',
          },
        ]}
      >
        {reassignError && <Alert variant="error" message={reassignError} />}
        <p className="text-sm text-amber-800 mb-3">
          Gán hiện tại tại <strong>{reassignTarget?.propertyName}</strong> sẽ chuyển sang INACTIVE.
          Nhiệm vụ đang mở (nếu có) cần xử lý thủ công.
        </p>
        <select
          className="input-field w-full"
          value={reassignPropertyId}
          onChange={e => setReassignPropertyId(e.target.value)}
        >
          <option value="">Chọn homestay mới</option>
          {properties
            .filter(p => p.id !== reassignTarget?.propertyId)
            .map(p => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
        </select>
      </Modal>
    </AdminLayout>
  );
}
