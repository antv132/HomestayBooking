import { useState, useEffect, useCallback } from 'react';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { Drawer, StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchManagerDamageReportsV1,
  fetchManagerDamageReportDetailV1,
  approveDamageReportV1,
  rejectDamageReportV1,
  type DamageReportSummary,
  type DamageReportDetail,
} from '../../api/managerDamageApi';
import { managerApi } from '../../api/managerApi';
import type { AssignedProperty } from '../../api/reportApi';

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  DRAFT:            { label: 'Nháp',           variant: 'neutral' },
  PENDING_APPROVAL: { label: 'Chờ duyệt',      variant: 'warning' },
  APPROVED:         { label: 'Đã duyệt',       variant: 'success' },
  DISPUTED:         { label: 'Khiếu nại',      variant: 'warning' },
  PAID:             { label: 'Đã thanh toán',  variant: 'success' },
};

const STATUS_FILTERS = [
  { value: 'PENDING_APPROVAL', label: 'Chờ duyệt' },
  { value: '', label: 'Tất cả' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'DISPUTED', label: 'Khiếu nại' },
  { value: 'PAID', label: 'Đã thanh toán' },
  { value: 'DRAFT', label: 'Nháp' },
];

function formatVnd(v?: number | null): string {
  if (v == null) return '—';
  return `${v.toLocaleString('vi-VN')} ₫`;
}

