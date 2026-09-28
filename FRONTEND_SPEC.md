# StockPulse · Frontend Specification & API Contract
## Reactive Merchandising Console (`FRONTEND_SPEC.md`)

This document serves as the single source of truth for developing the StockPulse Merchandising Console (React 18 / Vite). It includes exact backend JSON payloads, TypeScript interfaces, endpoint contracts, SSE token streaming protocol, UI state machines, and a turnkey frontend implementation prompt.

---

## 1. System Overview & Architecture

StockPulse connects real-time inventory signals with autonomous AI commercial decision-making:
- **Backend Port**: `http://localhost:8080` (Spring Boot 3.x with H2 database & CORS enabled for all origins)
- **Frontend Target**: `http://localhost:5173` (React 18 + Vite, modern executive dark theme)
- **Reactive Polling**: Polls pending suggestions and catalog state every `3000ms` (3 seconds) with manual refresh support.

```
[Customer / Demo Order] ---> POST /products/{id}/orders
                                     │
                      (Stock < Threshold OR Velocity Surge)
                                     ▼
                      [Agentic Event Queue (Async)]
                                     │
                 (Dual Prompt AI Advisor / Rule Fallback)
                                     ▼
                   [Pending Suggestions Queued]
                                     │
                      (3s Poll / Real-time Query)
                                     ▼
        [React Merchandising Console: Accept / Reject Checkpoint]
                                     │
                            (Merchandiser Accepts)
                                     ▼
          Atomic Update: Product Price & Stock Updated in DB
```

---

## 2. TypeScript Interfaces & Data Models

```typescript
// --- Enums ---
export type Category = 'ELECTRONICS' | 'APPAREL' | 'HOME';

export type ProductStatus = 'ACTIVE' | 'PRICE_REVIEW_PENDING' | 'OUT_OF_STOCK';

export type TriggerReason = 'INITIAL' | 'INVENTORY_LOW' | 'DEMAND_SPIKE' | 'MANUAL';

export type ChangeDirection = 'INCREASE' | 'DECREASE' | 'HOLD';

export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

// --- Domain Models ---
export interface Product {
  id: string; // e.g. "PRD-001"
  sku: string; // e.g. "SKU-ELEC-001"
  name: string; // e.g. "Wireless Earbuds Pro"
  category: Category;
  currentPrice: number; // e.g. 79.99
  stockLevel: number; // e.g. 45
  reorderThreshold: number; // e.g. 20
  demandVelocity: number; // e.g. 3 (orders in last 24h)
  status: ProductStatus;
  
  // Sprint 2 Extension Seams
  costPrice?: number | null; // e.g. 35.00
  supplierId?: string | null; // e.g. "SUP-TECH-01"
  competitorPrice?: number | null; // e.g. 84.50
  
  createdAt?: string;
  updatedAt?: string;
}

export interface PricingSuggestion {
  id: number;
  product: Product;
  currentPrice: number;
  recommendedPrice: number;
  changeDirection: ChangeDirection;
  confidence: number; // 0.0 to 1.0 (e.g. 0.92)
  reasoning: string;
  status: SuggestionStatus;
  triggerReason: TriggerReason;
  strategyUsed: string; // e.g. "AI_ADVISOR", "RULE_BASED", "COMPETITOR_AWARE"
  createdAt: string;
  updatedAt?: string;
}

export interface ReorderSuggestion {
  id: number;
  product: Product;
  currentStock: number;
  recommendedQuantity: number;
  suggestedLeadTimeDays: number; // e.g. 5
  confidence: number; // 0.0 to 1.0 (e.g. 0.89)
  reasoning: string;
  status: SuggestionStatus;
  triggerReason: TriggerReason;
  strategyUsed: string;
  createdAt: string;
  updatedAt?: string;
}

export interface StrategyConfig {
  activePricingStrategy: string; // "RULE_BASED" | "AI" | "COMPETITOR_AWARE"
  activeReorderStrategy: string; // "RULE_BASED" | "AI"
  availablePricingStrategies: string[];
  availableReorderStrategies: string[];
}
```

---

## 3. Exhaustive Backend API Specification & Sample Payloads

### 3.1 Products Catalog (`/products`)

#### `GET /products`
Optional query parameters:
- `status`: `ACTIVE`, `PRICE_REVIEW_PENDING`, `OUT_OF_STOCK`
- `category`: `ELECTRONICS`, `APPAREL`, `HOME`

