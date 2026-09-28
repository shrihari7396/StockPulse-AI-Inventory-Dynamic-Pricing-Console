import React, { useState, useEffect, useCallback } from 'react';
import Header from './components/Header';
import MetricsBar from './components/MetricsBar';
import SuggestionsDesk from './components/SuggestionsDesk';
import CatalogBoard from './components/CatalogBoard';
import StreamModal from './components/StreamModal';
import ProductDetailModal from './components/ProductDetailModal';
import {
  fetchProducts,
  fetchPricingSuggestions,
  fetchReorderSuggestions,
  fetchStrategyConfig,
  updateStrategyConfig,
  updatePricingSuggestionStatus,
  updateReorderSuggestionStatus,
  simulateOrder,
  updateStock
} from './services/api';

export default function App() {
  const [products, setProducts] = useState([]);
  const [pricingSuggestions, setPricingSuggestions] = useState([]);
  const [reorderSuggestions, setReorderSuggestions] = useState([]);
  const [strategyConfig, setStrategyConfig] = useState(null);

  const [isRefreshing, setIsRefreshing] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [processingSuggestionId, setProcessingSuggestionId] = useState(null);

  const [activeStreamProduct, setActiveStreamProduct] = useState(null);
  const [activeDetailProduct, setActiveDetailProduct] = useState(null);
  const [toasts, setToasts] = useState([]);

  const addToast = (message, type = 'info') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  };

  const loadData = useCallback(async (isManual = false) => {
    if (isManual) setIsRefreshing(true);
    try {
      const [prods, pricing, reorders, config] = await Promise.all([
        fetchProducts(),
        fetchPricingSuggestions(null, 'PENDING'),
        fetchReorderSuggestions(null, 'PENDING'),
        fetchStrategyConfig()
      ]);

      setProducts(prods);
      setPricingSuggestions(pricing);
      setReorderSuggestions(reorders);
      setStrategyConfig(config);
    } catch (err) {
      console.error('Failed to sync data:', err);
    } finally {
      if (isManual) setIsRefreshing(false);
    }
  }, []);

  // Initial load and 3-second autonomous polling loop
  useEffect(() => {
    loadData();
    const interval = setInterval(() => {
      loadData(false);
    }, 3000);
    return () => clearInterval(interval);
  }, [loadData]);

  // Handle Strategy Switch
  const handleStrategyChange = async (newConfig) => {
    try {
      const updated = await updateStrategyConfig(newConfig);
      setStrategyConfig(updated);
      addToast(`Strategy switched to ${updated.activePricingStrategy}`, 'success');
      loadData(false);
    } catch (err) {
      addToast(`Strategy switch failed: ${err.message}`, 'error');
    }
  };

  // Handle Simulated Order
  const handleSimulateOrder = async (productId, quantity) => {
    setActionLoadingId(productId);
    try {
      const updated = await simulateOrder(productId, quantity);
      addToast(`Simulated ${quantity} order(s) on ${updated.sku}. New Stock: ${updated.stockLevel}, Velocity: ${updated.demandVelocity}`, 'success');
      await loadData(false);
    } catch (err) {
      addToast(err.message, 'error');
    } finally {
      setActionLoadingId(null);
    }
  };

  // Handle Manual Stock Adjustment
  const handleUpdateStock = async (productId, newStockLevel) => {
    setActionLoadingId(productId);
    try {
      const updated = await updateStock(productId, newStockLevel);
      addToast(`Stock for ${updated.sku} adjusted to ${updated.stockLevel}`, 'success');
      await loadData(false);
    } catch (err) {
      addToast(err.message, 'error');
    } finally {
      setActionLoadingId(null);
    }
  };

  // Handle Accept Pricing
  const handleAcceptPricing = async (suggestionId) => {
    setProcessingSuggestionId(`price-${suggestionId}`);
    try {
      const updated = await updatePricingSuggestionStatus(suggestionId, 'ACCEPTED');
      addToast(`Price proposal accepted! Live retail price updated to $${Number(updated.recommendedPrice).toFixed(2)}`, 'success');
      await loadData(false);
    } catch (err) {
      addToast(`Acceptance failed: ${err.message}`, 'error');
    } finally {
      setProcessingSuggestionId(null);
    }
  };

  // Handle Reject Pricing
  const handleRejectPricing = async (suggestionId) => {
    setProcessingSuggestionId(`price-${suggestionId}`);
    try {
      await updatePricingSuggestionStatus(suggestionId, 'REJECTED');
      addToast('Pricing recommendation rejected.', 'info');
      await loadData(false);
    } catch (err) {
      addToast(`Rejection failed: ${err.message}`, 'error');
    } finally {
      setProcessingSuggestionId(null);
    }
  };

  // Handle Accept Reorder
  const handleAcceptReorder = async (suggestionId) => {
    setProcessingSuggestionId(`reorder-${suggestionId}`);
    try {
      const updated = await updateReorderSuggestionStatus(suggestionId, 'ACCEPTED');
      addToast(`Inbound reorder authorized! +${updated.recommendedQuantity} units added to inventory.`, 'success');
      await loadData(false);
    } catch (err) {
      addToast(`Reorder failed: ${err.message}`, 'error');
    } finally {
      setProcessingSuggestionId(null);
    }
  };

  // Handle Reject Reorder
  const handleRejectReorder = async (suggestionId) => {
    setProcessingSuggestionId(`reorder-${suggestionId}`);
    try {
      await updateReorderSuggestionStatus(suggestionId, 'REJECTED');
      addToast('Reorder recommendation rejected.', 'info');
      await loadData(false);
    } catch (err) {
      addToast(`Rejection failed: ${err.message}`, 'error');
    } finally {
      setProcessingSuggestionId(null);
    }
  };

  // Summary Metrics calculations
  const pendingCount = pricingSuggestions.length + reorderSuggestions.length;
  const lowStockCount = products.filter((p) => p.stockLevel < p.reorderThreshold).length;
  const spikeCount = products.filter((p) => p.demandVelocity >= 8).length;

  return (
    <div className="app-container">
      {/* Header with Live Engine Status & Strategy Selector */}
      <Header
        strategyConfig={strategyConfig}
        onStrategyChange={handleStrategyChange}
        isRefreshing={isRefreshing}
        onRefresh={() => loadData(true)}
      />

      {/* KPI Metrics Summary Bar */}
      <MetricsBar
        pendingCount={pendingCount}
        lowStockCount={lowStockCount}
        spikeCount={spikeCount}
        totalProducts={products.length}
      />

      {/* Merchandising Decision Desk (Pending Suggestions) */}
      <SuggestionsDesk
        pricingSuggestions={pricingSuggestions}
        reorderSuggestions={reorderSuggestions}
        onAcceptPricing={handleAcceptPricing}
        onRejectPricing={handleRejectPricing}
        onAcceptReorder={handleAcceptReorder}
        onRejectReorder={handleRejectReorder}
        processingId={processingSuggestionId}
      />

      {/* Catalog & Inventory Velocity Board */}
      <CatalogBoard
        products={products}
        onSimulateOrder={handleSimulateOrder}
        onUpdateStock={handleUpdateStock}
        onOpenStreamModal={(product) => setActiveStreamProduct(product)}
        onOpenDetailModal={(product) => setActiveDetailProduct(product)}
        actionLoadingId={actionLoadingId}
      />

      {/* SSE Reasoning Stream Modal (Bonus +5 pts) */}
      {activeStreamProduct && (
        <StreamModal
          product={activeStreamProduct}
          onClose={() => setActiveStreamProduct(null)}
          onSuggestionCreated={() => loadData(false)}
        />
      )}

      {/* Sprint 2 Extensibility Inspector Modal */}
      {activeDetailProduct && (
        <ProductDetailModal
          product={activeDetailProduct}
          onClose={() => setActiveDetailProduct(null)}
        />
      )}

      {/* Toast Notification Container */}
      <div className="toast-container">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.type}`}>
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
