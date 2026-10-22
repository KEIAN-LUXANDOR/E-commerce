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




# Customer Delivery Addresses API

Base URL: `http://localhost:8080`

All endpoints below use the `/api/v1/account/addresses` base path.

These endpoints allow authenticated customers to manage their delivery addresses. Customers can only manage their own addresses.

## 1. List Delivery Addresses

**Endpoint:** `GET /api/v1/account/addresses`

Returns the delivery addresses belonging to the logged-in customer.

**Authentication:** Required

**Success response:** `200 OK`

```json
[
  {
    "id": "5bf63674-4813-4d92-9ceb-c7f56b067503",
    "fullName": "Riya Shah",
    "phone": "9876543210",
    "line1": "10 Koregaon Park",
    "line2": "Near Main Road",
    "landmark": "Koregaon Park",
    "city": "Pune",
    "state": "Maharashtra",
    "pincode": "411001",
    "defaultAddress": true
  }
]
```

If the customer has no saved addresses, the API returns `200 OK` with an empty array:

```json
[]
```

## 2. Create Delivery Address

**Endpoint:** `POST /api/v1/account/addresses`

Creates a new delivery address for the logged-in customer.

**Authentication:** Required

**Request body:**

```json
{
  "fullName": "Riya Shah",
  "phone": "9876543210",
  "line1": "123 MG Road",
  "line2": "Near City Mall",
  "landmark": "Near City Mall",
  "city": "Pune",
  "state": "Maharashtra",
  "pincode": "411001"
}
```

| Field | Type | Required |
|---|---|---|
| `fullName` | String | Yes |
| `phone` | String | Yes |
| `line1` | String | Yes |
| `line2` | String | No |
| `landmark` | String | No |
| `city` | String | Yes |
| `state` | String | Yes |
| `pincode` | String | Yes |

**Success response:** `201 Created`

Returns the newly created address, including its generated `id` and `defaultAddress` status.

The first address is automatically marked as the default. Subsequent addresses are not automatically made default.

## 3. Get Indian States and Union Territories

**Endpoint:** `GET /api/v1/account/addresses/states`

Returns the supported Indian states and union territories.

**Authentication:** Required

**Success response:** `200 OK`

```json
[
  "Andhra Pradesh",
  "Arunachal Pradesh",
  "Assam",
  "Bihar",
  "Chhattisgarh",
  "Goa",
  "Gujarat",
  "Haryana",
  "Himachal Pradesh",
  "Jharkhand",
  "Karnataka",
  "Kerala",
  "Madhya Pradesh",
  "Maharashtra",
  "Manipur",
  "Meghalaya",
  "Mizoram",
  "Nagaland",
  "Odisha",
  "Punjab",
  "Rajasthan",
  "Sikkim",
  "Tamil Nadu",
  "Telangana",
  "Tripura",
  "Uttar Pradesh",
  "Uttarakhand",
  "West Bengal",
  "Andaman and Nicobar Islands",
  "Chandigarh",
  "Dadra and Nagar Haveli and Daman and Diu",
  "Delhi",
  "Jammu and Kashmir",
  "Ladakh",
  "Lakshadweep",
  "Puducherry"
]
```

## 4. Update Delivery Address

**Endpoint:** `PUT /api/v1/account/addresses/{id}`

Updates an existing address belonging to the logged-in customer.

**Authentication:** Required

**Path parameter:**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | ID of the address to update |

**Request body:**

```json
{
  "fullName": "Riya Shah",
  "phone": "9876543210",
  "line1": "456 FC Road",
  "line2": "Near College",
  "landmark": "Central Pune",
  "city": "Pune",
  "state": "Maharashtra",
  "pincode": "411004"
}
```

**Success response:** `200 OK`

Returns the updated address.

Updating an address does not change its default status.

## 5. Set Default Delivery Address

**Endpoint:** `PUT /api/v1/account/addresses/{id}/default`

Marks the specified address as the default delivery address for the logged-in customer.

**Authentication:** Required

**Path parameter:**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | ID of the address to make default |

**Success response:** `200 OK`

Returns the updated address with:

```json
{
  "id": "5bf63674-4813-4d92-9ceb-c7f56b067503",
  "defaultAddress": true
}
```

