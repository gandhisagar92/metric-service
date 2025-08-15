# Financial Data Relationship Explorer - Mock Graph API

Run server:

```
pip install -r requirements.txt
python -m server.app
```

## Endpoints

- GET /schema
- PUT /schema
- GET /schema/node-types
- POST /schema/node-types
- GET /schema/node-types/{name}
- PUT /schema/node-types/{name}
- DELETE /schema/node-types/{name}?cascade=true|false

- GET /schema/relationship-types
- POST /schema/relationship-types
- GET /schema/relationship-types/{name}
- PUT /schema/relationship-types/{name}
- DELETE /schema/relationship-types/{name}?cascade=true|false

- GET /nodes?type=STOCK&labelContains=ibm&limit=50&offset=0
- POST /nodes { id?, type, label?, attributes }
- GET /nodes/{id}
- PUT /nodes/{id} { label?, attributes? }
- DELETE /nodes/{id}?cascade=true|false

- GET /relationships?type=HAS_OPTION&sourceId=STK:IBM&targetId=OPT:...&limit=50&offset=0
- POST /relationships { id?, type, source, target, attributes }
- GET /relationships/{id}
- PUT /relationships/{id} { type?, source?, target?, attributes? }
- DELETE /relationships/{id}

Data stored in `/workspace/data/{schema,nodes,edges}.json`.

## Sample requests: ETF schema, data, and graph

### Create node types

```bash
curl -X POST http://localhost:8080/schema/node-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ETFInstrument",
    "label": "ETF Instrument",
    "attributes": {
      "InstrumentId": { "type": "string", "required": true },
      "ISIN": { "type": "string", "required": true }
    }
  }'
```

```bash
curl -X POST http://localhost:8080/schema/node-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ETFTradingLine",
    "label": "ETF Trading Line",
    "attributes": {
      "TradingLineId": { "type": "string", "required": true },
      "RIC": { "type": "string", "required": true },
      "BloombergTicker": { "type": "string", "required": true }
    }
  }'
```

```bash
curl -X POST http://localhost:8080/schema/node-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ETFExchange",
    "label": "ETF Exchange",
    "attributes": {
      "MIC": { "type": "string", "required": true }
    }
  }'
```

```bash
curl -X POST http://localhost:8080/schema/node-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ETFBasket",
    "label": "ETF Basket",
    "attributes": {
      "BasketId": { "type": "string", "required": true }
    }
  }'
```

```bash
curl -X POST http://localhost:8080/schema/node-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ETFRedemptionBasket",
    "label": "ETF Redemption Basket",
    "attributes": {
      "IndexInstrumentId": { "type": "string", "required": true }
    }
  }'
```

```bash
curl -X POST http://localhost:8080/schema/node-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ETFCreationBasket",
    "label": "ETF Creation Basket",
    "attributes": {
      "IndexInstrumentId": { "type": "string", "required": true }
    }
  }'
```

```bash
curl -X POST http://localhost:8080/schema/node-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ETFCalculationBasket",
    "label": "ETF Calculation Basket",
    "attributes": {
      "IndexInstrumentId": { "type": "string", "required": true }
    }
  }'
```

### Create relationship types

```bash
curl -X POST http://localhost:8080/schema/relationship-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "LISTED_ON_TRADING_LINE",
    "label": "LISTED_ON_TRADING_LINE",
    "attributes": {},
    "sourceTypes": ["ETFInstrument"],
    "targetTypes": ["ETFTradingLine"]
  }'
```

```bash
curl -X POST http://localhost:8080/schema/relationship-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "LISTED_ON_EXCHANGE",
    "label": "LISTED_ON_EXCHANGE",
    "attributes": {},
    "sourceTypes": ["ETFTradingLine"],
    "targetTypes": ["ETFExchange"]
  }'
```

```bash
curl -X POST http://localhost:8080/schema/relationship-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "LINKED_TO_BASKET",
    "label": "LINKED_TO_BASKET",
    "attributes": {},
    "sourceTypes": ["ETFInstrument"],
    "targetTypes": ["ETFBasket"]
  }'
```

```bash
curl -X POST http://localhost:8080/schema/relationship-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "HAS_REDEMPTION_BASKET",
    "label": "HAS_REDEMPTION_BASKET",
    "attributes": {},
    "sourceTypes": ["ETFBasket"],
    "targetTypes": ["ETFRedemptionBasket"]
  }'
```

```bash
curl -X POST http://localhost:8080/schema/relationship-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "HAS_CREATION_BASKET",
    "label": "HAS_CREATION_BASKET",
    "attributes": {},
    "sourceTypes": ["ETFBasket"],
    "targetTypes": ["ETFCreationBasket"]
  }'
```

```bash
curl -X POST http://localhost:8080/schema/relationship-types \
  -H "Content-Type: application/json" \
  -d '{
    "name": "HAS_CALCULATION_BASKET",
    "label": "HAS_CALCULATION_BASKET",
    "attributes": {},
    "sourceTypes": ["ETFBasket"],
    "targetTypes": ["ETFCalculationBasket"]
  }'
```

### Create sample nodes

