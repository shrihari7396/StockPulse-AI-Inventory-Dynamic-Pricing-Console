import React from 'react';
import { Check, X, TrendingUp, TrendingDown, Package, ShieldCheck, Sparkles, AlertTriangle } from './Icons';

export default function SuggestionsDesk({
  pricingSuggestions,
  reorderSuggestions,
  onAcceptPricing,
  onRejectPricing,
  onAcceptReorder,
  onRejectReorder,
  processingId
}) {
  const hasPending = pricingSuggestions.length > 0 || reorderSuggestions.length > 0;

  const renderBadge = (triggerReason) => {
    switch (triggerReason) {
      case 'INVENTORY_LOW':
        return (
          <span className="badge inventory-low">
            <AlertTriangle size={12} />
            Inventory Low
          </span>
        );
      case 'DEMAND_SPIKE':
        return (
          <span className="badge demand-spike">
            <Sparkles size={12} />
            Demand Spike
          </span>
        );
      default:
        return (
          <span className="badge manual">
            Manual Review
          </span>
        );
    }
  };

  return (
    <section className="section">
      <div className="section-header">
        <div className="section-title-wrap">
          <h2 className="section-title">Merchandising Decision Desk</h2>
          <span className="section-badge">
            {pricingSuggestions.length + reorderSuggestions.length} Pending Actions
          </span>
        </div>
        <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
          Review autonomous AI proposals before publishing to live storefront
        </p>
      </div>

      {!hasPending ? (
        <div className="empty-state">
          <ShieldCheck className="empty-icon" />
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: 6 }}>
            Commerce State in Equilibrium
          </h3>
          <p style={{ fontSize: '0.88rem', maxWidth: 460, margin: '0 auto' }}>
            No pending pricing or replenishment proposals. Simulate customer orders or trigger stock adjustments below to activate the agentic recommendation loop.
          </p>
        </div>
      ) : (
        <div className="suggestions-grid">
          {/* Pricing Suggestions */}
          {pricingSuggestions.map((item) => {
            const current = Number(item.currentPrice);
            const recommended = Number(item.recommendedPrice);
            const deltaPct = current > 0 ? (((recommended - current) / current) * 100).toFixed(1) : 0;
            const isUp = recommended > current;
            const isDown = recommended < current;
            const isProcessing = processingId === `price-${item.id}`;

            return (
              <div
                key={`price-${item.id}`}
                className={`suggestion-card ${item.triggerReason.toLowerCase().replace('_', '-')}`}
              >
                <div className="card-top">
                  <div>
                    <span className="sku-pill">{item.product.sku}</span>
                    <h3 className="card-product-name">{item.product.name}</h3>
                  </div>
                  {renderBadge(item.triggerReason)}
                </div>

                <div className="proposal-box">
                  <div className="proposal-item">
                    <span className="proposal-label">Current Retail</span>
                    <span className="proposal-val">${current.toFixed(2)}</span>
                  </div>
                  <div className="proposal-item">
                    <span className="proposal-label">Proposed Price</span>
                    <span className="proposal-val highlight">
                      ${recommended.toFixed(2)}
                      <span className={`delta-pill ${isUp ? 'up' : isDown ? 'down' : ''}`}>
                        {isUp && <TrendingUp size={10} style={{ display: 'inline', marginRight: 2 }} />}
                        {isDown && <TrendingDown size={10} style={{ display: 'inline', marginRight: 2 }} />}
                        {isUp ? `+${deltaPct}%` : isDown ? `${deltaPct}%` : '0%'}
                      </span>
                    </span>
                  </div>
                </div>

                <div className={`reasoning-box ${item.triggerReason.toLowerCase().replace('_', '-')}`}>
                  <strong style={{ display: 'block', fontSize: '0.74rem', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-muted)', marginBottom: 4 }}>
                    Strategy: {item.strategyUsed}
                  </strong>
                  {item.reasoning}
                </div>

                <div className="confidence-bar-wrap">
                  <span>Advisor Confidence</span>
                  <div className="meter-track">
                    <div
                      className="meter-fill"
                      style={{ width: `${Math.round(item.confidence * 100)}%` }}
                    ></div>
                  </div>
                  <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                    {Math.round(item.confidence * 100)}%
                  </span>
                </div>

                <div className="card-actions">
                  <button
                    className="btn btn-accept"
                    disabled={isProcessing}
                    onClick={() => onAcceptPricing(item.id)}
                  >
                    <Check size={16} />
                    <span>{isProcessing ? 'Updating...' : 'Publish Price'}</span>
                  </button>
                  <button
                    className="btn btn-reject"
                    disabled={isProcessing}
                    onClick={() => onRejectPricing(item.id)}
                  >
                    <X size={16} />
                    <span>Reject</span>
                  </button>
                </div>
              </div>
            );
          })}

          {/* Reorder Suggestions */}
          {reorderSuggestions.map((item) => {
            const isProcessing = processingId === `reorder-${item.id}`;

            return (
              <div
                key={`reorder-${item.id}`}
                className={`suggestion-card ${item.triggerReason.toLowerCase().replace('_', '-')}`}
              >
                <div className="card-top">
                  <div>
                    <span className="sku-pill">{item.product.sku}</span>
                    <h3 className="card-product-name">{item.product.name}</h3>
                  </div>
                  {renderBadge(item.triggerReason)}
                </div>

                <div className="proposal-box">
                  <div className="proposal-item">
                    <span className="proposal-label">Current Stock</span>
                    <span className="proposal-val">{item.currentStock} units</span>
                  </div>
                  <div className="proposal-item">
                    <span className="proposal-label">Order Quantity</span>
                    <span className="proposal-val highlight">
                      +{item.recommendedQuantity} units
                      <span className="delta-pill up" style={{ marginLeft: 6 }}>
                        {item.suggestedLeadTimeDays}d lead
                      </span>
                    </span>
                  </div>
                </div>

                <div className={`reasoning-box ${item.triggerReason.toLowerCase().replace('_', '-')}`}>
                  <strong style={{ display: 'block', fontSize: '0.74rem', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-muted)', marginBottom: 4 }}>
                    Strategy: {item.strategyUsed} (Replenishment)
                  </strong>
                  {item.reasoning}
                </div>

                <div className="confidence-bar-wrap">
                  <span>Replenishment Confidence</span>
                  <div className="meter-track">
                    <div
                      className="meter-fill"
                      style={{ width: `${Math.round(item.confidence * 100)}%` }}
                    ></div>
                  </div>
                  <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                    {Math.round(item.confidence * 100)}%
                  </span>
                </div>

                <div className="card-actions">
                  <button
                    className="btn btn-accept"
                    disabled={isProcessing}
                    onClick={() => onAcceptReorder(item.id)}
                  >
                    <Package size={16} />
                    <span>{isProcessing ? 'Simulating Inbound...' : 'Authorize Reorder'}</span>
                  </button>
                  <button
                    className="btn btn-reject"
                    disabled={isProcessing}
                    onClick={() => onRejectReorder(item.id)}
                  >
                    <X size={16} />
                    <span>Reject</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
