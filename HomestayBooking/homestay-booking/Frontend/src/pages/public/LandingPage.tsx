import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PublicLayout from '../../layouts/PublicLayout';
import {
  fetchFeaturedProperties,
  fetchFeaturedRooms,
  fetchPlatformStats,
  fetchPromotions,
  type FeaturedProperty,
  type FeaturedRoom,
  type PlatformStats,
  type Promotion,
  type SearchSuggestion,
} from '../../api/publicApi';
import { formatStatValue, resolveMediaUrl } from '../../utils/mediaUrl';
import SafeImage from '../../components/ui/SafeImage';
import RoomCard from '../../components/ui/RoomCard';
import SearchSuggestionsInput from '../../components/rooms/SearchSuggestionsInput';

/**
 * Fallback hero when API returns zero Active promotions (SC-007).
 * Banner marketing: GET /api/v1/public/promotions — Admin tại /admin/promotions.
 */
const COLOR_GRADIENTS: Record<string, string> = {
  red: 'linear-gradient(135deg, #0F766E 0%, #0D9488 100%)',
  blue: 'linear-gradient(135deg, #1a3c5e 0%, #2d6a9f 100%)',
  green: 'linear-gradient(135deg, #1a5c3a 0%, #2e9c5e 100%)',
  purple: 'linear-gradient(135deg, #4c1d8f 0%, #7c3aed 100%)',
  orange: 'linear-gradient(135deg, #b45309 0%, #f59e0b 100%)',
};

/** Fallback text slides when API returns empty (SC-007). */
const DEFAULT_PROMOTIONS: Promotion[] = [
  {
    id: 'default-1',
    subtitle: 'ƯU ĐÃI',
    title: 'Find Your Zen',
    description: '',
    ctaText: 'Đặt ngay →',
    ctaUrl: '/search',
    colorTheme: 'red',
    isActive: true,
    sortOrder: 0,
  },
];

interface HeroSlide {
  image?: string;
  gradient?: string;
  subtitle?: string;
  title?: string;
  description?: string;
  ctaText?: string;
  ctaUrl?: string;
}

const HOW_IT_WORKS = [
  { step: '01', title: 'Search & Browse', desc: 'Find your perfect room by location, dates, type and capacity with real-time availability.' },
  { step: '02', title: 'Book & Deposit', desc: 'Confirm your booking with a 40% deposit. Receive your contract instantly via email.' },
  { step: '03', title: 'Check In & Enjoy', desc: 'Pay the remaining 60% via VNPay before arrival, or settle in cash at reception during check-in — then enjoy your stay.' },
];

function toRoomCardProps(room: FeaturedRoom) {
  return {
    id: room.id,
    roomNumber: room.roomNumber,
    roomType: room.roomType,
    pricePerNight: room.pricePerNight,
    capacity: room.capacity,
    area: room.area,
    status: room.status,
    propertyName: room.propertyName,
    address: room.propertyName,
    imageUrl: room.primaryImageUrl || '',
  };
}

function PropertyCard({ property }: { property: FeaturedProperty }) {
  const exploreUrl = `/search?location=${encodeURIComponent(property.name)}`;

  return (
    <Link
      to={exploreUrl}
      className="card"
      style={{ overflow: 'hidden', display: 'block', textDecoration: 'none', transition: 'transform 0.2s' }}
      onMouseEnter={(e) => {
        (e.currentTarget as HTMLElement).style.transform = 'translateY(-2px)';
      }}
      onMouseLeave={(e) => {
        (e.currentTarget as HTMLElement).style.transform = '';
      }}
    >
      <SafeImage
        src={property.coverImageUrl}
        alt={property.name}
        style={{ width: '100%', height: 180, objectFit: 'cover' }}
      />
      <div style={{ padding: 20 }}>
        <h3 className="heading-sm" style={{ marginBottom: 4 }}>
          {property.name}
        </h3>
        <p className="body-sm text-charcoal" style={{ marginBottom: 12 }}>
          📍 {property.address}
        </p>
        <div className="flex items-center justify-between">
          <div className="flex gap-3">
            <span className="badge badge-neutral">{property.roomCount} rooms</span>
            <span className="badge badge-success">{property.availableRoomCount} available</span>
          </div>
          <span className="btn-ghost btn-sm" style={{ pointerEvents: 'none' }}>
            Explore →
          </span>
        </div>
      </div>
    </Link>
  );
}