**Sample Response (`200 OK`)**:
```json
[
  {
    "id": "PRD-001",
    "sku": "SKU-ELEC-001",
    "name": "Wireless Earbuds Pro",
    "category": "ELECTRONICS",
    "currentPrice": 79.99,
    "stockLevel": 45,
    "reorderThreshold": 20,
    "demandVelocity": 3,
    "status": "ACTIVE",
    "costPrice": 35.00,
    "supplierId": "SUP-TECH-01",
    "competitorPrice": 84.50,
    "createdAt": "2026-09-28T19:39:39.454",
    "updatedAt": "2026-09-28T19:39:39.454"
  },
  {
    "id": "PRD-003",
    "sku": "SKU-APP-001",
    "name": "Organic Cotton T-Shirt",
    "category": "APPAREL",
    "currentPrice": 24.99,
    "stockLevel": 8,
    "reorderThreshold": 15,
    "demandVelocity": 12,
    "status": "PRICE_REVIEW_PENDING",
    "costPrice": 9.20,
    "supplierId": "SUP-TEXTILE-01",
    "competitorPrice": 26.00,
    "createdAt": "2026-09-28T19:39:39.454",
    "updatedAt": "2026-09-28T19:39:39.454"
  }
]
```

#### `GET /products/{id}`
**Sample Response (`200 OK`)**:
```json
{
  "id": "PRD-003",
  "sku": "SKU-APP-001",
  "name": "Organic Cotton T-Shirt",
  "category": "APPAREL",
  "currentPrice": 24.99,
  "stockLevel": 8,
  "reorderThreshold": 15,
  "demandVelocity": 12,
  "status": "PRICE_REVIEW_PENDING",
  "costPrice": 9.20,
  "supplierId": "SUP-TEXTILE-01",
  "competitorPrice": 26.00
}
```

#### `PATCH /products/{id}/stock`
Updates manual stock count. If new stock < `reorderThreshold`, automatically triggers agentic low inventory loop.
**Sample Request**:
```json
{
  "stockLevel": 10
}
```
**Sample Response (`200 OK`)**:
```json
{
  "id": "PRD-001",
  "sku": "SKU-ELEC-001",
  "name": "Wireless Earbuds Pro",
  "category": "ELECTRONICS",
  "currentPrice": 79.99,
  "stockLevel": 10,
  "reorderThreshold": 20,
  "demandVelocity": 3,
  "status": "ACTIVE"
}
```

#### `POST /products/{id}/orders`
Simulates a customer order: decrements `stockLevel` and increments `demandVelocity`.
Triggers Trigger A (Low Stock) if `stockLevel < reorderThreshold` and Trigger B (Demand Spike) if `velocity >= 2.5x peer average` or `velocity >= 10`.
**Sample Request**:
```json
{
  "quantity": 2
}
```
**Sample Response (`200 OK`)**:
```json
{
  "id": "PRD-008",
  "sku": "SKU-APP-003",
  "name": "Hoodie — Heather Grey",
  "category": "APPAREL",
  "currentPrice": 54.99,
  "stockLevel": 9,
  "reorderThreshold": 12,
  "demandVelocity": 17,
  "status": "ACTIVE"
}
```

#### `POST /products/{id}/suggest-pricing`
On-demand pricing consultation using active strategy.
**Sample Response (`200 OK`)**:
```json
{
  "id": 12,
  "product": { "id": "PRD-008", "sku": "SKU-APP-003", ... },
  "currentPrice": 54.99,
  "recommendedPrice": 62.99,
  "changeDirection": "INCREASE",
  "confidence": 0.94,
  "reasoning": "AI Model (Gemini Flash Advisor): Demand velocity spiked 3.5x over category peers. High conversion velocity suggests low price elasticity. Recommending a +14.5% price increase.",
  "status": "PENDING",
  "triggerReason": "MANUAL",
  "strategyUsed": "AI_ADVISOR",
  "createdAt": "2026-09-28T19:42:01.120"
}
```

#### `POST /products/{id}/suggest-reorder`
On-demand replenishment consultation using active strategy.
**Sample Response (`200 OK`)**:
```json
{
  "id": 14,
  "product": { "id": "PRD-008", "sku": "SKU-APP-003", ... },
  "currentStock": 9,
  "recommendedQuantity": 48,
  "suggestedLeadTimeDays": 5,
  "confidence": 0.91,
  "reasoning": "AI Replenishment: Current velocity will deplete remaining inventory within 18 hours. Urgent reorder of 48 units required.",
  "status": "PENDING",
  "triggerReason": "MANUAL",
  "strategyUsed": "AI_ADVISOR",
  "createdAt": "2026-09-28T19:42:01.150"
}
```

---

### 3.2 Suggestions Management (`/pricing-suggestions` & `/reorder-suggestions`)

