# Backend API

Base URL (local): `http://localhost:8080`

All prices are in **paise** (49900 = ₹499.00). Divide by 100 to display.

---

## List / search products

`GET /api/v1/products`

Returns ACTIVE products, one page at a time. DRAFT and ARCHIVED products are never returned.

| Query option | Meaning | Default |
|---|---|---|
| `q` | Search text in the product name (not case-sensitive) | none |
| `category` | Category slug, e.g. `clothing` | all categories |
| `minPrice` | Minimum price in paise. A product matches if any active variant is at or above it. | none |
| `maxPrice` | Maximum price in paise. A product matches if any active variant is at or below it. | none |
| `sort` | `name` (A–Z) or `newest` | `name` |
| `page` | Page number, starting at 0 | `0` |
| `size` | Products per page, 1 to 50 | `20` |

Example: `GET /api/v1/products?category=clothing&maxPrice=50000&page=0&size=20`

```json
{
  "items": [
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
      ],
      "variants": [
        {
          "id": "1b3e8fb2-76f9-49b0-bf85-6aed75ce1a1c",
          "sku": "TSHIRT-BLUE-M",
          "name": "Blue / M",
          "pricePaise": 49900,
          "mrpPaise": 79900,
          "inStock": true
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

| Page field | Meaning |
|---|---|
| `items` | Products on this page |
| `page` | Current page number (starts at 0) |
| `size` | Page size used |
| `totalItems` | Total matching products across all pages |
| `totalPages` | Number of pages |

If nothing matches, `items` is `[]` and `totalItems` is `0`.

---

## Get one product

`GET /api/v1/products/{slug}`

Returns one ACTIVE product, with the same product fields as above.

If the product does not exist, or is not ACTIVE, the API returns **404**:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product not found: does-not-exist",
  "instance": "/api/v1/products/does-not-exist"
}
```

---

## Product fields

| Field | Meaning |
|---|---|
| `id` | Unique product id (UUID) |
| `slug` | URL-friendly name, used in product page links |
| `name` | Product name to display |
| `description` | Product description (can be null) |
| `images` | List of `{ url, altText }`, first image first. Empty list if none. |
| `variants` | List of buyable versions (see below). Empty list means the product cannot be bought. |

### Variant fields

A customer buys a **variant** (for example "Blue / M"), not the product itself. Only active variants are returned.

| Field | Meaning |
|---|---|
| `id` | Unique variant id (UUID). Used later for cart and orders. |
| `sku` | Stock code, unique per variant |
| `name` | Variant label to display, e.g. "Blue / M" |
| `pricePaise` | Selling price in paise |
| `mrpPaise` | MRP in paise, shown crossed out. Can be `null`. |
| `inStock` | `true` if it can be bought, `false` if out of stock |

---

## List categories

`GET /api/v1/categories`

Returns all categories, sorted by `sort_order` and then by name.

```json
[
  { "id": "5e515fce-faa2-41d1-a0f7-280baf080378", "slug": "clothing", "name": "Clothing" },
  { "id": "b8482418-84fe-4f9b-b916-ce1d7c6da22c", "slug": "kitchen", "name": "Kitchen" }
]
```

---

## List products in a category

`GET /api/v1/categories/{slug}/products`

Returns all ACTIVE products in one category, sorted by name, as a plain list (no paging). Each product has the fields described in **Product fields**.

For paging and filters inside a category, use `GET /api/v1/products?category={slug}` instead.

If the category does not exist, the API returns **404** with `"detail": "Category not found: {slug}"`.