import { Link, useNavigate } from 'react-router-dom';
import PublicLayout from '../../layouts/PublicLayout';
import SafeImage from '../../components/ui/SafeImage';
import { useAuthStore } from '../../store/authStore';
import { cartItemNights, cartItemTotal, useCartStore, type CartItem } from '../../store/cartStore';

const DEPOSIT_RATE = 0.4;

function formatVnd(amount: number) {
  return `₫${amount.toLocaleString('vi-VN')}`;
}

function formatDate(iso: string) {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

function todayIso() {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

export default function CartPage() {
  const navigate = useNavigate();
  const { isAuthenticated, role } = useAuthStore();
  const items = useCartStore((s) => s.items);
  const removeItem = useCartStore((s) => s.removeItem);
  const clear = useCartStore((s) => s.clear);

  const today = todayIso();
  const isExpired = (item: CartItem) => item.checkIn < today;
  const validItems = items.filter((i) => !isExpired(i));
  const total = validItems.reduce((sum, i) => sum + cartItemTotal(i), 0);
  const deposit = Math.round(total * DEPOSIT_RATE);

  function handleBook(item: CartItem) {
    const target = `/request-booking/${item.roomId}?checkIn=${item.checkIn}&checkOut=${item.checkOut}&guests=${item.guests}`;
    if (!isAuthenticated || role !== 'CUSTOMER') {
      navigate(`/login?redirect=${encodeURIComponent(target)}`);
      return;
    }
    navigate(target);
  }

  return (
    <PublicLayout>
      <div className="container-wide" style={{ paddingTop: 32, paddingBottom: 64 }}>
        <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', marginBottom: 24 }}>
          <div>
            <h1 className="heading-lg font-display" style={{ margin: 0 }}>Giỏ phòng</h1>
            <p className="body-sm text-charcoal" style={{ margin: '6px 0 0' }}>
              {items.length > 0 ? `${items.length} phòng đã chọn` : 'Chưa có phòng nào trong giỏ'}
            </p>
          </div>
          {items.length > 0 && (
            <button type="button" className="btn-ghost btn-sm" onClick={() => { if (window.confirm('Xóa tất cả phòng khỏi giỏ?')) clear(); }}>
              Xóa tất cả
            </button>
          )}
        </div>

        {items.length === 0 ? (
          <div className="card" style={{ padding: '48px 24px', textAlign: 'center' }}>
            <p className="body-lg" style={{ margin: '0 0 8px', fontWeight: 600 }}>Giỏ phòng đang trống</p>
            <p className="body-sm text-charcoal" style={{ margin: '0 0 20px' }}>
              Chọn ngày ở trang chi tiết phòng rồi bấm “Thêm vào giỏ” để so sánh và đặt nhiều phòng.
            </p>
            <Link to="/rooms" className="btn-primary">Xem phòng</Link>
          </div>
        ) : (
          <div className="cart-layout">
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {items.map((item) => {
                const expired = isExpired(item);
                const nights = cartItemNights(item);
                return (
                  <div key={item.id} className="card cart-item" style={{ opacity: expired ? 0.7 : 1 }}>
                    <Link to={`/rooms/${item.roomId}?checkIn=${item.checkIn}&checkOut=${item.checkOut}&guests=${item.guests}`} className="cart-item__media">
                      <SafeImage src={item.imageUrl} alt={`${item.propertyName} – phòng ${item.roomNumber}`} />
                    </Link>
                    <div className="cart-item__body">
                      <p className="label-sm" style={{ margin: 0, color: 'var(--primary)' }}>{item.propertyName}</p>
                      <Link
                        to={`/rooms/${item.roomId}`}
                        className="heading-sm"
                        style={{ color: 'var(--ink)', textDecoration: 'none', display: 'block', margin: '4px 0 8px' }}
                      >
                        Phòng {item.roomNumber} · {item.roomType}
                      </Link>
                      <p className="body-sm text-charcoal" style={{ margin: 0 }}>
                        {formatDate(item.checkIn)} → {formatDate(item.checkOut)} · {nights} đêm · {item.guests} khách
                      </p>
                      <p className="body-sm text-charcoal" style={{ margin: '4px 0 0' }}>
                        {formatVnd(item.pricePerNight)} × {nights} đêm
                      </p>
                      {expired && (
                        <p className="body-sm" style={{ margin: '8px 0 0', color: '#b42318', fontWeight: 600 }}>
                          Ngày check-in đã qua — hãy chọn lại ngày.
                        </p>
                      )}
                    </div>
                    <div className="cart-item__side">
                      <span style={{ fontWeight: 700, fontSize: 18 }}>{formatVnd(cartItemTotal(item))}</span>
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                        <button type="button" className="btn-ghost btn-sm" onClick={() => removeItem(item.id)}>
                          Xóa
                        </button>
                        {expired ? (
                          <Link to={`/rooms/${item.roomId}`} className="btn-outline btn-sm">Chọn lại ngày</Link>
                        ) : (
                          <button type="button" className="btn-primary btn-sm" onClick={() => handleBook(item)}>
                            Đặt phòng này
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <aside className="card cart-summary">
              <h2 className="heading-sm" style={{ margin: '0 0 16px' }}>Tóm tắt</h2>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span className="body-sm text-charcoal">Số phòng có thể đặt</span>
                <span className="body-sm" style={{ fontWeight: 600 }}>{validItems.length}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: 12, marginTop: 4, borderTop: '1px solid var(--hairline)' }}>
                <span className="body-sm" style={{ fontWeight: 600 }}>Tổng cộng</span>
                <span style={{ fontWeight: 700, fontSize: 18 }}>{formatVnd(total)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }}>
                <span className="body-sm text-charcoal">Cọc dự kiến (40%)</span>
                <span className="body-sm text-primary" style={{ fontWeight: 600 }}>{formatVnd(deposit)}</span>
              </div>
              <p className="body-sm text-charcoal" style={{ margin: '16px 0', background: 'var(--surface-bone)', borderRadius: 10, padding: 12 }}>
                Mỗi phòng được đặt và đặt cọc riêng. Sau khi bấm “Đặt phòng này”, phòng được giữ trong thời gian ngắn để bạn thanh toán cọc qua VNPay.
              </p>
              {!isAuthenticated && (
                <p className="body-sm text-charcoal" style={{ margin: '0 0 12px' }}>
                  Bạn sẽ được yêu cầu đăng nhập tài khoản khách hàng khi đặt phòng.
                </p>
              )}
              <Link to="/rooms" className="btn-outline" style={{ width: '100%', justifyContent: 'center' }}>
                Tiếp tục xem phòng
              </Link>
            </aside>
          </div>
        )}
      </div>
    </PublicLayout>
  );
}
