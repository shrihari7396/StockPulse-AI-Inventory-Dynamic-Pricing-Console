import React, { useEffect, useState } from 'react';
import { X, Sparkles, CheckCircle } from './Icons';
import { streamPricingReasoning } from '../services/api';

export default function StreamModal({ product, onClose, onSuggestionCreated }) {
  const [statusMessage, setStatusMessage] = useState('Connecting to live AI inference gateway...');
  const [streamedText, setStreamedText] = useState('');
  const [isDone, setIsDone] = useState(false);
  const [finalSuggestion, setFinalSuggestion] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  useEffect(() => {
    if (!product) return;

    setStatusMessage(`Requesting real-time SSE stream for SKU: ${product.sku}...`);
    setStreamedText('');
    setIsDone(false);
    setFinalSuggestion(null);
    setErrorMessage(null);

    streamPricingReasoning(product.id, {
      onStatus: (msg) => {
        setStatusMessage(msg);
      },
      onToken: (token) => {
        setStreamedText((prev) => prev + token);
      },
      onComplete: (suggestion) => {
        setIsDone(true);
        setFinalSuggestion(suggestion);
        setStatusMessage('AI Reasoning Complete. Suggestion Persisted.');
        if (onSuggestionCreated) {
          onSuggestionCreated(suggestion);
        }
      },
      onError: (err) => {
        setErrorMessage(err);
        setIsDone(true);
      }
    });
  }, [product]);

  if (!product) return null;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title">
            <Sparkles size={18} color="var(--accent-blue)" />
            <span>AI Reasoning Stream · {product.sku}</span>
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
          <div style={{ marginBottom: 14, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)', textTransform: 'uppercase', fontWeight: 600 }}>
              Target SKU: <strong style={{ color: 'var(--text-main)' }}>{product.name}</strong>
            </span>
            <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: isDone ? 'var(--accent-emerald)' : 'var(--accent-blue)' }}>
              {statusMessage}
            </span>
          </div>

          {/* Terminal Box */}
          <div className="stream-terminal">
            <div style={{ color: 'var(--text-dim)', fontSize: '0.75rem', marginBottom: 8, borderBottom: '1px solid rgba(255,255,255,0.06)', paddingBottom: 4 }}>
              [SSE Protocol · text/event-stream · POST /products/{product.id}/suggest-pricing/stream]
            </div>
            {streamedText || 'Awaiting first reasoning token from LLM...'}
            {!isDone && <span className="terminal-cursor"></span>}
          </div>

          {errorMessage && (
            <div style={{ marginTop: 14, padding: 12, borderRadius: 'var(--radius-md)', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', fontSize: '0.85rem' }}>
              Stream Error: {errorMessage}
            </div>
          )}

          {/* Finalized Suggestion Box */}
          {finalSuggestion && (
            <div style={{
              marginTop: 18,
              padding: 16,
              borderRadius: 'var(--radius-md)',
              background: 'rgba(16, 185, 129, 0.08)',
              border: '1px solid rgba(16, 185, 129, 0.3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#34d399', fontWeight: 600, fontSize: '0.88rem' }}>
                  <CheckCircle size={16} />
                  <span>Suggestion #{finalSuggestion.id} Queued</span>
                </div>
                <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: 2 }}>
                  Recommended: ${Number(finalSuggestion.recommendedPrice).toFixed(2)} (Confidence: {Math.round(finalSuggestion.confidence * 100)}%)
                </div>
              </div>

              <button
                className="btn btn-accept"
                onClick={onClose}
                style={{ padding: '8px 14px', fontSize: '0.8rem' }}
              >
                View on Desk
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
