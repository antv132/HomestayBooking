import { useState } from 'react';
import { Link } from 'react-router-dom';
import PublicLayout from '../../layouts/PublicLayout';

const STORY_IMAGE =
  'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?auto=format&fit=crop&w=1400&q=80';
const HERO_IMAGE =
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=2000&q=80';

const STATS = [
  { value: '2023', label: 'Thành lập' },
  { value: '5+', label: 'Thành phố' },
  { value: '5.000+', label: 'Khách đã lưu trú' },
  { value: '98%', label: 'Hài lòng' },
];

const VALUES = [
  {
    title: 'Tin cậy',
    desc: 'Mỗi property được xác minh. Thông tin phòng và giá luôn khớp thực tế.',
  },
  {
    title: 'Minh bạch',
    desc: 'Không phí ẩn. Cọc 40%, còn lại 60% — hiển thị rõ trước khi đặt.',
  },
  {
    title: 'An toàn',
    desc: 'Hợp đồng PDF sau khi xác nhận cọc. Thanh toán cọc và phần còn lại qua VNPay; có thể thu phần còn lại bằng tiền mặt tại quầy khi check-in.',
  },
  {
    title: 'Hỗ trợ',
    desc: 'Khiếu nại và tranh chấp được xử lý theo quy trình, trong khung thời gian rõ ràng.',
  },
];

const CONTACT_ROWS = [
  { label: 'Địa chỉ', value: '125 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh' },
  { label: 'Email', value: 'support@homestay-resort.vn' },
  { label: 'Điện thoại', value: '+84 28 1234 5678' },
  { label: 'Giờ làm việc', value: 'Thứ 2 – Thứ 6: 8:00 – 17:30 (ICT)' },
];