```bash
curl -X POST http://localhost:8080/nodes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ETF:SPY",
    "type": "ETFInstrument",
    "label": "SPDR S&P 500 ETF",
    "attributes": { "InstrumentId": "ETF:SPY", "ISIN": "US78462F1030" }
  }'
```

```bash
curl -X POST http://localhost:8080/nodes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ETFTL:SPY:ARCA",
    "type": "ETFTradingLine",
    "label": "SPY ARCA",
    "attributes": { "TradingLineId": "ETFTL:SPY:ARCA", "RIC": "SPY.P", "BloombergTicker": "SPY US" }
  }'
```

```bash
curl -X POST http://localhost:8080/nodes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ETFTL:SPY:LSE",
    "type": "ETFTradingLine",
    "label": "SPY LSE",
    "attributes": { "TradingLineId": "ETFTL:SPY:LSE", "RIC": "SPY.L", "BloombergTicker": "SPY LN" }
  }'
```

```bash
curl -X POST http://localhost:8080/nodes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ETFX:ARCA",
    "type": "ETFExchange",
    "label": "Cboe/NYSE Arca",
    "attributes": { "MIC": "ARCX" }
  }'
```

```bash
curl -X POST http://localhost:8080/nodes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ETFX:LSE",
    "type": "ETFExchange",
    "label": "London Stock Exchange",
    "attributes": { "MIC": "XLON" }
  }'
```

```bash
curl -X POST http://localhost:8080/nodes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ETFB:SPY",
    "type": "ETFBasket",
    "label": "SPY Basket",
    "attributes": { "BasketId": "SPY-BASKET-001" }
  }'
```

```bash
curl -X POST http://localhost:8080/nodes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ETFRB:SPY:RED",
    "type": "ETFRedemptionBasket",
    "label": "SPY Redemption Basket",
    "attributes": { "IndexInstrumentId": "IDX:SPX" }
  }'
```

```bash
curl -X POST http://localhost:8080/nodes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ETFCB:SPY:CRE",
    "type": "ETFCreationBasket",
    "label": "SPY Creation Basket",
    "attributes": { "IndexInstrumentId": "IDX:SPX" }
  }'
```

```bash
curl -X POST http://localhost:8080/nodes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "ETFCALB:SPY:CALC",
    "type": "ETFCalculationBasket",
    "label": "SPY Calculation Basket",
    "attributes": { "IndexInstrumentId": "IDX:SPX" }
  }'
```

### Create relationships

```bash
curl -X POST http://localhost:8080/relationships \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LISTED_ON_TRADING_LINE",
    "source": "ETF:SPY",
    "target": "ETFTL:SPY:ARCA",
    "attributes": {}
  }'
```

```bash
curl -X POST http://localhost:8080/relationships \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LISTED_ON_TRADING_LINE",
    "source": "ETF:SPY",
    "target": "ETFTL:SPY:LSE",
    "attributes": {}
  }'
```

```bash
curl -X POST http://localhost:8080/relationships \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LISTED_ON_EXCHANGE",
    "source": "ETFTL:SPY:ARCA",
    "target": "ETFX:ARCA",
    "attributes": {}
  }'
```

```bash
curl -X POST http://localhost:8080/relationships \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LISTED_ON_EXCHANGE",
    "source": "ETFTL:SPY:LSE",
    "target": "ETFX:LSE",
    "attributes": {}
  }'
```

```bash
curl -X POST http://localhost:8080/relationships \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LINKED_TO_BASKET",
    "source": "ETF:SPY",
    "target": "ETFB:SPY",
    "attributes": {}
  }'
```

```bash
curl -X POST http://localhost:8080/relationships \
  -H "Content-Type: application/json" \
  -d '{
    "type": "HAS_REDEMPTION_BASKET",
    "source": "ETFB:SPY",
    "target": "ETFRB:SPY:RED",
    "attributes": {}
  }'
```

```bash
curl -X POST http://localhost:8080/relationships \
  -H "Content-Type: application/json" \
  -d '{
    "type": "HAS_CREATION_BASKET",
    "source": "ETFB:SPY",
    "target": "ETFCB:SPY:CRE",
    "attributes": {}
  }'
```

```bash
curl -X POST http://localhost:8080/relationships \
  -H "Content-Type: application/json" \
  -d '{
    "type": "HAS_CALCULATION_BASKET",
    "source": "ETFB:SPY",
    "target": "ETFCALB:SPY:CALC",
    "attributes": {}
  }'
```

### Graph requests

```bash
curl -X POST http://localhost:8080/graph/search \
  -H "Content-Type: application/json" \
  -d '{
    "referenceDataType": "ETFInstrument",
    "queryBy": "ISIN",
    "inputs": { "value": "US78462F1030" },
    "graphOptions": { "maxDepth": 1, "maxNeighborsPerType": 50, "includeFacets": true }
  }'
```

```bash
curl -X POST http://localhost:8080/graph/expand \
  -H "Content-Type: application/json" \
  -d '{
    "nodeId": "ETF:SPY",
    "relations": ["LISTED_ON_TRADING_LINE","LINKED_TO_BASKET"],
    "filters": [],
    "pagination": { "limit": 50 },
    "sort": { "field": "label", "direction": "asc" }
  }'
```