When the default address changes, the previously selected address is no longer the default.

## 6. Delete Delivery Address

**Endpoint:** `DELETE /api/v1/account/addresses/{id}`

Deletes an address belonging to the logged-in customer.

**Authentication:** Required

**Path parameter:**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | ID of the address to delete |

**Success response:** `204 No Content`

The response has no body. A subsequent request to list addresses can verify that the address was removed.

## Authentication and Security

- A customer must be logged in to access these endpoints.
- Requests that modify data must include a valid CSRF token if required by the application's security configuration.
- The CSRF token is sent using the `X-CSRF-TOKEN` header.
- Address access must be restricted to the authenticated customer's own records.
- Unauthenticated requests return `401 Unauthorized`.

**Example unauthorized response:**

```json
{
  "status": 401,
  "title": "Unauthorized",
  "detail": "Please log in to continue."
}
```

## Database Migration

The addresses feature introduces Flyway migration:

`backend/src/main/resources/db/migration/V7__create_addresses.sql`

The migration creates the `addresses` table used to store customer delivery addresses.

## Testing Status

The following operations have been manually tested successfully:

- Creating a delivery address.
- Listing saved addresses.
- Retrieving Indian states and union territories.
- Updating an address.
- Changing the default address.
- Deleting a non-default address.
- Deleting the default address.
- Returning an empty list after all addresses are deleted.

Cross-customer authorization and the full automated test suite should also be verified before production release.





## Wishlist
All need login. PUT and DELETE also need the `X-CSRF-TOKEN` header.

| Method | Address | Reply |
|---|---|---|
| GET | /api/v1/account/wishlist | 200, list of `{ product, addedAt }`, newest first |
| GET | /api/v1/account/wishlist/ids | 200, list of product ids (for heart icons) |
| PUT | /api/v1/account/wishlist/{productId} | 204. Adding the same product twice does nothing |
| DELETE | /api/v1/account/wishlist/{productId} | 204. Also 204 if it wasn't in the wishlist |

`product` has the same fields as the product API (id, slug, name, description, images, variants).

```json
[
  {
    "product": { "id": "…", "slug": "steel-bottle", "name": "Steel Water Bottle", "description": "…", "images": [], "variants": [ … ] },
    "addedAt": "2026-10-22T04:15:30Z"
  }
]
```

Rules: only ACTIVE products can be added (**404** otherwise); products that are later archived disappear from the wishlist; maximum 100 products (**400**).





## Profile and password
All need login. PUT and POST also need the `X-CSRF-TOKEN` header.

### PUT /api/v1/account/profile
| Field | Required | Rules |
|---|---|---|
| fullName | yes | Max 100 |
| phone | no | 10-digit Indian mobile number. Empty or missing removes it |

**200**: same body as `/auth/me`. **400** for invalid fields.
Email can't be changed here.

### POST /api/v1/account/change-password
```json
{ "currentPassword": "...", "newPassword": "..." }
```
- **200** `{ "message": "Your password has been changed." }`. The customer stays logged in, and a "Your password was changed" email is sent.
- **400**: current password wrong, new password the same as the current one, or not 8 to 72 characters.
- **429**: after 5 wrong current passwords (same lock as login, 15 minutes).

## Checkout eligibility
### GET /api/v1/account/checkout-eligibility
Call before showing checkout.
- **200** `{ "canCheckout": true }`
- **403** when the email isn't verified:
```json
{ "status": 403, "title": "Forbidden", "detail": "Please verify your email address before placing an order.", "code": "EMAIL_NOT_VERIFIED" }
```
On `EMAIL_NOT_VERIFIED`, show a message with a "Resend verification email" button (`POST /api/v1/auth/resend-verification`).
Placing an order will do the same check on the server.

**Note:** for POST, PUT and DELETE, a request without a CSRF token gets **403** before login is checked. With a token but not logged in, the reply is **401**.







Values are UPPERCASE. A wrong value returns `400` with `"detail": "Invalid value for '<parameter>'."`.
Response: `items`, `page`, `size`, `totalItems`, `totalPages`. Use `totalItems` for the "Diamonds (N)" count.
Reset Filters = call with no filter parameters.