function formatDateTime(iso?: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function shortId(id: string): string {
  return `BK-${id.slice(0, 8).toUpperCase()}`;
}

function badge(status: string, requiresAdminEscalation?: boolean, approvedByName?: string | null) {
  if (status === 'PENDING_APPROVAL' && requiresAdminEscalation) {
    const escalated = !!approvedByName;
    return (
      <StatusBadge
        status={escalated ? 'Chờ Admin' : 'Cần escalate'}
        variant="info"
      />
    );
  }
  const cfg = STATUS_VI[status] ?? { label: status, variant: 'neutral' as StatusVariant };
  return <StatusBadge status={cfg.label} variant={cfg.variant} />;
}

export default function DamageReportsPage() {
  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState('');
  const [statusFilter, setStatusFilter] = useState('PENDING_APPROVAL');
  const [escalatedOnly, setEscalatedOnly] = useState(false);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const [reports, setReports] = useState<DamageReportSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const [detail, setDetail] = useState<DamageReportDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [approvedAmount, setApprovedAmount] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [drawerSubmitting, setDrawerSubmitting] = useState(false);
  const [drawerError, setDrawerError] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    managerApi.getMyAssignedProperties()
      .then(res => {
        if (res.success && res.data) {
          setProperties(res.data);
          if (res.data.length > 0) {
            setSelectedPropertyId(prev => prev || res.data![0].id);
          }
        }
      })
      .catch(() => setError('Không thể tải danh sách homestay.'));
  }, []);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(search.trim()), 400);
    return () => clearTimeout(t);
  }, [search]);

  const loadReports = useCallback(() => {
    if (!selectedPropertyId) {
      setReports([]);
      return;
    }
    setLoading(true);
    setError(null);
    fetchManagerDamageReportsV1({
      propertyId: selectedPropertyId,
      status: statusFilter || undefined,
      escalated: escalatedOnly || undefined,
      search: debouncedSearch || undefined,
      page: 0,
      size: 100,
    })
      .then(data => setReports(data.content))
      .catch((err: unknown) => {
        const ax = err as { response?: { status?: number; data?: { message?: string } } };
        if (ax?.response?.status === 403) {
          setError('Bạn không có quyền xem homestay này.');
        } else {
          setError(ax?.response?.data?.message ?? 'Không thể tải danh sách báo cáo hư hại.');
        }
        setReports([]);
      })
      .finally(() => setLoading(false));
  }, [selectedPropertyId, statusFilter, escalatedOnly, debouncedSearch]);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  function openDrawer(row: DamageReportSummary) {
    setDetail(null);
    setApprovedAmount('');
    setRejectReason('');
    setDrawerError(null);
    setDetailLoading(true);
    fetchManagerDamageReportDetailV1(row.id)
      .then(data => {
        setDetail(data);
        setApprovedAmount(String(data.totalEstimatedCost ?? ''));
      })
      .catch(() => setDrawerError('Không thể tải chi tiết báo cáo.'))
      .finally(() => setDetailLoading(false));
  }

  async function handleApprove() {
    if (!detail) return;
    setDrawerSubmitting(true);
    setDrawerError(null);
    try {
      const amount = approvedAmount ? Number(approvedAmount) : undefined;
      await approveDamageReportV1(detail.id, { approvedAmount: amount });
      setDetail(null);
      setSuccessMsg(detail.requiresAdminEscalation ? 'Đã chuyển Admin duyệt.' : 'Đã duyệt bồi thường.');
      loadReports();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setDrawerError(ax?.response?.data?.message ?? 'Không thể duyệt báo cáo.');
    } finally {
      setDrawerSubmitting(false);
    }
  }

  async function handleReject() {
    if (!detail) return;
    if (!rejectReason.trim()) {
      setDrawerError('Vui lòng nhập lý do từ chối.');
      return;
    }
    setDrawerSubmitting(true);
    setDrawerError(null);
    try {
      await rejectDamageReportV1(detail.id, { reason: rejectReason.trim() });
      setDetail(null);
      setSuccessMsg('Đã từ chối báo cáo hư hại.');
      loadReports();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setDrawerError(ax?.response?.data?.message ?? 'Không thể từ chối báo cáo.');
    } finally {
      setDrawerSubmitting(false);
    }
  }

  const alreadyEscalated =
    !!detail?.requiresAdminEscalation && !!detail?.approvedByName && detail.status === 'PENDING_APPROVAL';
  const canAct = detail?.status === 'PENDING_APPROVAL' && !alreadyEscalated;
  const isEmpty = !loading && reports.length === 0;
  const evidenceImages = (detail?.attachments ?? []).filter(a => a.type === 'IMAGE' && a.url);

  return (
    <ManagerLayout>
      <div className="space-y-6">
        <div>
          <h1 className="heading-md m-0">Báo cáo hư hại</h1>
          <p className="text-sm text-[#64748B] mt-1 m-0">
            Duyệt bồi thường hư hại phòng theo homestay.
          </p>
        </div>

        {error && <Alert variant="error" message={error} closeable onClose={() => setError(null)} />}
        {successMsg && (
          <Alert variant="success" message={successMsg} closeable onClose={() => setSuccessMsg(null)} />
        )}

        <div className="flex flex-wrap gap-3 items-end bg-white rounded-xl border border-[#E2E8F0] p-4">
          <div className="min-w-[180px] flex-1 sm:max-w-xs">
            <label className="block text-sm font-medium text-[#334155] mb-1">Homestay</label>
            <select
              className="input-field w-full"
              value={selectedPropertyId}
              onChange={e => setSelectedPropertyId(e.target.value)}
            >
              <option value="">— Chọn homestay —</option>
              {properties.map(p => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          </div>
          <div className="min-w-[180px] flex-1 sm:max-w-xs">
            <label className="block text-sm font-medium text-[#334155] mb-1">Tìm phòng / booking</label>
            <input
              type="text"
              className="input-field w-full"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Số phòng hoặc mã booking..."
            />
          </div>
          <div className="flex gap-2 flex-wrap items-center">
            {STATUS_FILTERS.map(f => (
              <button
                key={f.value}
                type="button"
                className={`px-3 py-1.5 text-sm rounded-full border ${
                  statusFilter === f.value
                    ? 'border-[#0F766E] bg-[#0F766E]/10 text-[#0F766E] font-semibold'
                    : 'border-[#E2E8F0] text-[#64748B]'
                }`}
                onClick={() => setStatusFilter(f.value)}
              >
                {f.label}
              </button>
            ))}
            <button
              type="button"
              className={`px-3 py-1.5 text-sm rounded-full border ${
                escalatedOnly
                  ? 'border-[#2563EB] bg-[#3B82F6]/10 text-[#2563EB] font-semibold'
                  : 'border-[#E2E8F0] text-[#64748B]'
              }`}
              onClick={() => setEscalatedOnly(v => !v)}
            >
              Cần Admin duyệt
            </button>
          </div>
        </div>

        {!selectedPropertyId ? (
          <div className="text-center py-16 text-[#64748B]">
            Vui lòng chọn homestay để xem báo cáo hư hại.
          </div>
        ) : loading ? (
          <div className="h-64 bg-[#F1F5F9] rounded-xl animate-pulse" />
        ) : isEmpty ? (
          <div className="text-center py-16 bg-white rounded-xl border border-[#E2E8F0] text-[#64748B]">
            {statusFilter === 'PENDING_APPROVAL' && !escalatedOnly && !debouncedSearch
              ? 'Không có báo cáo hư hại chờ duyệt.'
              : 'Không có báo cáo hư hại phù hợp.'}
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-[#E2E8F0] overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-[#F8FAFC] text-[#64748B] text-left text-[12px] uppercase tracking-wide">
                    <th className="px-4 py-3 font-medium">Phòng</th>
                    <th className="px-4 py-3 font-medium">Booking</th>
                    <th className="px-4 py-3 font-medium">Chi phí ước tính</th>
                    <th className="px-4 py-3 font-medium">Người báo</th>
                    <th className="px-4 py-3 font-medium">Trạng thái</th>
                    <th className="px-4 py-3 font-medium text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {reports.map(row => (
                    <tr
                      key={row.id}
                      className="border-t border-[#F1F5F9] hover:bg-[#F8FAFC] cursor-pointer"
                      onClick={() => openDrawer(row)}
                    >
                      <td className="px-4 py-3 font-medium text-[#1E293B] whitespace-nowrap">
                        {row.roomNumber}
                      </td>
                      <td className="px-4 py-3 text-[#64748B] whitespace-nowrap">{shortId(row.bookingId)}</td>
                      <td className="px-4 py-3 text-[#334155] whitespace-nowrap">
                        {formatVnd(row.totalEstimatedCost)}
                        {row.requiresAdminEscalation && (
                          <span className="ml-2 text-[11px] text-[#2563EB] bg-[#3B82F6]/10 rounded-full px-2 py-0.5">
                            &gt; 5M
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-[#64748B] whitespace-nowrap">
                        {row.inspectorName || '—'}
                      </td>
                      <td className="px-4 py-3">
                        {badge(row.status, row.requiresAdminEscalation, row.approvedByName)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          type="button"
                          className="text-[#0F766E] hover:underline text-sm font-medium"
                          onClick={e => { e.stopPropagation(); openDrawer(row); }}
                        >
                          Xem
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      <Drawer
        isOpen={!!detail || detailLoading}
        onClose={() => {
          setDetail(null);
          setDetailLoading(false);
          setPreviewUrl(null);
        }}
        title={detail ? `Phòng ${detail.roomNumber} — Hư hại` : 'Chi tiết báo cáo'}
        footer={
          detail && canAct ? (
            <div className="flex flex-col gap-2 w-full">
              <button
                type="button"
                className="btn-primary w-full"
                disabled={drawerSubmitting}
                onClick={handleApprove}
              >
                {drawerSubmitting
                  ? 'Đang lưu...'
                  : detail.requiresAdminEscalation ? 'Chuyển Admin duyệt' : 'Duyệt bồi thường'}
              </button>
              <button
                type="button"
                className="btn-outline w-full text-[#DC2626] border-[#DC2626]"
                disabled={drawerSubmitting}
                onClick={handleReject}
              >
                Từ chối
              </button>
            </div>
          ) : undefined
        }
      >
        {detailLoading ? (
          <div className="h-40 bg-[#F1F5F9] rounded-lg animate-pulse" />
        ) : detail ? (
          <div className="space-y-4">
            {drawerError && <Alert variant="error" message={drawerError} />}

            {alreadyEscalated && (
              <Alert
                variant="info"
                message="Báo cáo đã chuyển Admin đồng phê duyệt. Không thể duyệt/từ chối thêm tại đây."
              />
            )}

            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-[#64748B]">Trạng thái</span>
                {badge(detail.status, detail.requiresAdminEscalation, detail.approvedByName)}
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Booking</span>
                <span className="font-medium">{shortId(detail.bookingId)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Người báo</span>
                <span className="font-medium">{detail.inspectorName || '—'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Tạo lúc</span>
                <span>{formatDateTime(detail.createdAt)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Tổng chi phí ước tính</span>
                <span className="font-semibold">{formatVnd(detail.totalEstimatedCost)}</span>
              </div>
              {detail.approvedAmount != null && (
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Số tiền đã duyệt</span>
                  <span className="font-semibold">{formatVnd(detail.approvedAmount)}</span>
                </div>
              )}
              {detail.requiresAdminEscalation && (
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Cần Admin duyệt</span>
                  <span className="text-[#2563EB] font-medium">Có (&gt; 5M)</span>
                </div>
              )}
              {detail.approvedByName && (
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Manager xử lý</span>
                  <span className="font-medium">{detail.approvedByName}</span>
                </div>
              )}
            </div>

            <div>
              <span className="text-[#64748B] text-sm">Hạng mục hư hại</span>
              <div className="mt-2 space-y-2">
                {detail.items.length === 0 ? (
                  <p className="text-sm text-[#94A3B8] m-0">Không có hạng mục.</p>
                ) : (
                  detail.items.map(it => (
                    <div key={it.id} className="border border-[#E2E8F0] rounded-lg p-3">
                      <div className="flex justify-between gap-2">
                        <span className="font-medium text-[#1E293B]">{it.itemName}</span>
                        <span className="text-[#334155] whitespace-nowrap">{formatVnd(it.estimatedCost)}</span>
                      </div>
                      {it.description && (
                        <p className="text-sm text-[#64748B] m-0 mt-1">{it.description}</p>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>

            <div>
              <span className="text-[#64748B] text-sm">Ảnh minh chứng</span>
              {evidenceImages.length === 0 ? (
                <p className="text-sm text-[#94A3B8] m-0 mt-2">Chưa có ảnh đính kèm.</p>
              ) : (
                <div className="mt-2 grid grid-cols-2 gap-2">
                  {evidenceImages.map((att, i) => (
                    <button
                      key={`${att.url}-${i}`}
                      type="button"
                      className="p-0 border border-[#E2E8F0] rounded-lg overflow-hidden bg-[#F8FAFC]"
                      onClick={() => setPreviewUrl(att.url)}
                    >
                      <img
                        src={att.url}
                        alt={`Hư hại ${i + 1}`}
                        className="w-full aspect-[4/3] object-cover"
                      />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {detail.note && (
              <div>
                <span className="text-[#64748B] text-sm">Ghi chú</span>
                <p className="m-0 mt-1 text-[#334155] whitespace-pre-wrap">{detail.note}</p>
              </div>
            )}

            {canAct && (
              <>
                {!detail.requiresAdminEscalation && (
                  <div>
                    <label className="block text-sm font-medium text-[#334155] mb-1">
                      Số tiền duyệt (₫)
                    </label>
                    <input
                      type="number"
                      min="0"
                      className="input-field w-full"
                      value={approvedAmount}
                      onChange={e => setApprovedAmount(e.target.value)}
                      placeholder="Mặc định theo tổng chi phí ước tính"
                    />
                  </div>
                )}
                <div>
                  <label className="block text-sm font-medium text-[#334155] mb-1">
                    Lý do từ chối (nếu từ chối)
                  </label>
                  <textarea
                    className="input-field w-full min-h-[80px]"
                    value={rejectReason}
                    onChange={e => setRejectReason(e.target.value)}
                    placeholder="Nhập lý do trả lại báo cáo..."
                  />
                </div>
              </>
            )}
          </div>
        ) : (
          <p className="text-sm text-[#64748B]">Không có dữ liệu.</p>
        )}
      </Drawer>

      {previewUrl && (
        <div
          className="fixed inset-0 z-[60] bg-black/70 flex items-center justify-center p-4"
          onClick={() => setPreviewUrl(null)}
          role="presentation"
        >
          <img
            src={previewUrl}
            alt="Xem ảnh hư hại"
            className="max-w-full max-h-[90vh] rounded-lg object-contain"
            onClick={e => e.stopPropagation()}
          />
        </div>
      )}
    </ManagerLayout>
  );
}
