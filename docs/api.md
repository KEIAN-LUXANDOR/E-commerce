\# Backend API



Base address (local): http://localhost:8080



\## List products



GET /api/v1/products



Returns all products that are on sale, sorted by name. Draft and archived products are not included.



Example response:


json

\[

&#x20; {

&#x20;   "id": "a793d09e-b438-4877-9339-3e840221aa7e",

&#x20;   "slug": "cotton-tshirt",

&#x20;   "name": "Cotton T-shirt",

&#x20;   "description": "100% cotton, regular fit"

&#x20; }

]




| Field | Type | Notes |

|---|---|---|

| id | text (UUID) | Unique product ID |

| slug | text | Readable web address, use it in page URLs |

| name | text | Product name |

| description | text or null | May be empty |

## Get product details

**GET** `/api/v1/products/{slug}`

Returns the details of one active product using its unique slug.

**Path parameter**

| Parameter | Type | Description |
|---|---|---|
| `slug` | string | Unique, readable product identifier |

**Successful response — `200 OK`**

Example:

json
{
  "id": "a793d09e-b438-4877-9339-3e840221aa7e",
  "slug": "cotton-tshirt",
  "name": "Cotton T-shirt",
  "description": "100% cotton, regular fit"
}

**Error response — `404 Not Found`**

Returned when the product does not exist or is not active.

Example:

json
{
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Product not found: invalid-slug"
}



# Backend API

Base URL (local): `http://localhost:8080`

## List products

GET /api/v1/products`

Returns all ACTIVE products, sorted by name. DRAFT and ARCHIVED products are hidden.

son
[
  {
    "id": "a793d09e-b438-4877-9339-3e840221aa7e",
    "slug": "cotton-tshirt",
    "name": "Cotton T-shirt",
    "description": "100% cotton, regular fit",
    "images": [
      {
        "url": "http://localhost:8080/images/cotton-tshirt.webp",
        "altText": "Cotton T-Shirt front"
      }
    ]
  },
  {
    "id": "bcd4d03a-c317-4bbf-a5be-ea107fa9e2d4",
    "slug": "steel-bottle",
    "name": "Steel Water Bottle",
    "description": "1 litre, keeps water cold",
    "images": []
  }
]


| Field | Meaning |
|---|---|
| `id` | Unique product id (UUID) |
| `slug` | URL-friendly name, used in product page links |
| `name` | Product name to display |
| `description` | Product description (can be null) |
| `images` | List of `{ url, altText }`, first image first. Empty list if the product has no images. |

## Get one product

`GET /api/v1/products/{slug}`

Returns one ACTIVE product by its slug, with the same fields as above.

Example: `GET /api/v1/products/cotton-tshirt`

json
{
  "id": "a793d09e-b438-4877-9339-3e840221aa7e",
  "slug": "cotton-tshirt",
  "name": "Cotton T-shirt",
  "description": "100% cotton, regular fit",
  "images": [
    {
      "url": "http://localhost:8080/images/cotton-tshirt.webp",
      "altText": "Cotton T-Shirt front"
    }
  ]
}


If the product does not exist, or is not ACTIVE, the API returns **404**:

json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product not found: does-not-exist",
  "instance": "/api/v1/products/does-not-exist"
}


## List categories

`GET /api/v1/categories`

Returns all categories, sorted by `sort_order` and then by name. Used for the category menu.

json
[
  { "id": "5e515fce-faa2-41d1-a0f7-280baf080378", "slug": "clothing", "name": "Clothing" },
  { "id": "b8482418-84fe-4f9b-b916-ce1d7c6da22c", "slug": "kitchen", "name": "Kitchen" }
]


| Field | Meaning |
|---|---|
| `id` | Unique category id (UUID) |
| `slug` | URL-friendly name, used in category page links |
| `name` | Category name to display |

## List products in a category

`GET /api/v1/categories/{slug}/products`

Returns the ACTIVE products in one category, sorted by name. Each product has the same fields as in **List products** (including `images`).

Example: `GET /api/v1/categories/clothing/products`

json
[
  {
    "id": "a793d09e-b438-4877-9339-3e840221aa7e",
    "slug": "cotton-tshirt",
    "name": "Cotton T-shirt",
    "description": "100% cotton, regular fit",
    "images": [
      {
        "url": "http://localhost:8080/images/cotton-tshirt.webp",
        "altText": "Cotton T-Shirt front"
      }
    ]
  }
]

If the category exists but has no active products, the API returns an empty list `[]`.

If the category does not exist, the API returns **404**:

json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Category not found: toys",
  "instance": "/api/v1/categories/toys/products"
}
