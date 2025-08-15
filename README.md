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