export default function AboutPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  function validate() {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = 'Vui lòng nhập họ tên.';
    if (!email.trim()) e.email = 'Vui lòng nhập email.';
    else if (!/\S+@\S+\.\S+/.test(email)) e.email = 'Email không hợp lệ.';
    if (!subject.trim()) e.subject = 'Vui lòng nhập tiêu đề.';
    if (!message.trim()) e.message = 'Vui lòng nhập nội dung.';
    return e;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setSubmitted(true);
    }, 1000);
  }

  function FieldError({ field }: { field: string }) {
    return errors[field] ? (
      <p className="caption mt-1" style={{ color: 'var(--error)' }}>
        {errors[field]}
      </p>
    ) : null;
  }

  return (
    <PublicLayout>
      {/* Hero — brand + one message + atmosphere */}
      <section className="about-hero" aria-label="Giới thiệu">
        <div className="about-hero__media" aria-hidden="true">
          <img src={HERO_IMAGE} alt="" className="about-hero__img" />
          <div className="about-hero__shade" />
        </div>
        <div className="about-hero__content">
          <p className="about-hero__brand">
            Homestay<span>&</span>Resort
          </p>
          <h1 className="about-hero__title">Kết nối bạn với chỗ nghỉ đúng ý</h1>
          <p className="about-hero__sub">
            Nền tảng đặt homestay &amp; resort tại Việt Nam — giá rõ ràng, hợp đồng số, thanh toán an toàn.
          </p>
          <div className="about-hero__actions">
            <Link to="/rooms" className="btn-primary" style={{ height: 44, padding: '0 22px' }}>
              Xem phòng
            </Link>
            <a href="#contact" className="btn-outline" style={{ height: 44, padding: '0 22px', background: 'rgba(255,255,255,0.12)', borderColor: 'rgba(255,255,255,0.35)', color: '#fff' }}>
              Liên hệ
            </a>
          </div>
        </div>
      </section>

      {/* Stats strip — one row, no cards */}
      <section style={{ background: 'var(--surface-card)', borderBottom: '1px solid var(--hairline)' }}>
        <div className="container-wide about-stats">
          {STATS.map((s) => (
            <div key={s.label} className="about-stat">
              <div className="about-stat__value">{s.value}</div>
              <div className="about-stat__label">{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Story */}
      <section className="section-pad" style={{ background: 'var(--canvas)' }}>
        <div className="container-wide">
          <div className="about-story-grid">
            <div>
              <p className="label-sm" style={{ color: 'var(--primary)', marginBottom: 10, letterSpacing: '0.08em' }}>
                CÂU CHUYỆN
              </p>
              <h2 className="display-md font-display" style={{ color: 'var(--ink)', marginBottom: 20, lineHeight: 1.2 }}>
                Sinh ra từ trải nghiệm đặt phòng khó khăn
              </h2>
              <p className="body-lg" style={{ color: 'var(--body)', marginBottom: 16, lineHeight: 1.7 }}>
                Homestay&amp;Resort ra mắt năm 2023 sau khi chúng tôi gặp phải listing phân tán,
                giá không rõ ràng và không có hợp đồng số khi đặt chỗ nghỉ tại Việt Nam.
              </p>
              <p className="body-lg" style={{ color: 'var(--body)', marginBottom: 0, lineHeight: 1.7 }}>
                Nay nền tảng phục vụ khách và chủ property đã xác minh tại Đà Nẵng, Đà Lạt, Hội An,
                Phú Quốc và Nha Trang — từ tìm phòng, đặt cọc đến hợp đồng PDF trong một luồng.
              </p>
            </div>
            <div>
              <img
                src={STORY_IMAGE}
                alt="Homestay ven biển Việt Nam"
                style={{
                  width: '100%',
                  height: 'min(440px, 55vw)',
                  objectFit: 'cover',
                  borderRadius: 16,
                  display: 'block',
                }}
              />
            </div>
          </div>
        </div>
      </section>

      {/* Values */}
      <section className="section-pad-sm" style={{ background: 'var(--surface-bone)' }}>
        <div className="container-wide">
          <div style={{ maxWidth: 560, marginBottom: 40 }}>
            <p className="label-sm" style={{ color: 'var(--primary)', marginBottom: 10, letterSpacing: '0.08em' }}>
              GIÁ TRỊ
            </p>
            <h2 className="heading-lg font-display" style={{ color: 'var(--ink)', marginBottom: 8 }}>
              Cách chúng tôi vận hành
            </h2>
            <p className="body-md" style={{ color: 'var(--charcoal)' }}>
              Mỗi quyết định sản phẩm bám theo bốn nguyên tắc dưới đây.
            </p>
          </div>
          <div className="about-values-grid">
            {VALUES.map((v, i) => (
              <div key={v.title} className="about-value-item">
                <span className="about-value-num">{String(i + 1).padStart(2, '0')}</span>
                <h3 className="heading-sm" style={{ color: 'var(--ink)', marginBottom: 8 }}>
                  {v.title}
                </h3>
                <p className="body-sm" style={{ color: 'var(--charcoal)', lineHeight: 1.65, margin: 0 }}>
                  {v.desc}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Contact */}
      <section id="contact" className="section-pad" style={{ background: 'var(--canvas)', scrollMarginTop: 80 }}>
        <div className="container-wide">
          <div className="about-contact-grid">
            <div>
              <p className="label-sm" style={{ color: 'var(--primary)', marginBottom: 10, letterSpacing: '0.08em' }}>
                LIÊN HỆ
              </p>
              <h2 className="display-md font-display" style={{ color: 'var(--ink)', marginBottom: 14 }}>
                Chúng tôi sẵn sàng hỗ trợ
              </h2>
              <p className="body-lg" style={{ color: 'var(--body)', marginBottom: 32, lineHeight: 1.7 }}>
                Câu hỏi, khiếu nại hoặc hợp tác — đội ngũ phản hồi trong giờ làm việc, thường trong 24 giờ.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
                {CONTACT_ROWS.map((c) => (
                  <div key={c.label}>
                    <p className="label-sm" style={{ color: 'var(--ash)', marginBottom: 4 }}>
                      {c.label}
                    </p>
                    <p className="body-md" style={{ color: 'var(--ink)', margin: 0, fontWeight: 500 }}>
                      {c.value}
                    </p>
                  </div>
                ))}
              </div>
            </div>

            <div>
              {submitted ? (
                <div
                  style={{
                    background: 'var(--surface-card)',
                    border: '1px solid var(--hairline)',
                    borderRadius: 16,
                    padding: 40,
                    textAlign: 'center',
                    minHeight: 380,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <div
                    style={{
                      width: 48,
                      height: 48,
                      borderRadius: '50%',
                      background: 'var(--primary-light)',
                      color: 'var(--primary)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 22,
                      fontWeight: 700,
                      marginBottom: 16,
                    }}
                  >
                    ✓
                  </div>
                  <h3 className="heading-md" style={{ color: 'var(--ink)', marginBottom: 8 }}>
                    Đã gửi thành công
                  </h3>
                  <p className="body-md" style={{ color: 'var(--charcoal)', marginBottom: 24, maxWidth: 320 }}>
                    Cảm ơn bạn đã liên hệ. Chúng tôi sẽ phản hồi trong 24 giờ làm việc.
                  </p>
                  <button
                    type="button"
                    className="btn-outline"
                    onClick={() => {
                      setSubmitted(false);
                      setName('');
                      setEmail('');
                      setSubject('');
                      setMessage('');
                    }}
                  >
                    Gửi tin nhắn khác
                  </button>
                </div>
              ) : (
                <div
                  style={{
                    background: 'var(--surface-card)',
                    border: '1px solid var(--hairline)',
                    borderRadius: 16,
                    padding: '28px 28px 32px',
                  }}
                >
                  <h3 className="heading-sm" style={{ color: 'var(--ink)', marginBottom: 20 }}>
                    Gửi tin nhắn
                  </h3>
                  <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                    <div>
                      <label className="form-label" htmlFor="contact-name">
                        Họ và tên
                      </label>
                      <input
                        id="contact-name"
                        type="text"
                        className="input"
                        placeholder="Nguyễn Văn A"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        style={{ borderRadius: 10 }}
                      />
                      <FieldError field="name" />
                    </div>
                    <div>
                      <label className="form-label" htmlFor="contact-email">
                        Email
                      </label>
                      <input
                        id="contact-email"
                        type="email"
                        className="input"
                        placeholder="you@example.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        style={{ borderRadius: 10 }}
                      />
                      <FieldError field="email" />
                    </div>
                    <div>
                      <label className="form-label" htmlFor="contact-subject">
                        Tiêu đề
                      </label>
                      <input
                        id="contact-subject"
                        type="text"
                        className="input"
                        placeholder="Nội dung cần hỗ trợ"
                        value={subject}
                        onChange={(e) => setSubject(e.target.value)}
                        maxLength={200}
                        style={{ borderRadius: 10 }}
                      />
                      <FieldError field="subject" />
                    </div>
                    <div>
                      <label className="form-label" htmlFor="contact-message">
                        Nội dung
                      </label>
                      <textarea
                        id="contact-message"
                        className="textarea"
                        rows={5}
                        placeholder="Mô tả ngắn gọn vấn đề của bạn…"
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                        style={{ borderRadius: 10 }}
                      />
                      <FieldError field="message" />
                    </div>
                    <button
                      id="contact-submit"
                      type="submit"
                      className="btn-primary"
                      style={{ height: 48, fontSize: 15, width: '100%' }}
                      disabled={loading}
                    >
                      {loading ? 'Đang gửi…' : 'Gửi tin nhắn'}
                    </button>
                  </form>
                </div>
              )}
            </div>
          </div>
        </div>
      </section>
    </PublicLayout>
  );
}
