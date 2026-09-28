import React, { useState } from 'react';
import { ShoppingCart, Flame, Plus, Sparkles, Info, Search } from 'lucide-react';

export default function CatalogBoard({
  products,
  onSimulateOrder,
  onUpdateStock,
  onOpenStreamModal,
  onOpenDetailModal,
  actionLoadingId
}) {
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [showLowStockOnly, setShowLowStockOnly] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const filteredProducts = products.filter((p) => {
    if (selectedCategory !== 'ALL' && p.category !== selectedCategory) return false;
    if (showLowStockOnly && p.stockLevel >= p.reorderThreshold) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q);
    }
    return true;
  });

  return (
    <section className="section">
      <div className="section-header">
        <div className="section-title-wrap">
          <h2 className="section-title">Catalog Inventory & Velocity Board</h2>
          <span className="section-badge">{products.length} Active SKUs</span>
        </div>
        <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
          Real-time stock trajectory, demand velocity signals, and interactive simulation triggers
        </p>
      </div>

      <div className="catalog-card">
        {/* Toolbar */}
        <div className="catalog-toolbar">
          <div className="filter-pills">
            {['ALL', 'ELECTRONICS', 'APPAREL', 'HOME'].map((cat) => (
              <button
                key={cat}
                className={`pill-btn ${selectedCategory === cat ? 'active' : ''}`}
                onClick={() => setSelectedCategory(cat)}
              >
                {cat === 'ALL' ? 'All Departments' : cat.charAt(0) + cat.slice(1).toLowerCase()}
              </button>
            ))}
            <button
              className={`pill-btn ${showLowStockOnly ? 'active' : ''}`}
              onClick={() => setShowLowStockOnly(!showLowStockOnly)}
              style={{
                borderColor: showLowStockOnly ? 'var(--accent-amber)' : 'var(--border-light)',
                color: showLowStockOnly ? 'var(--accent-amber)' : 'var(--text-muted)'
              }}
            >
              ⚠️ Low Stock Only
            </button>
          </div>

          <div style={{ position: 'relative', minWidth: 240 }}>
            <Search size={15} style={{ position: 'absolute', left: 10, top: 10, color: 'var(--text-dim)' }} />
            <input
              type="text"
              placeholder="Search SKU or name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                width: '100%',
                padding: '6px 12px 6px 32px',
                borderRadius: 'var(--radius-sm)',
                background: 'rgba(0, 0, 0, 0.3)',
                border: '1px solid var(--border-light)',
                color: 'var(--text-main)',
                fontSize: '0.82rem',
                outline: 'none'
              }}
            />
          </div>
        </div>

        {/* Table */}
        <div className="table-responsive">
          <table className="catalog-table">
            <thead>
              <tr>
                <th>Product SKU & Details</th>
                <th>Category</th>
                <th>Stock Level / Buffer</th>
                <th>Reorder Point</th>
                <th>24h Velocity</th>
                <th>Live Price</th>
                <th>Lifecycle Status</th>
                <th style={{ textAlign: 'right' }}>Interactive Demo Triggers</th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.map((p) => {
                const stock = p.stockLevel || 0;
                const threshold = p.reorderThreshold || 1;
                const isLow = stock < threshold && stock > 0;
                const isOut = stock === 0;
                const maxBar = Math.max(threshold * 2.5, stock);
                const fillPct = Math.min(100, Math.round((stock / maxBar) * 100));
                const isSpike = p.demandVelocity >= 8;
                const isLoading = actionLoadingId === p.id;

                return (
                  <tr key={p.id}>
                    <td>
                      <div className="product-cell">
                        <span className="product-sku">{p.sku}</span>
                        <span className="product-name">{p.name}</span>
                      </div>
                    </td>
                    <td>
                      <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                        {p.category}
                      </span>
                    </td>
                    <td>
                      <div className="stock-meter-wrap">
                        <div className="stock-count-text">
                          <span style={{ color: isOut ? 'var(--accent-rose)' : isLow ? 'var(--accent-amber)' : 'var(--accent-emerald)', fontWeight: 700 }}>
                            {stock} units
                          </span>
                          <span style={{ color: 'var(--text-dim)' }}>cap {Math.round(maxBar)}</span>
                        </div>
                        <div className="stock-bar-track">
                          <div
                            className={`stock-bar-fill ${isOut ? 'out' : isLow ? 'low' : 'healthy'}`}
                            style={{ width: `${fillPct}%` }}
                          ></div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                        {threshold} units
                      </span>
                    </td>
                    <td>
                      <div className={`velocity-badge ${isSpike ? 'hot' : ''}`}>
                        {isSpike && <Flame size={14} color="var(--accent-pink)" />}
                        <span>{p.demandVelocity} /day</span>
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, fontSize: '0.95rem' }}>
                          ${Number(p.currentPrice).toFixed(2)}
                        </span>
                        <button
                          className="btn-icon-action"
                          style={{ padding: '2px 5px', fontSize: '0.68rem' }}
                          title="View wholesale margin & Sprint 2 extensions"
                          onClick={() => onOpenDetailModal(p)}
                        >
                          <Info size={12} />
                        </button>
                      </div>
                    </td>
                    <td>
                      <span className={`status-tag ${p.status}`}>
                        {p.status === 'PRICE_REVIEW_PENDING' ? 'Review Pending' : p.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td>
                      <div className="row-actions" style={{ justifyContent: 'flex-end' }}>
                        {/* Simulate 1 Order */}
                        <button
                          className="btn-icon-action"
                          title="Simulate customer order (decrements stock by 1)"
                          disabled={stock <= 0 || isLoading}
                          onClick={() => onSimulateOrder(p.id, 1)}
                        >
                          <ShoppingCart size={13} />
                          <span>-1 Sale</span>
                        </button>

                        {/* Simulate Viral Spike (+5) */}
                        <button
                          className="btn-icon-action spike"
                          title="Simulate viral burst order (fires Trigger B demand surge)"
                          disabled={stock < 5 || isLoading}
                          onClick={() => onSimulateOrder(p.id, 5)}
                        >
                          <Flame size={13} />
                          <span>+5 Viral</span>
                        </button>

                        {/* Quick Restock (+20) */}
                        <button
                          className="btn-icon-action"
                          title="Manual restock (+20 units)"
                          disabled={isLoading}
                          onClick={() => onUpdateStock(p.id, stock + 20)}
                        >
                          <Plus size={13} />
                          <span>+20 Stock</span>
                        </button>

                        {/* SSE Stream AI Reasoning */}
                        <button
                          className="btn-icon-action ai"
                          title="Live stream AI Advisor tokens (Bonus +5 pts)"
                          onClick={() => onOpenStreamModal(p)}
                        >
                          <Sparkles size={13} />
                          <span>Stream AI</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
