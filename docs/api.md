\# Backend API



Base address (local): http://localhost:8080



\## List products



GET /api/v1/products



Returns all products that are on sale, sorted by name. Draft and archived products are not included.



Example response:



```json

\[

&#x20; {

&#x20;   "id": "a793d09e-b438-4877-9339-3e840221aa7e",

&#x20;   "slug": "cotton-tshirt",

&#x20;   "name": "Cotton T-shirt",

&#x20;   "description": "100% cotton, regular fit"

&#x20; }

]

```



| Field | Type | Notes |

|---|---|---|

| id | text (UUID) | Unique product ID |

| slug | text | Readable web address, use it in page URLs |

| name | text | Product name |

| description | text or null | May be empty |