#### `GET /pricing-suggestions`
Optional query parameters:
- `productId`: e.g. "PRD-003"
- `status`: `PENDING`, `ACCEPTED`, `REJECTED`
- `triggerReason`: `INVENTORY_LOW`, `DEMAND_SPIKE`, `MANUAL`

**Sample Response (`200 OK`)**:
```json
[
  {
    "id": 1,
    "product": {
      "id": "PRD-003",
      "sku": "SKU-APP-001",
      "name": "Organic Cotton T-Shirt",
      "category": "APPAREL",
      "currentPrice": 24.99,
      "stockLevel": 8,
      "reorderThreshold": 15,
      "demandVelocity": 12,
      "status": "PRICE_REVIEW_PENDING"
    },
    "currentPrice": 24.99,
    "recommendedPrice": 27.49,
    "changeDirection": "INCREASE",
    "confidence": 0.92,
    "reasoning": "Stock (8 units) is critically below reorder threshold (15) with high demand velocity (12 orders/24h). Recommend +10% price protection to preserve margin while reorder is underway.",
    "status": "PENDING",
    "triggerReason": "INVENTORY_LOW",
    "strategyUsed": "AI_ADVISOR",
    "createdAt": "2026-09-28T19:39:39.632"
  }
]
```

#### `PATCH /pricing-suggestions/{id}`
**Sample Request**:
```json
{
  "status": "ACCEPTED"
}
```
*Note: Accepting atomically updates `Product.currentPrice` to `recommendedPrice` and resets product status to `ACTIVE`.*

#### `GET /reorder-suggestions`
Optional query parameters: `productId`, `status`, `triggerReason`.
**Sample Response (`200 OK`)**:
```json
[
  {
    "id": 1,
    "product": {
      "id": "PRD-003",
      "sku": "SKU-APP-001",
      "name": "Organic Cotton T-Shirt",
      "category": "APPAREL",
      "currentPrice": 24.99,
      "stockLevel": 8,
      "reorderThreshold": 15,
      "demandVelocity": 12,
      "status": "PRICE_REVIEW_PENDING"
    },
    "currentStock": 8,
    "recommendedQuantity": 37,
    "suggestedLeadTimeDays": 4,
    "confidence": 0.89,
    "reasoning": "Replenishment required immediately. Current stock of 8 covers less than 16 hours of current velocity (12 units/day). Target safety stock is 45 units.",
    "status": "PENDING",
    "triggerReason": "INVENTORY_LOW",
    "strategyUsed": "AI_ADVISOR",
    "createdAt": "2026-09-28T19:39:39.632"
  }
]
```

#### `PATCH /reorder-suggestions/{id}`
**Sample Request**:
```json
{
  "status": "ACCEPTED"
}
```
*Note: Accepting atomically increments `Product.stockLevel` by `recommendedQuantity` (simulated inbound shipment).*

---

### 3.3 Strategy Configuration (`/api/config/strategy`)

#### `GET /api/config/strategy`
**Sample Response (`200 OK`)**:
```json
{
  "activePricingStrategy": "RULE_BASED",
  "activeReorderStrategy": "RULE_BASED",
  "availablePricingStrategies": ["RULE_BASED", "AI", "COMPETITOR_AWARE"],
  "availableReorderStrategies": ["RULE_BASED", "AI"]
}
```

#### `POST /api/config/strategy`
Switches active strategies in real-time without restart.
**Sample Request**:
```json
{
  "pricingStrategy": "AI",
  "reorderStrategy": "AI"
}
```

---

### 3.4 SSE Reasoning Stream (`POST /products/{id}/suggest-pricing/stream`)

Streams real-time thinking tokens before final suggestion is persisted.
**Header**: `Accept: text/event-stream`
**Stream format**:
```
event: status
data: Initializing StockPulse AI Commerce Advisor for SKU SKU-APP-001...

event: token
data: Evaluating 

event: token
data: SKU 

event: token
data: 'Organic Cotton T-Shirt' 

...

event: complete
data: {"id":15,"product":{"id":"PRD-003",...},"recommendedPrice":27.99,...}
```

---

## 4. UI Design & Component Hierarchy

### Theme & Palette
- **Background**: `#0b0f19` (Deep Slate / Dark Navy)
- **Cards**: `#111827` with `border: 1px solid rgba(255, 255, 255, 0.08)` and subtle backdrop blur
- **Accents**:
  - `INVENTORY_LOW`: Amber/Orange `#f59e0b` (`bg-amber-500/10 text-amber-400 border-amber-500/20`)
  - `DEMAND_SPIKE`: Neon Purple/Rose `#ec4899` (`bg-pink-500/10 text-pink-400 border-pink-500/20`)
  - `MANUAL`: Cyan `#06b6d4` (`bg-cyan-500/10 text-cyan-400 border-cyan-500/20`)
  - `SUCCESS / ACCEPT`: Emerald `#10b981`
  - `REJECT`: Crimson `#ef4444`

