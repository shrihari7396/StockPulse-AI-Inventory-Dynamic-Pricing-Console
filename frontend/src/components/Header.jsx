import React from 'react';
import { Activity, RefreshCw, Cpu } from './Icons';

export default function Header({
  strategyConfig,
  onStrategyChange,
  isRefreshing,
  onRefresh
}) {
  const activePricing = strategyConfig?.activePricingStrategy || 'RULE_BASED';

  const handleSelect = (e) => {
    const val = e.target.value;
    onStrategyChange({
      pricingStrategy: val,
      reorderStrategy: val === 'COMPETITOR_AWARE' ? 'RULE_BASED' : val
    });
  };

  return (
    <header className="header">
      <div className="brand">
        <div className="brand-icon-wrapper">
          <Activity size={24} />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <h1 className="brand-title">StockPulse</h1>
            <span className="brand-tag">Autonomous Commerce</span>
          </div>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            AI Inventory Signals & Reactive Dynamic Pricing Console
          </p>
        </div>
      </div>

      <div className="header-actions">
        {/* Live Polling Status */}
        <div className="sync-status">
          <span className="pulse-dot"></span>
          <span>Live Signal Engine (3s)</span>
        </div>

        {/* Manual Refresh Button */}
        <button
          className="btn btn-secondary"
          onClick={onRefresh}
          disabled={isRefreshing}
          title="Force immediate refresh"
          style={{ padding: '8px 12px' }}
        >
          <RefreshCw size={15} className={isRefreshing ? 'terminal-cursor' : ''} />
          <span>{isRefreshing ? 'Syncing...' : 'Refresh'}</span>
        </button>

        {/* Runtime Strategy Switcher */}
        <div className="strategy-switch-group">
          <Cpu size={16} color="var(--accent-blue)" style={{ marginLeft: 8 }} />
          <span className="strategy-label">Engine:</span>
          <select
            className="strategy-select"
            value={activePricing}
            onChange={handleSelect}
            aria-label="Select dynamic commerce strategy"
          >
            <option value="RULE_BASED">Rule-Based Engine (Deterministic)</option>
            <option value="AI">AI Commerce Advisor (LLM Flash)</option>
            <option value="COMPETITOR_AWARE">Competitor-Aware Engine (Sprint 2)</option>
          </select>
        </div>
      </div>
    </header>
  );
}
