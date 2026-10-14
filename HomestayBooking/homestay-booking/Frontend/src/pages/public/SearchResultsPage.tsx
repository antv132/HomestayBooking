import { Link } from 'react-router-dom';
import PublicLayout from '../../layouts/PublicLayout';
import Pagination from '../../components/ui/Pagination';
import Alert from '../../components/ui/Alert';
import RoomSearchCard, { RoomCardSkeleton } from '../../components/rooms/RoomSearchCard';
import SearchSuggestionsInput from '../../components/rooms/SearchSuggestionsInput';
import { buildSearchSummary, useRoomSearch } from '../../hooks/useRoomSearch';
import {
  blockNonDigitKey,
  clampPositiveIntString,
  digitsOnly,
} from '../../utils/filterInput';

type SearchResultsVariant = 'search' | 'rooms';

export function SearchResultsContent({ variant = 'search' }: { variant?: SearchResultsVariant }) {
  const s = useRoomSearch();
  const {
    draft,
    setDraft,
    priceMin,
    sliderMax,
    MAX_FILTER_PRICE,
    MAX_FILTER_GUESTS,
    properties,
    rooms,
    totalElements,
    totalPages,
    loading,
    error,
    urlPage,
    urlSort,
    urlLocation,
    urlCheckIn,
    urlCheckOut,
    urlGuests,
    activeChips,
    hasActiveFilters,
    detailQuerySuffix,
    applyFilters,
    applySuggestionTerm,
    clearFilters,
    toggleRoomType,
    toggleProperty,
    handleSortChange,
    handlePageChange,
    loadRooms,
    ROOM_TYPES,
  } = s;

  const summary = buildSearchSummary(urlLocation, urlCheckIn, urlCheckOut, urlGuests, hasActiveFilters);

  const breadcrumbCurrent = variant === 'rooms' ? 'Danh sách phòng' : 'Kết quả tìm kiếm';
  const pageTitle =
    variant === 'rooms' && !hasActiveFilters && !urlLocation && !urlCheckIn
      ? 'Tất cả phòng đang trống'
      : summary;

  return (
    <div className="container-wide section-pad-sm">
      <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20, flexWrap: 'wrap' }}>
        <Link to="/" className="text-primary" style={{ textDecoration: 'none' }}>
          Trang chủ
        </Link>
        <span>›</span>
        <span className="text-ink" style={{ fontWeight: 600 }}>
          {breadcrumbCurrent}
        </span>
      </div>

      <section
        style={{
          background: 'var(--surface-bone)',
          borderRadius: 12,
          padding: '20px 24px',
          marginBottom: 28,
          border: '1px solid var(--hairline)',
        }}
      >
        <h1 className="heading-md font-display" style={{ marginBottom: 4 }}>
          {pageTitle}
        </h1>
        <p className="body-sm text-charcoal">
          {loading ? 'Đang tìm...' : `${totalElements} phòng phù hợp`}
        </p>
      </section>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(260px, 280px) minmax(0, 1fr)',
          gap: 28,
          alignItems: 'start',
        }}
      >
        <aside
          className="card search-filter-aside"
          style={{ padding: 24, position: 'sticky', top: 88, border: '1px solid var(--hairline)' }}
        >
          <h2 className="heading-sm font-display" style={{ marginBottom: 20 }}>
            Bộ lọc
          </h2>

          <div style={{ marginBottom: 20 }}>
            <label className="form-label">Địa điểm / tìm kiếm</label>
            <SearchSuggestionsInput
              value={draft.search}
              onChange={(search) => setDraft((p) => ({ ...p, search }))}
              onSelect={(item) => applySuggestionTerm(item.label)}
              onEnter={() => applyFilters()}
              placeholder="Đà Nẵng, Hội An..."
              ariaLabel="Địa điểm / tìm kiếm"
              style={{ borderRadius: 10 }}
              inputClassName="input"
            />
          </div>

          <div style={{ marginBottom: 20 }}>
            <label className="form-label">Khoảng giá</label>
            <div className="search-filter-price-row" style={{ marginBottom: 8 }}>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                className="input search-filter-input-compact"
                placeholder="Tối thiểu"
                value={draft.minPrice}
                onKeyDown={blockNonDigitKey}
                onChange={(e) => setDraft((p) => ({ ...p, minPrice: digitsOnly(e.target.value) }))}
                onBlur={() =>
                  setDraft((p) => ({
                    ...p,
                    minPrice: p.minPrice ? clampPositiveIntString(p.minPrice, MAX_FILTER_PRICE) : '',
                  }))
                }
              />
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                className="input search-filter-input-compact"
                placeholder="Tối đa"
                value={draft.maxPrice}
                onKeyDown={blockNonDigitKey}
                onChange={(e) => setDraft((p) => ({ ...p, maxPrice: digitsOnly(e.target.value) }))}
                onBlur={() =>
                  setDraft((p) => {
                    let maxP = p.maxPrice ? clampPositiveIntString(p.maxPrice, MAX_FILTER_PRICE) : '';
                    const minP = p.minPrice ? clampPositiveIntString(p.minPrice, MAX_FILTER_PRICE) : '';
                    if (minP && maxP && Number(maxP) < Number(minP)) maxP = minP;
                    return { ...p, maxPrice: maxP };
                  })
                }
              />
            </div>
            <input
              type="range"
              min={priceMin}
              max={sliderMax}
              step={Math.round((sliderMax - priceMin) / 20 / 50000) * 50000 || 50000}
              value={draft.maxPrice ? Math.min(Number(draft.maxPrice), sliderMax) : sliderMax}
              onChange={(e) =>
                setDraft((p) => ({
                  ...p,
                  maxPrice: clampPositiveIntString(e.target.value, MAX_FILTER_PRICE),
                }))
              }
              style={{ width: '100%' }}
            />
            <p className="body-sm text-charcoal" style={{ marginTop: 6, marginBottom: 0 }}>
              Tối đa {MAX_FILTER_PRICE.toLocaleString('vi-VN')}
            </p>
          </div>

          <div style={{ marginBottom: 20 }}>
            <p className="form-label" style={{ marginBottom: 10 }}>
              Loại phòng
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {ROOM_TYPES.map((type) => (
                <label key={type} className="flex items-center gap-2 body-sm" style={{ cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={draft.roomTypes.includes(type)}
                    onChange={() => toggleRoomType(type)}
                  />
                  {type}
                </label>
              ))}
            </div>
          </div>

          <div style={{ marginBottom: 20 }}>
            <p className="form-label" style={{ marginBottom: 10 }}>
              Homestay / Resort
            </p>
            <div style={{ maxHeight: 160, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 8 }}>
              {properties.map((p) => (
                <label key={p.id} className="flex items-center gap-2 body-sm" style={{ cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={draft.propertyIds.includes(p.id)}
                    onChange={() => toggleProperty(p.id)}
                  />
                  <span style={{ lineHeight: 1.3 }}>{p.name}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="search-filter-dates" style={{ marginBottom: 20 }}>
            <div className="search-filter-date-field">
              <label className="form-label">Check-in</label>
              <input
                type="date"
                className="input search-filter-input-compact search-filter-date-input"
                value={draft.checkIn}
                onChange={(e) => setDraft((p) => ({ ...p, checkIn: e.target.value }))}
              />
            </div>
            <div className="search-filter-date-field">
              <label className="form-label">Check-out</label>
              <input
                type="date"
                className="input search-filter-input-compact search-filter-date-input"
                value={draft.checkOut}
                min={draft.checkIn || undefined}
                onChange={(e) => setDraft((p) => ({ ...p, checkOut: e.target.value }))}
              />
            </div>
          </div>

          <div style={{ marginBottom: 20 }}>
            <label className="form-label">Số khách tối thiểu</label>
            <input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              className="input"
              placeholder="1–100"
              value={draft.guests}
              onKeyDown={blockNonDigitKey}
              onChange={(e) => setDraft((p) => ({ ...p, guests: digitsOnly(e.target.value) }))}
              onBlur={() =>
                setDraft((p) => ({
                  ...p,
                  guests: p.guests ? clampPositiveIntString(p.guests, MAX_FILTER_GUESTS) : '',
                }))
              }
              style={{ borderRadius: 10, height: 40, fontSize: 14 }}
            />
          </div>

          <div className="flex flex-col gap-2">
            <button type="button" className="btn-primary btn-sm" onClick={() => applyFilters(true)}>
              Áp dụng
            </button>
            <button type="button" className="btn-ghost btn-sm" onClick={clearFilters}>
              Xóa bộ lọc
            </button>
          </div>
        </aside>

        <div>
          <div className="flex items-center justify-between flex-wrap gap-3" style={{ marginBottom: 16 }}>
            <div className="flex flex-wrap gap-2">
              {activeChips.map((chip) => (
                <button
                  key={chip.label}
                  type="button"
                  className="badge badge-tag"
                  style={{ cursor: 'pointer', border: '1px solid var(--hairline)' }}
                  onClick={chip.onRemove}
                >
                  {chip.label} ×
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2">
              <span className="body-sm text-charcoal">Sắp xếp:</span>
              <select
                className="select"
                value={urlSort}
                onChange={(e) => handleSortChange(e.target.value)}
                style={{ width: 'auto', paddingRight: 36, fontSize: 14, height: 38 }}
              >
                <option value="newest">Mới nhất</option>
                <option value="price-asc">Giá: thấp → cao</option>
                <option value="price-desc">Giá: cao → thấp</option>
                <option value="rating">Đánh giá cao</option>
              </select>
            </div>
          </div>

          {error && (
            <div style={{ marginBottom: 16 }}>
              <Alert variant="error" message={error} />
              <button type="button" className="btn-primary btn-sm" onClick={() => loadRooms()} style={{ marginTop: 12 }}>
                Thử lại
              </button>
            </div>
          )}

          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
              {Array.from({ length: 6 }).map((_, i) => (
                <RoomCardSkeleton key={i} />
              ))}
            </div>
          ) : rooms.length === 0 ? (
            <div
              className="card"
              style={{ textAlign: 'center', padding: '64px 32px', border: '1px dashed var(--hairline)' }}
            >
              <div style={{ fontSize: 56, marginBottom: 16, lineHeight: 1 }}>🏨</div>
              <h3 className="heading-sm font-display" style={{ marginBottom: 8 }}>
                Không tìm thấy phòng phù hợp
              </h3>
              <p className="body-md text-charcoal" style={{ marginBottom: 24, maxWidth: 400, margin: '0 auto 24px' }}>
                Thử đổi địa điểm, ngày ở hoặc bỏ bớt bộ lọc để xem thêm kết quả.
              </p>
              <button type="button" className="btn-primary" onClick={clearFilters}>
                Xóa bộ lọc
              </button>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
                {rooms.map((room) => (
                  <RoomSearchCard key={room.id} room={room} querySuffix={detailQuerySuffix} />
                ))}
              </div>
              <Pagination page={urlPage} totalPages={totalPages} onPageChange={handlePageChange} />
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default function SearchResultsPage() {
  return (
    <PublicLayout>
      <SearchResultsContent />
    </PublicLayout>
  );
}
