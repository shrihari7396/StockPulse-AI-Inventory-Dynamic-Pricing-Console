import React from 'react';
import { X, Layers, DollarSign, Building, BarChart2 } from 'lucide-react';

export default function ProductDetailModal({ product, onClose }) {
  if (!product) return null;

  const current = Number(product.currentPrice) || 0;
  const cost = Number(product.costPrice) || 0;
  const comp = Number(product.competitorPrice) || 0;
  const grossMargin = current > 0 && cost > 0 ? (((current - cost) / current) * 100).toFixed(1) : 'N/A';

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title">
            <Layers size={18} color="var(--accent-purple)" />
            <span>Sprint 2 Extensibility Inspector · {product.sku}</span>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer'
            }}
          >
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          <div style={{ marginBottom: 18 }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-main)' }}>
              {product.name}
            </h3>
            <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: 'var(--text-dim)' }}>
              ID: {product.id} · Department: {product.category} · Status: {product.status}
            </span>
          </div>

          <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: 20, lineHeight: 1.5 }}>
            Sprint 2 extension fields are natively modeled on the JPA Product entity and database schema. 
            These seams allow the pluggable <code>CompetitorAwareStrategy</code> to factor in wholesale margins and competitor market baselines.
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
            <div className="proposal-box">
              <div className="proposal-item">
                <span className="proposal-label">Retail Price</span>
                <span className="proposal-val">${current.toFixed(2)}</span>
              </div>
              <div className="proposal-item">
                <span className="proposal-label">Wholesale Cost (costPrice)</span>
                <span className="proposal-val" style={{ color: 'var(--accent-blue)' }}>
                  {cost > 0 ? `$${cost.toFixed(2)}` : 'Not Set'}
                </span>
              </div>
            </div>

            <div className="proposal-box">
              <div className="proposal-item">
                <span className="proposal-label">Competitor Benchmark</span>
                <span className="proposal-val" style={{ color: comp > current ? 'var(--accent-emerald)' : 'var(--accent-amber)' }}>
                  {comp > 0 ? `$${comp.toFixed(2)}` : 'Not Tracked'}
                </span>
              </div>
              <div className="proposal-item">
                <span className="proposal-label">Gross Margin %</span>
                <span className="proposal-val highlight">
                  {grossMargin !== 'N/A' ? `${grossMargin}%` : 'N/A'}
                </span>
              </div>
            </div>
          </div>

          <div style={{ marginTop: 16, padding: 14, borderRadius: 'var(--radius-md)', background: 'rgba(0,0,0,0.25)', border: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', gap: 12 }}>
            <Building size={20} color="var(--accent-cyan)" />
            <div>
              <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 600 }}>
                Supplier Replenishment Catalog ID (supplierId)
              </div>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.9rem', color: 'var(--text-main)', fontWeight: 600 }}>
                {product.supplierId || 'SUP-UNASSIGNED'}
              </div>
            </div>
          </div>

          <div style={{ marginTop: 24, display: 'flex', justifyContent: 'flex-end' }}>
            <button className="btn btn-secondary" onClick={onClose}>
              Close Inspector
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
