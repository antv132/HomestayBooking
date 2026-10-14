import React, { useState } from 'react';

interface PhotoLightboxProps {
  photoUrls: string[];
  getPhotoUrl: (url: string) => string;
}

export const PhotoLightbox: React.FC<PhotoLightboxProps> = ({ photoUrls, getPhotoUrl }) => {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null);

  if (!photoUrls || photoUrls.length === 0) return null;

  return (
    <div>
      {/* Thumbnails - always visible */}
      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        {photoUrls.map((url, idx) => (
          <div 
            key={idx} 
            onClick={() => setExpandedIndex(idx)}
            style={{ 
              width: 110, height: 110, borderRadius: 8, overflow: 'hidden', 
              cursor: 'pointer', border: '2px solid var(--hairline)',
              transition: 'transform 0.2s, box-shadow 0.2s',
            }}
            onMouseOver={e => { e.currentTarget.style.transform = 'scale(1.05)'; e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)'; }}
            onMouseOut={e => { e.currentTarget.style.transform = 'scale(1)'; e.currentTarget.style.boxShadow = 'none'; }}
          >
            <img src={getPhotoUrl(url)} alt={`Photo ${idx+1}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          </div>
        ))}
      </div>

      {/* Full screen overlay - only when a photo is clicked */}
      {expandedIndex !== null && (
        <div 
          onClick={() => setExpandedIndex(null)}
          style={{
            position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
            backgroundColor: 'rgba(0,0,0,0.92)', zIndex: 9999,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}
        >
          {/* Close button - top right */}
          <button 
            onClick={() => setExpandedIndex(null)}
            style={{
              position: 'absolute', top: 16, right: 24, background: 'none', border: 'none',
              color: 'white', fontSize: 36, cursor: 'pointer', zIndex: 10001,
              width: 44, height: 44, display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
            title="Đóng"
          >
            &times;
          </button>

          {/* Download button - top right, next to close */}
          <a 
            href={getPhotoUrl(photoUrls[expandedIndex])} 
            download
            target="_blank"
            rel="noreferrer"
            onClick={e => e.stopPropagation()}
            style={{
              position: 'absolute', top: 20, right: 72, 
              background: 'rgba(255,255,255,0.15)', color: 'white',
              textDecoration: 'none', borderRadius: 8, padding: '8px 16px', 
              fontWeight: 600, fontSize: 14,
              display: 'inline-flex', gap: 8, alignItems: 'center',
              zIndex: 10001, transition: 'background 0.2s', cursor: 'pointer',
              backdropFilter: 'blur(4px)',
            }}
            onMouseOver={e => e.currentTarget.style.background = 'rgba(255,255,255,0.3)'}
            onMouseOut={e => e.currentTarget.style.background = 'rgba(255,255,255,0.15)'}
            title="Tải ảnh về máy"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            Tải về
          </a>

          {/* The expanded image */}
          <img 
            src={getPhotoUrl(photoUrls[expandedIndex])} 
            alt="Expanded"
            onClick={e => e.stopPropagation()}
            style={{ 
              maxWidth: '90vw', maxHeight: '90vh', objectFit: 'contain',
              borderRadius: 4,
            }} 
          />
        </div>
      )}
    </div>
  );
};