function SectionSkeleton({ count, cols = 4 }: { count: number; cols?: number }) {
  const gridClass =
    cols === 3 ? 'grid grid-cols-1 md:grid-cols-3 gap-6' : 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5';
  return (
    <div className={gridClass}>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="card" style={{ height: 320, background: 'var(--surface-bone)', opacity: 0.6 }} />
      ))}
    </div>
  );
}

export default function LandingPage() {
  const navigate = useNavigate();
  const [search, setSearch] = useState({ location: '', checkIn: '', checkOut: '', guests: '2' });

  const [featuredProperties, setFeaturedProperties] = useState<FeaturedProperty[]>([]);
  const [featuredRooms, setFeaturedRooms] = useState<FeaturedRoom[]>([]);
  const [stats, setStats] = useState<PlatformStats | null>(null);
  const [loadingProperties, setLoadingProperties] = useState(true);
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [heroIndex, setHeroIndex] = useState(0);

  const promoSlides: HeroSlide[] = promotions.map((p) => ({
    image: p.imageUrl?.trim() ? resolveMediaUrl(p.imageUrl) : undefined,
    gradient: COLOR_GRADIENTS[p.colorTheme] ?? COLOR_GRADIENTS.red,
    subtitle: p.subtitle,
    title: p.title,
    description: p.description?.trim() || undefined,
    ctaText: p.ctaText,
    ctaUrl: p.ctaUrl,
  }));

  const heroSlides: HeroSlide[] =
    promoSlides.length > 0
      ? promoSlides
      : DEFAULT_PROMOTIONS.map((p) => ({
          gradient: COLOR_GRADIENTS[p.colorTheme] ?? COLOR_GRADIENTS.red,
          subtitle: p.subtitle,
          title: p.title,
          description: p.description?.trim() || undefined,
          ctaText: p.ctaText,
          ctaUrl: p.ctaUrl,
        }));

  const activeSlide = heroSlides[heroIndex] ?? heroSlides[0];

  useEffect(() => {
    if (heroSlides.length <= 1) return;
    const timer = setInterval(() => {
      setHeroIndex((prev) => (prev + 1) % heroSlides.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [heroSlides.length]);

  useEffect(() => {
    let cancelled = false;

    async function loadLandingData() {
      setLoadError('');
      setLoadingProperties(true);
      setLoadingRooms(true);

      const results = await Promise.allSettled([
        fetchFeaturedProperties(6),
        fetchFeaturedRooms(8),
        fetchPlatformStats(),
        fetchPromotions(),
      ]);

      if (cancelled) return;

      const [propertiesResult, roomsResult, statsResult, promoResult] = results;

      if (promoResult.status === 'fulfilled') {
        const promos = [...promoResult.value].sort((a, b) => a.sortOrder - b.sortOrder);
        setPromotions(promos);
      } else {
        setPromotions([]);
      }

      if (propertiesResult.status === 'fulfilled') {
        setFeaturedProperties(propertiesResult.value);
      } else {
        setFeaturedProperties([]);
      }

      if (roomsResult.status === 'fulfilled') {
        setFeaturedRooms(roomsResult.value);
      } else {
        setFeaturedRooms([]);
      }

      if (statsResult.status === 'fulfilled') {
        setStats(statsResult.value);
      }

      const allFailed = results.every((r) => r.status === 'rejected');
      if (allFailed) {
        setLoadError(
          'Không kết nối được backend (port 8080). Mở IntelliJ → Run HomestayApplication, rồi bấm Thử lại.',
        );
      }

      setLoadingProperties(false);
      setLoadingRooms(false);
    }

    loadLandingData();
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  useEffect(() => {
    if (window.location.hash === '#properties') {
      document.getElementById('properties')?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [loadingProperties]);

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (search.checkIn && search.checkOut && search.checkOut <= search.checkIn) {
      return;
    }
    const params = new URLSearchParams();
    const term = search.location.trim();
    if (term) params.set('location', term);
    if (search.checkIn) params.set('checkIn', search.checkIn);
    if (search.checkOut) params.set('checkOut', search.checkOut);
    const guests = parseInt(search.guests, 10);
    if (guests > 0) params.set('guests', String(guests));
    navigate('/search?' + params.toString());
  }

  function applySuggestion(item: SearchSuggestion) {
    const params = new URLSearchParams();
    if (item.type === 'property') {
      params.set('search', item.label);
      params.set('location', item.label);
    } else {
      params.set('location', item.label);
    }
    if (search.checkIn) params.set('checkIn', search.checkIn);
    if (search.checkOut) params.set('checkOut', search.checkOut);
    const guests = parseInt(search.guests, 10);
    if (guests > 0) params.set('guests', String(guests));
    navigate('/search?' + params.toString());
  }

  const today = new Date().toISOString().slice(0, 10);

  const statItems = stats
    ? [
        { value: formatStatValue(stats.totalProperties), label: 'Properties' },
        { value: formatStatValue(stats.totalRooms), label: 'Rooms' },
        { value: formatStatValue(stats.totalAvailableRooms), label: 'Available Rooms' },
        { value: stats.averageRating > 0 ? stats.averageRating.toFixed(1) : '—', label: 'Average Rating' },
      ]
    : [
        { value: '—', label: 'Properties' },
        { value: '—', label: 'Rooms' },
        { value: '—', label: 'Available Rooms' },
        { value: '—', label: 'Average Rating' },
      ];

  return (
    <PublicLayout>
      {/* Hero — full-bleed lifestyle banner (Klook-style) */}
      <section className="landing-klook-hero" aria-label="Hero">
        <div className="landing-klook-hero__media" aria-hidden="true">
          {heroSlides.map((slide, i) =>
            slide.image ? (
              <img
                key={(slide.image || '') + i}
                src={slide.image}
                alt=""
                className={
                  'landing-klook-hero__img' + (i === heroIndex ? ' landing-klook-hero__img--active' : '')
                }
              />
            ) : (
              <div
                key={'grad-' + i}
                className={
                  'landing-klook-hero__img' + (i === heroIndex ? ' landing-klook-hero__img--active' : '')
                }
                style={{ background: slide.gradient ?? COLOR_GRADIENTS.red }}
              />
            ),
          )}
          <div className="landing-klook-hero__shade" />
        </div>

        <svg className="landing-klook-hero__blob landing-klook-hero__blob--tl" viewBox="0 0 200 160" aria-hidden="true">
          <path fill="#F5C518" d="M40 20c40-30 110-10 140 40 20 35-10 90-55 100-50 12-100-20-110-60C5 70 10 40 40 20z" />
        </svg>
        <svg className="landing-klook-hero__blob landing-klook-hero__blob--br" viewBox="0 0 220 180" aria-hidden="true">
          <path fill="#FF5B00" d="M10 100c20-60 90-90 150-60 45 22 60 80 30 120-28 38-95 40-140 10C15 150-5 130 10 100z" />
        </svg>
        <svg className="landing-klook-hero__squiggle" viewBox="0 0 120 40" aria-hidden="true">
          <path
            d="M4 28c18-22 36 10 54-12 18-22 36 10 54-12"
            fill="none"
            stroke="#2DD4BF"
            strokeWidth="6"
            strokeLinecap="round"
          />
        </svg>

        <div className="landing-klook-hero__content">
          {activeSlide?.subtitle ? (
            <p className="landing-klook-hero__brand">{activeSlide.subtitle}</p>
          ) : (
            <p className="landing-klook-hero__brand">
              Homestay<span>&</span>Resort
            </p>
          )}
          <h1 className="landing-klook-hero__title">
            {activeSlide?.title ? activeSlide.title : 'Find Your Zen'}
          </h1>
          <p className="landing-klook-hero__sub">
            {activeSlide?.description
              ? activeSlide.description
              : 'Homestay & resort giữa thiên nhiên — đặt phòng nhẹ nhàng, minh bạch giá.'}
          </p>

          {activeSlide?.ctaText && activeSlide.ctaUrl && (
            activeSlide.ctaUrl.startsWith('http') ? (
              <a href={activeSlide.ctaUrl} className="landing-klook-hero__cta">
                {activeSlide.ctaText}
              </a>
            ) : (
              <Link to={activeSlide.ctaUrl} className="landing-klook-hero__cta">
                {activeSlide.ctaText}
              </Link>
            )
          )}

          <form onSubmit={handleSearch} className="landing-klook-search" role="search">
            <div className="landing-klook-search__field landing-klook-search__field--location">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" stroke="#94A3B8" strokeWidth="2" />
                <circle cx="12" cy="10" r="3" stroke="#94A3B8" strokeWidth="2" />
              </svg>
              <SearchSuggestionsInput
                value={search.location}
                onChange={(location) => setSearch((p) => ({ ...p, location }))}
                onSelect={applySuggestion}
                placeholder="Bạn muốn đi đâu?"
                ariaLabel="Địa điểm"
                inputClassName=""
                style={{ flex: 1, minWidth: 0 }}
              />
            </div>

            <div className="landing-klook-search__sep" aria-hidden="true" />

            <div className="landing-klook-search__field">
              <input
                type="date"
                aria-label="Ngày nhận phòng"
                value={search.checkIn}
                min={today}
                onChange={(e) => setSearch((p) => ({ ...p, checkIn: e.target.value }))}
              />
            </div>

            <div className="landing-klook-search__sep" aria-hidden="true" />

            <div className="landing-klook-search__field">
              <input
                type="date"
                aria-label="Ngày trả phòng"
                value={search.checkOut}
                min={search.checkIn || today}
                onChange={(e) => setSearch((p) => ({ ...p, checkOut: e.target.value }))}
              />
            </div>

            <div className="landing-klook-search__sep" aria-hidden="true" />

            <div className="landing-klook-search__field landing-klook-search__field--guests">
              <input
                type="number"
                aria-label="Số khách"
                min={1}
                max={20}
                value={search.guests}
                onChange={(e) => setSearch((p) => ({ ...p, guests: e.target.value }))}
              />
              <span>khách</span>
            </div>

            <button type="submit">Tìm phòng</button>
          </form>

          <div className="landing-klook-hero__dots" role="tablist" aria-label="Ảnh homestay">
            {heroSlides.map((_, i) => (
              <button
                key={i}
                type="button"
                className={
                  'landing-klook-hero__dot' + (i === heroIndex ? ' landing-klook-hero__dot--active' : '')
                }
                aria-label={`Ảnh ${i + 1}`}
                aria-selected={i === heroIndex}
                onClick={() => setHeroIndex(i)}
              />
            ))}
          </div>
        </div>
      </section>

      {loadError && (
        <div className="container-wide" style={{ paddingTop: 16 }}>
          <div
            className="card"
            style={{
              padding: '16px 20px',
              border: '1px solid var(--error)',
              background: 'rgba(234,40,4,0.06)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 16,
              flexWrap: 'wrap',
            }}
          >
            <p className="body-sm" style={{ color: 'var(--error)', margin: 0 }}>
              {loadError}
            </p>
            <button type="button" className="btn-primary btn-sm" onClick={() => setReloadKey((k) => k + 1)}>
              Thử lại
            </button>
          </div>
        </div>
      )}



      {/* Featured Rooms */}
      <section className="section-pad-sm" style={{ background: 'var(--canvas)' }}>
        <div className="container-wide">
          <div className="flex items-end justify-between mb-8">
            <div>
              <h2 className="text-heading-lg" style={{ marginBottom: 6 }}>
                Featured Rooms
              </h2>
              <p className="body-md text-charcoal">Hand-picked rooms available for your next getaway</p>
            </div>
            <Link to="/rooms" className="btn-ghost">
              View all rooms →
            </Link>
          </div>

          {loadingRooms ? (
            <SectionSkeleton count={4} cols={4} />
          ) : featuredRooms.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {featuredRooms.map((room) => (
                <RoomCard key={room.id} room={toRoomCardProps(room)} />
              ))}
            </div>
          ) : (
            <div className="card" style={{ padding: 48, textAlign: 'center' }}>
              <p className="body-md text-charcoal">Featured rooms will appear here once properties are added.</p>
            </div>
          )}
        </div>
      </section>

      {/* Featured Properties */}
      <section id="properties" className="section-pad-sm" style={{ background: 'var(--surface-bone)', scrollMarginTop: 80 }}>
        <div className="container-wide">
          <div className="flex items-end justify-between mb-8">
            <div>
              <h2 className="text-heading-lg" style={{ marginBottom: 6 }}>
                Popular Destinations
              </h2>
              <p className="body-md text-charcoal">Premium homestays and resorts in Vietnam&apos;s top destinations</p>
            </div>
            <Link to="/rooms" className="btn-ghost">
              View all properties →
            </Link>
          </div>

          {loadingProperties ? (
            <SectionSkeleton count={3} cols={3} />
          ) : featuredProperties.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {featuredProperties.map((prop) => (
                <PropertyCard key={prop.id} property={prop} />
              ))}
            </div>
          ) : (
            <div className="card" style={{ padding: 48, textAlign: 'center' }}>
              <p className="body-md text-charcoal">Properties will appear here once added by managers.</p>
            </div>
          )}
        </div>
      </section>

      {/* How it Works */}
      <section className="section-pad" style={{ background: 'var(--canvas)' }}>
        <div className="container-wide">
          <h2 className="display-md" style={{ marginBottom: 12 }}>
            How It Works
          </h2>
          <p className="body-lg text-charcoal" style={{ marginBottom: 56 }}>
            Book your perfect stay in 3 simple steps
          </p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {HOW_IT_WORKS.map((step) => (
              <div key={step.step} className="card" style={{ padding: 28 }}>
                <div className="display-lg text-primary" style={{ marginBottom: 16 }}>
                  {step.step}
                </div>
                <h3 className="heading-sm" style={{ marginBottom: 10 }}>
                  {step.title}
                </h3>
                <p className="body-md text-charcoal" style={{ lineHeight: 1.7 }}>
                  {step.desc}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats */}
      <section className="section-pad-sm" style={{ background: 'var(--surface-bone)' }}>
        <div className="container-wide">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
            {statItems.map((stat) => (
              <div key={stat.label}>
                <div className="display-md text-primary" style={{ marginBottom: 6 }}>
                  {stat.value}
                </div>
                <p className="body-sm text-charcoal">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Band */}
      <section style={{ background: 'var(--primary)', padding: '72px 32px', textAlign: 'center' }}>
        <div className="container-wide">
          <h2 className="display-md" style={{ color: 'var(--on-dark)', marginBottom: 12 }}>
            Ready to Book Your Stay?
          </h2>
          <p className="body-lg" style={{ color: 'rgba(252,252,252,0.85)', marginBottom: 32, maxWidth: 520, margin: '0 auto 32px' }}>
            Join thousands of travelers who trust Homestay&amp;Resort for premium accommodations across Vietnam.
          </p>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link to="/rooms" className="btn-dark">
              Browse Rooms
            </Link>
            <Link
              to="/register"
              className="btn-outline"
              style={{ borderColor: 'rgba(255,255,255,0.35)', color: 'var(--on-dark)', background: 'transparent' }}
            >
              Create Free Account
            </Link>
          </div>
        </div>
      </section>
    </PublicLayout>
  );
}