### Layout Structure
1. **Executive Navbar**:
   - Logo: **StockPulse** with live pulsating green radar dot
   - Real-time indicator ("System Live · 3s Polling") with manual refresh button
   - Strategy Switcher Pill: Toggle between `RULE_BASED`, `AI`, and `COMPETITOR_AWARE`
2. **Metrics Bar**:
   - `4 Cards`: "Pending Actions", "Low Stock Triggers", "Demand Spikes", "Catalog SKUs"
3. **Merchandising Approval Desk (Floor Requirement)**:
   - Lists all `PENDING` suggestions (both Pricing and Reorder) grouped by SKU.
   - Shows: Trigger Badge (`INVENTORY_LOW` vs `DEMAND_SPIKE`), Current Price vs Proposed Price (with % diff), Reorder Units + Lead time, Confidence meter bar, AI Reasoning explanation box.
   - Action controls: "Accept Proposal" and "Reject".
4. **Interactive Catalog Board (Ceiling Requirement)**:
   - Filter pills: All, Electronics, Apparel, Home, Low Stock Only.
   - Table columns:
     - SKU & Name
     - Category
     - Stock Level (Color-coded progress: Red if `< threshold`, Green if healthy)
     - Reorder Threshold
     - Velocity (Orders/24h) (Flame badge if `velocity > 8`)
     - Price (with Sprint 2 Cost & Margin tooltip)
     - Status (`ACTIVE`, `PRICE_REVIEW_PENDING`, `OUT_OF_STOCK`)
     - Quick Demo Actions:
       - `🛒 -1 Sale`: Triggers instant sale demo
       - `🔥 +5 Viral Spike`: Triggers instant viral surge demo
       - `📦 Restock +20`: Quick stock replenishment
       - `⚡ Stream AI`: Opens SSE reasoning stream modal
5. **SSE Live Reasoning Stream Modal**:
   - Displays real-time streaming tokens with typewriter animation, status log, and instant suggestion creation.
6. **Sprint 2 Extension Drawer**:
   - Shows Wholesale Cost (`costPrice`), Supplier ID (`supplierId`), and Tracked Competitor Price (`competitorPrice`).

---

## 5. Ready-to-Use Frontend Developer Prompt

Below is the turnkey prompt for building the complete React 18 frontend:

```text
Build the StockPulse Merchandising Console using React 18 and Vite.
Connect to the Spring Boot backend at http://localhost:8080.

Requirements:
1. Executive Dark Mode Design System:
   - Deep navy/charcoal background (#0a0e17) with frosted glass cards (#111827).
   - High-contrast typography with Inter/system font.
   - Badges: Amber for INVENTORY_LOW, Fuchsia for DEMAND_SPIKE, Cyan for MANUAL.

2. State Management & Real-Time Loop:
   - Poll GET /products, GET /pricing-suggestions?status=PENDING, and GET /reorder-suggestions?status=PENDING every 3000ms.
   - Provide manual "Refresh Now" button with spinning icon.
   - GET /api/config/strategy and allow switching strategies via dropdown or segmented control (RULE_BASED, AI, COMPETITOR_AWARE).

3. Merchandising Action Desk:
   - Prominently display all pending suggestions.
   - For Pricing: Show current price -> recommended price, % delta, confidence progress bar, trigger reason badge, strategy used badge, and plain-English AI reasoning.
   - For Reorder: Show current stock, recommended reorder quantity, lead time days, confidence, and reasoning.
   - Wire Accept (PATCH /pricing-suggestions/{id} or /reorder-suggestions/{id} with status=ACCEPTED) and Reject buttons.

4. Interactive Catalog Table:
   - Category filtering (ALL, ELECTRONICS, APPAREL, HOME) and Status filtering.
   - Show stock vs threshold progress bar.
   - One-click demo triggers on each SKU row:
     - "Buy 1" (POST /products/{id}/orders with {quantity: 1})
     - "Spike +5" (POST /products/{id}/orders with {quantity: 5})
     - "Stream AI Reasoning" (POST /products/{id}/suggest-pricing/stream via EventSource or fetch ReadableStream)
   - Product detail modal showing Sprint 2 extension fields: costPrice, supplierId, competitorPrice, and gross margin %.

5. SSE Token Streaming Drawer/Modal:
   - Live stream tokens as they arrive from POST /products/{id}/suggest-pricing/stream.
   - Animate the tokens in real time and display the finalized persisted suggestion when stream completes.
```
