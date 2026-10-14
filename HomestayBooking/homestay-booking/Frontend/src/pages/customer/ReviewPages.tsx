// ─── ReviewPages.tsx — SCR-30, 31 ────────────────────────────────────────────
// Exports: ReviewRatingPage, MyReviewsPage

import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import { bookingApi, BookingDetailResponse } from '../../api/bookingApi';
import { reviewApi, Review } from '../../api/reviewApi';
import Alert from '../../components/ui/Alert';
import Modal from '../../components/ui/Modal';

export const formatBookingId = (uuid: string): string => {
  if (!uuid) return '';
  if (uuid.startsWith('b00') && uuid.length === 36) {
    const match = uuid.match(/^b00([0-9])0000-/);
    if (match) return `B00${match[1]}`;
  }
  const parts = uuid.split('-');
  return parts[0].toUpperCase();
};

// ── SCR-25: Review & Rating Form ─────────────────────────────────────────────
const RATING_LABELS = ['', 'Tệ', 'Tạm', 'Khá', 'Rất tốt', 'Xuất sắc'];

export function ReviewRatingPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const bookingId = params.get('bookingId');
  const reviewId = params.get('reviewId');
  const isEditMode = Boolean(reviewId);

  const [booking, setBooking] = useState<BookingDetailResponse | null>(null);
  const [reviewMeta, setReviewMeta] = useState<Review | null>(null);
  const [rating, setRating] = useState(0);
  const [hovered, setHovered] = useState(0);
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadingSubmit, setLoadingSubmit] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [gateMessage, setGateMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setApiError(null);
      setGateMessage(null);

      if (!bookingId && !reviewId) {
        setApiError('Thiếu mã đơn đặt phòng hoặc mã đánh giá.');
        setLoading(false);
        return;
      }

      try {
        if (reviewId) {
          const res = await reviewApi.getReviewById(reviewId);
          if (cancelled) return;
          if (res.success && res.data) {
            setReviewMeta(res.data);
            setRating(res.data.rating);
            setComment(res.data.comment ?? '');
          } else {
            setApiError(res.message || 'Không tải được thông tin đánh giá.');
          }
        } else if (bookingId) {
          const res = await bookingApi.getMyBookingDetail(bookingId);
          if (cancelled) return;
          if (res.success && res.data) {
            const b = res.data;
            if (b.status !== 'CHECKED_OUT') {
              setGateMessage('Bạn chỉ có thể đánh giá sau khi hoàn tất lưu trú (Đã trả phòng).');
            } else if (b.isReviewed) {
              setGateMessage('Đơn đặt phòng này đã được đánh giá. Bạn có thể xem hoặc sửa trong trang Đánh giá của tôi.');
            } else {
              setBooking(b);
            }
          } else {
            setApiError(res.message || 'Không tải được thông tin đơn đặt phòng.');
          }
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
          setApiError(msg || 'Không tải được dữ liệu. Vui lòng thử lại.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => { cancelled = true; };
  }, [bookingId, reviewId]);

  function validate() {
    const e: Record<string, string> = {};
    if (rating === 0) e.rating = 'Vui lòng chọn số sao đánh giá (1–5).';
    const normalizedComment = comment.trim();
    if (!normalizedComment) {
      e.comment = 'Nội dung đánh giá là bắt buộc.';
    } else if (normalizedComment.length > 1000) {
      e.comment = 'Bình luận không được vượt quá 1000 ký tự.';
    }
    return e;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) {
      setErrors(errs);
      return;
    }
    setErrors({});
    setApiError(null);
    setLoadingSubmit(true);
    const normalizedComment = comment.trim();

    try {
      if (isEditMode && reviewId) {
        const res = await reviewApi.updateReview(reviewId, { rating, comment: normalizedComment });
        if (res.success) {
          navigate('/customer/reviews');
        } else {
          setApiError(res.message || 'Không cập nhật được đánh giá.');
          setLoadingSubmit(false);
        }
      } else if (bookingId) {
        const res = await reviewApi.createReview({ bookingId, rating, comment: normalizedComment });
        if (res.success) {
          navigate('/customer/reviews');
        } else {
          setApiError(res.message || 'Không gửi được đánh giá.');
          setLoadingSubmit(false);
        }
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setApiError(msg || 'Đã xảy ra lỗi. Vui lòng thử lại.');
      setLoadingSubmit(false);
    }
  }

  if (loading) {
    return (
      <CustomerLayout>
        <div style={{ textAlign: 'center', padding: 60 }}>
          <p className="body-md text-charcoal">Đang tải...</p>
        </div>
      </CustomerLayout>
    );
  }

  if (apiError && !booking && !reviewMeta && !gateMessage) {
    return (
      <CustomerLayout>
        <div style={{ maxWidth: 560, margin: '0 auto', padding: '20px 0' }}>
          <div style={{ marginBottom: 20 }}>
            <Alert variant="error" message={apiError} />
          </div>
          <Link to="/customer/bookings" className="btn-primary">Quay lại đơn đặt phòng</Link>
        </div>
      </CustomerLayout>
    );
  }

  if (gateMessage) {
    return (
      <CustomerLayout>
        <div style={{ maxWidth: 560, margin: '0 auto', padding: '20px 0' }}>
          <div style={{ marginBottom: 20 }}>
            <Alert variant="info" message={gateMessage} />
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
            <Link to="/customer/reviews" className="btn-primary" style={{ textDecoration: 'none' }}>Đánh giá của tôi</Link>
            <Link to="/customer/bookings" className="btn-ghost" style={{ textDecoration: 'none' }}>Quay lại đơn đặt phòng</Link>
          </div>
        </div>
      </CustomerLayout>
    );
  }

  const roomNumber = booking?.roomNumber ?? reviewMeta?.roomNumber ?? '';
  const roomType = booking?.roomType ?? '';
  const propertyName = booking?.propertyName ?? reviewMeta?.propertyName ?? '';
  const checkInDate = booking?.checkInDate;
  const checkOutDate = booking?.checkOutDate;

  return (
    <CustomerLayout>
      <div style={{ maxWidth: 560, margin: '0 auto' }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to="/customer/reviews" className="text-primary" style={{ textDecoration: 'none' }}>Đánh giá của tôi</Link>
          <span>›</span>
          <span style={{ fontWeight: 600 }}>{isEditMode ? 'Sửa đánh giá' : 'Viết đánh giá'}</span>
        </div>
        <h1 className="heading-md" style={{ marginBottom: 24 }}>Đánh giá kỳ nghỉ</h1>

        {apiError && (
          <div style={{ marginBottom: 20 }}>
            <Alert variant="error" message={apiError} />
          </div>
        )}

        {(booking || reviewMeta) && (
          <div className="card" style={{ padding: 18, marginBottom: 20 }}>
            <div className="flex items-center gap-12">
              <div style={{ flex: 1 }}>
                <p style={{ fontWeight: 700 }}>
                  {roomNumber}{roomType ? ` — ${roomType}` : ''}
                </p>
                <p className="body-sm text-charcoal">{propertyName}</p>
                {checkInDate && checkOutDate && (
                  <p className="body-sm text-charcoal">{checkInDate} → {checkOutDate}</p>
                )}
              </div>
              {booking && <span className="badge badge-success">Đã trả phòng</span>}
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="card" style={{ padding: 28 }}>
          <div style={{ marginBottom: 24, textAlign: 'center' }}>
            <label className="form-label" style={{ display: 'block', marginBottom: 16 }}>Đánh giá tổng thể *</label>
            <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginBottom: 8 }}>
              {[1, 2, 3, 4, 5].map(star => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setRating(star)}
                  onMouseEnter={() => setHovered(star)}
                  onMouseLeave={() => setHovered(0)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', transition: 'transform 0.1s', transform: (hovered || rating) >= star ? 'scale(1.15)' : 'scale(1)' }}
                >
                  <svg width="40" height="40" viewBox="0 0 24 24" fill={(hovered || rating) >= star ? '#F59E0B' : '#e5e7eb'} style={{ transition: 'fill 0.15s' }}>
                    <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/>
                  </svg>
                </button>
              ))}
            </div>
            {(hovered || rating) > 0 && (
              <p style={{ fontWeight: 700, color: '#F59E0B', fontSize: 15 }}>{RATING_LABELS[hovered || rating]}</p>
            )}
            {errors.rating && <p className="form-error">{errors.rating}</p>}
          </div>

          <div style={{ marginBottom: 24 }}>
            <label className="form-label" htmlFor="comment">Nội dung đánh giá *</label>
            <textarea
              id="comment"
              className={`textarea ${errors.comment ? 'input-error' : ''}`}
              rows={5}
              placeholder="Chia sẻ trải nghiệm của bạn..."
              value={comment}
              onChange={ev => setComment(ev.target.value)}
              maxLength={1000}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              {errors.comment ? <p className="form-error">{errors.comment}</p> : <span />}
              <span className="form-hint" style={{ color: 'var(--charcoal)' }}>
                {comment.length} / 1000
              </span>
            </div>
          </div>

          {!isEditMode && (
            <div style={{ marginBottom: 20 }}>
              <Alert variant="info" message="Mỗi đơn đặt phòng chỉ được đánh giá một lần." />
            </div>
          )}

          <div style={{ display: 'flex', gap: 12 }}>
            <button type="submit" className="btn-primary" disabled={loadingSubmit || rating === 0}>
              {loadingSubmit ? 'Đang gửi...' : isEditMode ? 'Cập nhật đánh giá' : 'Gửi đánh giá'}
            </button>
            <Link to={isEditMode ? '/customer/reviews' : '/customer/bookings'} className="btn-ghost">Hủy</Link>
          </div>
        </form>
      </div>
    </CustomerLayout>
  );
}

// ── SCR-24: My Reviews (view only) ─────────────────────────────────────────────
const STATUS_CONFIG: Record<string, { cls: string; label: string }> = {
  PUBLISHED: { cls: 'badge-success', label: 'Đã công khai' },
  HIDDEN:    { cls: 'badge-neutral', label: 'Đã ẩn' },
};

function StatusBadge({ status }: { status: string }) {
  const v = STATUS_CONFIG[status] ?? { cls: 'badge-neutral', label: status };
  return <span className={`badge ${v.cls}`}>{v.label}</span>;
}

function StarRatingDisplay({ rating }: { rating: number }) {
  return (
    <div style={{ display: 'flex', gap: 2, alignItems: 'center' }}>
      {[1, 2, 3, 4, 5].map(i => (
        <svg key={i} width="16" height="16" viewBox="0 0 24 24" fill={i <= rating ? '#F59E0B' : '#e5e7eb'}>
          <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/>
        </svg>
      ))}
      <span className="body-sm text-charcoal" style={{ marginLeft: 4 }}>{rating}/5</span>
    </div>
  );
}

function formatReviewDate(iso: string): string {
  const d = new Date(iso);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  return `${day}/${month}/${year}`;
}

function getRoomImageUrl(url?: string): string {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  const backendBase = import.meta.env.VITE_API_URL || 'http://localhost:8080';
  return `${backendBase}${url.startsWith('/') ? '' : '/'}${url}`;
}

export function MyReviewsPage() {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState<string | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const loadReviews = async (cancelled?: { current: boolean }) => {
    setLoading(true);
    setApiError(null);
    try {
      const res = await reviewApi.getMyReviews({ page, size: 20 });
      if (cancelled?.current) return;
      if (res.success && res.data) {
        setReviews(res.data.content);
        setTotalPages(res.data.totalPages);
      } else {
        setReviews([]);
        setTotalPages(0);
        setApiError(res.message || 'Không tải được danh sách đánh giá.');
      }
    } catch {
      if (!cancelled?.current) {
        setReviews([]);
        setApiError('Không tải được danh sách đánh giá. Vui lòng thử lại.');
      }
    } finally {
      if (!cancelled?.current) setLoading(false);
    }
  };

  useEffect(() => {
    const cancelled = { current: false };
    loadReviews(cancelled);
    return () => { cancelled.current = true; };
  }, [page]);

  async function handleDelete() {
    if (!deleteId) return;
    setDeleting(true);
    try {
      const res = await reviewApi.deleteReview(deleteId);
      if (res.success) {
        setDeleteId(null);
        if (reviews.length === 1 && page > 0) {
          setPage(p => p - 1);
        } else {
          await loadReviews();
        }
      } else {
        setApiError(res.message || 'Không xóa được đánh giá.');
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setApiError(msg || 'Không xóa được đánh giá. Vui lòng thử lại.');
    } finally {
      setDeleting(false);
      setDeleteId(null);
    }
  }

  return (
    <CustomerLayout>
      <h1 className="heading-md" style={{ marginBottom: 24 }}>Đánh giá của tôi</h1>

      {apiError && (
        <div style={{ marginBottom: 20 }}>
          <Alert variant="error" message={apiError} />
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <p className="body-md text-charcoal">Đang tải...</p>
        </div>
      ) : reviews.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 60 }}>
          <div style={{ fontSize: 40, marginBottom: 12 }}>⭐</div>
          <h3 className="heading-sm" style={{ marginBottom: 8 }}>Bạn chưa viết đánh giá nào.</h3>
          <p className="body-md text-charcoal" style={{ marginBottom: 20 }}>
            Bạn có thể đánh giá sau khi hoàn tất lưu trú (trạng thái Đã trả phòng).
          </p>
          <Link to="/customer/bookings" className="btn-primary">Xem đơn đặt phòng</Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {reviews.map(r => (
            <div key={r.id} className="card" style={{ padding: 24 }}>
              <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
                {r.roomImageUrl && (
                  <img
                    src={getRoomImageUrl(r.roomImageUrl)}
                    alt={r.roomNumber}
                    style={{ width: 72, height: 72, borderRadius: 8, objectFit: 'cover', flexShrink: 0 }}
                  />
                )}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="flex items-start justify-between gap-12" style={{ marginBottom: 8 }}>
                    <div>
                      <p style={{ fontWeight: 700, fontSize: 15, marginBottom: 4 }}>
                        {r.roomNumber} — {r.propertyName}
                      </p>
                      <StarRatingDisplay rating={r.rating} />
                    </div>
                    <div style={{ textAlign: 'right', flexShrink: 0 }}>
                      <p className="body-sm text-charcoal" style={{ marginBottom: 6 }}>
                        {formatReviewDate(r.createdAt)}
                      </p>
                      <StatusBadge status={r.status} />
                    </div>
                  </div>
                  {r.comment?.trim() && (
                    <p
                      className="body-md text-body"
                      style={{ lineHeight: 1.65, wordBreak: 'break-word', whiteSpace: 'pre-wrap', marginBottom: 12 }}
                    >
                      {r.comment}
                    </p>
                  )}
                  <div className="flex items-center justify-between" style={{ flexWrap: 'wrap', gap: 8 }}>
                    <p className="body-sm text-charcoal" style={{ margin: 0 }}>
                      Đơn đặt phòng:{' '}
                      <Link
                        to={`/customer/bookings/${r.bookingId}`}
                        className="text-primary"
                        style={{ textDecoration: 'none', fontWeight: 600 }}
                      >
                        #{formatBookingId(r.bookingId)}
                      </Link>
                    </p>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <Link
                        to={`/customer/reviews/create?reviewId=${r.id}`}
                        className="btn-ghost btn-sm"
                        style={{ color: 'var(--primary)', padding: '4px 8px', textDecoration: 'none' }}
                      >
                        Sửa
                      </Link>
                      <button
                        type="button"
                        className="btn-ghost btn-sm"
                        style={{ color: 'var(--error)', padding: '4px 8px' }}
                        onClick={() => setDeleteId(r.id)}
                      >
                        Xóa
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && totalPages > 1 && (
        <div className="flex items-center justify-center gap-12" style={{ marginTop: 24 }}>
          <button
            type="button"
            className="btn-outline btn-sm"
            disabled={page === 0}
            onClick={() => setPage(p => p - 1)}
          >
            Trước
          </button>
          <span className="body-sm text-charcoal">
            Trang {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            className="btn-outline btn-sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage(p => p + 1)}
          >
            Sau
          </button>
        </div>
      )}

      <Modal
        isOpen={deleteId !== null}
        onClose={() => !deleting && setDeleteId(null)}
        title="Xóa đánh giá"
        size="sm"
        actions={[
          {
            label: 'Hủy',
            onClick: () => setDeleteId(null),
            variant: 'secondary',
          },
          {
            label: deleting ? 'Đang xóa...' : 'Xóa',
            onClick: handleDelete,
            variant: 'primary',
          },
        ]}
      >
        <p className="body-md text-charcoal" style={{ margin: 0 }}>
          Bạn có chắc muốn xóa đánh giá này? Hành động này không thể hoàn tác.
        </p>
      </Modal>
    </CustomerLayout>
  );
}
