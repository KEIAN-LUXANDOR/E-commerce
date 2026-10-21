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











## Auth

### POST /api/v1/auth/register
Creates a customer account. Every new account gets the role CUSTOMER. Nobody can register as ADMIN.

**Request body**
| Field | Type | Required | Rules |
|---|---|---|---|
| email | string | yes | Valid email, max 255. Spaces removed, stored in lower case |
| password | string | yes | 8 to 72 characters |
| fullName | string | yes | Max 100 characters |
| phone | string | no | 10-digit Indian mobile number |

Example:
```json
{
  "email": "riya@example.com",
  "password": "Secret@123",
  "fullName": "Riya Shah",
  "phone": "9876543210"
}
```

**201 Created**
```json
{
  "id": "4b1f6c2e-...",
  "email": "riya@example.com",
  "fullName": "Riya Shah",
  "phone": "9876543210",
  "role": "CUSTOMER",
  "emailVerified": false,
  "createdAt": "2026-10-19T09:15:30Z"
}
```

**400 Bad Request** (invalid input; lists every wrong field)
```json
{
  "status": 400,
  "title": "Invalid Input",
  "detail": "One or more fields are invalid.",
  "errors": [
    { "field": "email", "message": "Enter a valid email address" },
    { "field": "password", "message": "Password must be 8 to 72 characters" }
  ]
}
```
A broken JSON body also returns 400 with the detail "The request body is missing or is not valid JSON."

**409 Conflict** (email already registered; capital letters are ignored)
```json
{
  "status": 409,
  "title": "Conflict",
  "detail": "An account with this email already exists."
}
```


### How login works (for the website)
- Login uses a session cookie (`JSESSIONID`). Every `fetch` must use `credentials: "include"`.
- For every POST, PUT, PATCH or DELETE **except** register and login, first call `GET /api/v1/auth/csrf`
  and send the returned token in the `X-CSRF-TOKEN` header. Get a new token after logging in.
- Allowed website address locally: `http://localhost:3000`.

### POST /api/v1/auth/login
No CSRF token needed.

**Request body**
| Field | Type | Required |
|---|---|---|
| email | string | yes |
| password | string | yes |

**200 OK**: same body as register's 201 response, and a session cookie is set.

**401 Unauthorized**: `"detail": "Invalid email or password."` (same message for unknown email and wrong password)

**429 Too Many Requests**: after 5 wrong passwords for one email, it is locked for 15 minutes.

### GET /api/v1/auth/me
Returns the logged-in user (same body as register). **401** if not logged in.

### GET /api/v1/auth/csrf
```json
{ "headerName": "X-CSRF-TOKEN", "token": "..." }
```

### POST /api/v1/auth/logout
Needs the `X-CSRF-TOKEN` header. Returns **204 No Content** and clears the session cookie.

### Access rules
| Who | Can use |
|---|---|
| Anyone | GET products, categories, images; register; login; csrf |
| Logged-in user | me, logout (and later cart, orders, addresses) |
| ADMIN only | Everything under `/api/v1/admin/` |

**401 Unauthorized**: not logged in.
**403 Forbidden**: missing CSRF token, or a customer calling an admin API.



### Email verification and password reset
Rule: customers can log in before verifying, but must verify their email before placing an order.
None of these need a CSRF token or login.

| Method | Address | Body | Reply |
|---|---|---|---|
| POST | /api/v1/auth/verify-email | `{ "token": "..." }` | 200, or 400 if the link is invalid/expired/used |
| POST | /api/v1/auth/resend-verification | `{ "email": "..." }` | Always 202 with the same message |
| POST | /api/v1/auth/forgot-password | `{ "email": "..." }` | Always 202 with the same message |
| POST | /api/v1/auth/reset-password | `{ "token": "...", "newPassword": "..." }` | 200, or 400 if the link is invalid/expired/used |

Replies look like: `{ "message": "Your email address is verified." }`

**Pages the website needs:**
- `/verify-email?token=...`: read `token` from the address, send it to `/api/v1/auth/verify-email`, show the message.
- `/reset-password?token=...`: show a "new password" form, then send `token` and `newPassword` to `/api/v1/auth/reset-password`.
- A "Resend verification email" button and a "Forgot password?" link on the login page.

Links: verification valid 24 hours, reset valid 30 minutes, each usable once.
At most one resend / forgot-password email per address per minute.

**Local email testing:** run Mailpit once with
`docker run -d --name mailpit --restart unless-stopped -p 8025:8025 -p 1025:1025 axllent/mailpit`
then open http://localhost:8025 to read the emails.