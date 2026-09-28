import React from 'react';
import { AlertCircle, Flame, Layers, Clock } from 'lucide-react';

export default function MetricsBar({
  pendingCount,
  lowStockCount,
  spikeCount,
  totalProducts
}) {
  return (
    <div className="metrics-grid">
      <div className="metric-card">
        <div className="metric-icon-box blue">
          <Clock size={24} />
        </div>
        <div className="metric-info">
          <h3>Pending Approvals</h3>
          <div className="metric-value">{pendingCount}</div>
        </div>
      </div>

      <div className="metric-card">
        <div className="metric-icon-box amber">
          <AlertCircle size={24} />
        </div>
        <div className="metric-info">
          <h3>Low Stock Alerts</h3>
          <div className="metric-value">{lowStockCount}</div>
        </div>
      </div>

      <div className="metric-card">
        <div className="metric-icon-box pink">
          <Flame size={24} />
        </div>
        <div className="metric-info">
          <h3>Demand Velocity Spikes</h3>
          <div className="metric-value">{spikeCount}</div>
        </div>
      </div>

      <div className="metric-card">
        <div className="metric-icon-box purple">
          <Layers size={24} />
        </div>
        <div className="metric-info">
          <h3>Tracked SKUs</h3>
          <div className="metric-value">{totalProducts}</div>
        </div>
      </div>
    </div>
  );
}
