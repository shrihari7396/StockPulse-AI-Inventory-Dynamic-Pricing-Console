/**
 * StockPulse Backend API Service Client
 * Handles communication with Spring Boot backend (http://localhost:8080)
 */

const BASE_URL = 'http://localhost:8080';

export async function fetchProducts(status = null, category = null) {
  const params = new URLSearchParams();
  if (status) params.append('status', status);
  if (category) params.append('category', category);

  const res = await fetch(`${BASE_URL}/products?${params.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch products: ${res.statusText}`);
  return res.json();
}

export async function fetchProductById(id) {
  const res = await fetch(`${BASE_URL}/products/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch product ${id}`);
  return res.json();
}

export async function updateStock(id, stockLevel) {
  const res = await fetch(`${BASE_URL}/products/${id}/stock`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ stockLevel: Number(stockLevel) })
  });
  if (!res.ok) throw new Error(`Stock update failed: ${res.statusText}`);
  return res.json();
}

export async function simulateOrder(id, quantity = 1) {
  const res = await fetch(`${BASE_URL}/products/${id}/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quantity: Number(quantity) })
  });
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || `Order simulation failed: ${res.statusText}`);
  }
  return res.json();
}

export async function fetchPricingSuggestions(productId = null, status = null, triggerReason = null) {
  const params = new URLSearchParams();
  if (productId) params.append('productId', productId);
  if (status) params.append('status', status);
  if (triggerReason) params.append('triggerReason', triggerReason);

  const res = await fetch(`${BASE_URL}/pricing-suggestions?${params.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch pricing suggestions: ${res.statusText}`);
  return res.json();
}

export async function updatePricingSuggestionStatus(id, status) {
  const res = await fetch(`${BASE_URL}/pricing-suggestions/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status })
  });
  if (!res.ok) throw new Error(`Failed to update pricing suggestion: ${res.statusText}`);
  return res.json();
}

export async function fetchReorderSuggestions(productId = null, status = null, triggerReason = null) {
  const params = new URLSearchParams();
  if (productId) params.append('productId', productId);
  if (status) params.append('status', status);
  if (triggerReason) params.append('triggerReason', triggerReason);

  const res = await fetch(`${BASE_URL}/reorder-suggestions?${params.toString()}`);
  if (!res.ok) throw new Error(`Failed to fetch reorder suggestions: ${res.statusText}`);
  return res.json();
}

export async function updateReorderSuggestionStatus(id, status) {
  const res = await fetch(`${BASE_URL}/reorder-suggestions/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status })
  });
  if (!res.ok) throw new Error(`Failed to update reorder suggestion: ${res.statusText}`);
  return res.json();
}

export async function fetchStrategyConfig() {
  const res = await fetch(`${BASE_URL}/api/config/strategy`);
  if (!res.ok) throw new Error(`Failed to fetch strategy config: ${res.statusText}`);
  return res.json();
}

export async function updateStrategyConfig(config) {
  const res = await fetch(`${BASE_URL}/api/config/strategy`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(config)
  });
  if (!res.ok) throw new Error(`Failed to update strategy: ${res.statusText}`);
  return res.json();
}

export async function triggerManualPricing(productId) {
  const res = await fetch(`${BASE_URL}/products/${productId}/suggest-pricing`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error(`Manual pricing request failed: ${res.statusText}`);
  return res.json();
}

export async function triggerManualReorder(productId) {
  const res = await fetch(`${BASE_URL}/products/${productId}/suggest-reorder`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error(`Manual reorder request failed: ${res.statusText}`);
  return res.json();
}

/**
 * Bonus +5 pts: Real-time SSE token stream reader using ReadableStream
 */
export async function streamPricingReasoning(productId, callbacks = {}) {
  const { onStatus, onToken, onComplete, onError } = callbacks;

  try {
    const response = await fetch(`${BASE_URL}/products/${productId}/suggest-pricing/stream`, {
      method: 'POST',
      headers: { 'Accept': 'text/event-stream' }
    });

    if (!response.ok) {
      throw new Error(`SSE stream failed: ${response.statusText}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n\n');
      buffer = lines.pop(); // Keep incomplete chunk

      for (const block of lines) {
        if (!block.trim()) continue;
        const blockLines = block.split('\n');
        let eventType = 'message';
        let eventData = '';

        for (const line of blockLines) {
          if (line.startsWith('event:')) {
            eventType = line.replace('event:', '').trim();
          } else if (line.startsWith('data:')) {
            eventData = line.replace('data:', '').trim();
          }
        }

        if (eventType === 'status' && onStatus) {
          onStatus(eventData);
        } else if (eventType === 'token' && onToken) {
          onToken(eventData);
        } else if (eventType === 'complete' && onComplete) {
          try {
            const parsed = JSON.parse(eventData);
            onComplete(parsed);
          } catch {
            onComplete(eventData);
          }
        } else if (eventType === 'error' && onError) {
          onError(eventData);
        }
      }
    }
  } catch (err) {
    if (onError) onError(err.message || 'Stream connection failed');
  }
}
