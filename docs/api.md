# Keian Luxandor – Backend API (for the frontend team)

Last updated: 22 Oct 2026 · Backend: Spring Boot · All endpoints live under `/api/v1`

Local base URL: `http://localhost:8080` (Next.js proxies `/api/*` to it).

---

## Contents

1. [Basics you need everywhere](#1-basics-you-need-everywhere)
2. [Errors](#2-errors)
3. [Auth: register, login, logout, me, csrf](#3-auth)
4. [Email verification and password reset](#4-email-verification-and-password-reset)
5. [Products and shop filters](#5-products-and-shop-filters)
6. [Categories](#6-categories)
7. [My account: profile, password, checkout check](#7-my-account-profile-password-checkout-check)
8. [My account: addresses](#8-my-account-addresses)
9. [My account: wishlist](#9-my-account-wishlist)
10. [Pages the website needs](#10-pages-the-website-needs)
11. [Not built yet](#11-not-built-yet)
12. [All endpoints at a glance](#12-all-endpoints-at-a-glance)

---

## 1. Basics you need everywhere

| Topic | Rule |
|---|---|
| Money | Always in **paise** (integer). `18500000` = ₹1,85,000.00. Divide by 100 to display. Send paise in filters too. |
| Dates | ISO-8601 in UTC, e.g. `"2026-10-22T04:15:30Z"`. Convert to IST for display. |
| IDs | UUID strings. |
| Enum values | UPPERCASE, e.g. `LAB_GROWN`, `VERY_GOOD`, `VS1`. Map them to nice labels in the UI ("Lab-grown", "Very Good"). |
| Login | Session cookie `JSESSIONID` (HttpOnly, SameSite=Lax, 12 hours). Every `fetch` must use `credentials: "include"`. |
| CSRF | Every **POST / PUT / DELETE** needs header `X-CSRF-TOKEN`, **except** these 6: register, login, verify-email, resend-verification, forgot-password, reset-password. |
| Getting the CSRF token | `GET /api/v1/auth/csrf` → `{ "headerName": "X-CSRF-TOKEN", "token": "..." }`. Fetch a **new token after login** (the session changes). |
| Allowed origins (local) | `http://localhost:3000`, `http://localhost:3001` |

### Recommended fetch helper (TypeScript)

```ts
let csrfToken: string | null = null;

async function getCsrf(): Promise<string> {
  const r = await fetch("/api/v1/auth/csrf", { credentials: "include" });
  csrfToken = (await r.json()).token;
  return csrfToken!;
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);
  if (options.body) headers.set("Content-Type", "application/json");
  if (method !== "GET") headers.set("X-CSRF-TOKEN", csrfToken ?? (await getCsrf()));

  const res = await fetch(`/api/v1${path}`, { ...options, method, headers, credentials: "include" });
  if (res.status === 204) return undefined as T;
  const body = await res.json().catch(() => null);
  if (!res.ok) throw { status: res.status, ...body }; // body is the error object in section 2
  return body as T;
}
// After login or logout: call getCsrf() again.
```

---

## 2. Errors

Every error has the same JSON shape. **Show `detail` to the user.**

```json
{
  "status": 404,
  "title": "Resource Not Found",
  "detail": "Product not found: does-not-exist",
  "instance": "/api/v1/products/does-not-exist"
}
```

Invalid form fields (**400**) also include `errors`, listing **every** wrong field. Show each message under its input:

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

| Status | Meaning | What the UI should do |
|---|---|---|
| 400 | Invalid input, broken JSON, wrong filter value, or business rule (e.g. too many addresses) | Show `detail` / field `errors` |
| 401 | Not logged in (`"Please log in to continue."`) or wrong login | Send to login page |
| 403 | Missing/expired CSRF token (`"Missing or invalid CSRF token."`), not allowed (`"You do not have permission to do this."`), or a `code` (see below) | Refresh CSRF token and retry once; otherwise show `detail` |
| 404 | Not found, or not yours | Show "not found" |
| 409 | Conflict (e.g. email already registered) | Show `detail` |
| 429 | Too many attempts (login lock) | Show `detail` |

Some 403 errors carry a `code` you can check in code:

| code | When | UI |
|---|---|---|
| `EMAIL_NOT_VERIFIED` | Checkout before verifying email | Show message + "Resend verification email" button |

> Note: for POST/PUT/DELETE, a request **without** a CSRF token gets **403** even when not logged in. With a token but not logged in, it gets **401**.

---

## 3. Auth

### User object (returned by register, login, me, update profile)

```json
{
  "id": "4b1f6c2e-0d5a-4c4e-9a59-1a2b3c4d5e6f",
  "email": "riya@example.com",
  "fullName": "Riya Shah",
  "phone": "9876543210",
  "role": "CUSTOMER",
  "emailVerified": false,
  "createdAt": "2026-10-19T09:15:30Z"
}
```
`role` is `CUSTOMER` or `ADMIN`. `phone` can be `null`.

### POST `/api/v1/auth/register`
No login, no CSRF. Creates a **CUSTOMER** account (nobody can register as admin) and sends a verification email.

| Field | Required | Rules |
|---|---|---|
| email | yes | Valid email, max 255. Trimmed and stored in lower case |
| password | yes | 8 to 72 characters |
| fullName | yes | Max 100 |
| phone | no | 10-digit Indian mobile (starts with 6–9) |

```json
{ "email": "riya@example.com", "password": "Secret@123", "fullName": "Riya Shah", "phone": "9876543210" }
```
- **201** → user object. The customer is **not** logged in yet; call login next.
- **400** → field errors. **409** → `"An account with this email already exists."` (case-insensitive)

### POST `/api/v1/auth/login`
No CSRF.
```json
{ "email": "riya@example.com", "password": "Secret@123" }
```
- **200** → user object + session cookie. **Then fetch a new CSRF token.**
- **401** → `"Invalid email or password."` (same for unknown email and wrong password), or `"This account is disabled. Please contact support."`
- **429** → `"Too many failed login attempts. Please try again in 15 minutes."` (after 5 wrong passwords for one email)

### GET `/api/v1/auth/me`
- **200** → user object. **401** if not logged in. Call on app load to know who is logged in.

### GET `/api/v1/auth/csrf`
- **200** → `{ "headerName": "X-CSRF-TOKEN", "token": "..." }`

### POST `/api/v1/auth/logout`
Needs CSRF. **204**, session cleared.

---

## 4. Email verification and password reset

Customers **can log in before verifying**, but must verify before placing an order.
None of these need login or a CSRF token.

| Method | Path | Body | Reply |
|---|---|---|---|
| POST | `/api/v1/auth/verify-email` | `{ "token": "..." }` | **200** `{ "message": "Your email address is verified." }` |
| POST | `/api/v1/auth/resend-verification` | `{ "email": "..." }` | **202** always (see below) |
| POST | `/api/v1/auth/forgot-password` | `{ "email": "..." }` | **202** always (see below) |
| POST | `/api/v1/auth/reset-password` | `{ "token": "...", "newPassword": "..." }` | **200** `{ "message": "Your password has been changed. Please log in with your new password." }` |

- Invalid, expired or already-used link → **400** `"This link is invalid or has expired. Please request a new one."`
- `resend-verification` always replies `{ "message": "If an unverified account exists for this email, we have sent a new verification link." }` and `forgot-password` always replies `{ "message": "If an account exists for this email, we have sent a password reset link." }`. This is on purpose: the reply never reveals whether an email is registered.
- `newPassword`: 8 to 72 characters.
- Verification link valid **24 hours**; reset link valid **30 minutes**; each works **once**. Max one email per address per minute (extra requests are silently ignored).
- Resetting the password also marks the email as verified.

**Email links point to the website:**
- `{FRONTEND_URL}/verify-email?token=...`
- `{FRONTEND_URL}/reset-password?token=...`

Locally, emails go to **Mailpit**: open http://localhost:8025 to read them.

---

## 5. Products and shop filters

The store sells **loose diamonds** (natural and lab-grown). Each diamond is a product with **one variant** (the price and stock) and a `diamond` object (the grades).

### Product object

```json
{
  "id": "6f0d6a3e-...",
  "slug": "sample-emerald-201-lab",
  "name": "2.01 ct Emerald Lab-grown Diamond",
  "description": "Sample diamond for testing the shop filters.",
  "diamond": {
    "origin": "LAB_GROWN",
    "shape": "EMERALD",
    "carat": 2.01,
    "color": "F",
    "clarity": "VS1",
    "cut": null,
    "polish": "EXCELLENT",
    "symmetry": "EXCELLENT",
    "fluorescence": "NONE",
    "lab": "IGI",
    "quickShip": true
  },
  "images": [
    { "url": "https://placehold.co/800x800?text=Emerald", "altText": "2.01 ct Emerald Lab-grown Diamond" }
  ],
  "variants": [
    {
      "id": "1b3e8fb2-...",
      "sku": "SAMPLE-EMERALD-201-LAB",
      "name": "Single stone",
      "pricePaise": 19500000,
      "mrpPaise": null,
      "inStock": true
    }
  ]
}
```

| Field | Meaning |
|---|---|
| `diamond` | Grades (table below). **Can be `null`** for a product without diamond details — hide the grades section then. |
| `images` | First image first. Empty list → show a placeholder. |
| `variants` | Use `variants[0]`: `pricePaise` = price, `mrpPaise` = crossed-out price (can be `null`), `inStock` = can be bought. Empty list → not buyable. `variants[0].id` will be used by the cart. |

**Diamond fields**

| Field | Values | Display hint |
|---|---|---|
| `origin` | `NATURAL`, `LAB_GROWN` | "Natural", "Lab-grown" |
| `shape` | `ROUND`, `CUSHION`, `EMERALD`, `PEAR`, `ASSCHER`, `PRINCESS`, `OVAL`, `HEART`, `MARQUISE`, `RADIANT` | Shape icon + title case |
| `carat` | number, 2 decimals (e.g. `2.01`) | "2.01 ct" |
| `color` | `D` `E` `F` `G` `H` `I` `J` `K` `L` `M` (D is best) | as is |
| `clarity` | `FL`, `IF`, `VVS1`, `VVS2`, `VS1`, `VS2`, `SI1`, `SI2`, `I1`, `I2`, `I3` (FL is best) | as is |
| `cut` | `IDEAL`, `EXCELLENT`, `VERY_GOOD`, `GOOD`, `FAIR`, or **`null`** | `null` → "Not graded" (normal for fancy shapes) |
| `polish`, `symmetry` | `EXCELLENT`, `VERY_GOOD`, `GOOD`, `FAIR` | "Very Good" etc. |
| `fluorescence` | `NONE`, `FAINT`, `MEDIUM`, `STRONG`, `VERY_STRONG` | |
| `lab` | `GIA`, `IGI`, `HRD` | Certificate lab |
| `quickShip` | `true` / `false` | "Quick shipping" badge |

### GET `/api/v1/products` — list, search and filter (public)

Returns ACTIVE products only, one page at a time. All parameters are optional and combine with AND.

| Parameter | Values | Shop UI control |
|---|---|---|
| `origin` | `NATURAL` or `LAB_GROWN` (one value; omit for both) | Origin toggle |
| `shape` | one or more shapes (list) | Shape icons |
| `minCarat`, `maxCarat` | decimal, e.g. `2.00`, `2.50` | Carat slider |
| `minPrice`, `maxPrice` | **paise**, e.g. ₹750 → `75000` | Price slider |
| `withImage` | `true` / `false` (default `false`) | "With Image Only" |
| `quickShip` | `true` / `false` (default `false`) | "Quick Shipping" |
| `color` | one or more of `D`…`M` (list) | Color |
| `clarity` | one or more clarity values (list) | Clarity |
| `cut` | list | Advanced filters |
| `polish` | list | Advanced filters |
| `symmetry` | list | Advanced filters |
| `fluorescence` | list | Advanced filters |
| `lab` | list | Advanced filters |
| `q` | text search in the name (not case-sensitive) | Search box |
| `category` | category slug | (optional) |
| `sort` | see below | Sort dropdown |
| `page` | starts at `0` (default `0`) | Pagination |
| `size` | 1 to 50 (default `20`) | Page size |

**Lists** can be sent either way: `color=D,E,F` or `color=D&color=E&color=F`.
A diamond with `cut = null` is excluded when a `cut` filter is used.

**Sort values**

| `sort` | Meaning |
|---|---|
| `recommended` (default) | Newest first (for now) |
| `newest` | Newest first |
| `price_asc` / `price_desc` | Price low → high / high → low |
| `carat_asc` / `carat_desc` | Carat small → large / large → small |
| `name` | A → Z |

**Example** (Lab-grown, Emerald, 2.00–2.50 ct, F–D colour, with image, cheapest first):
```
GET /api/v1/products?origin=LAB_GROWN&shape=EMERALD&minCarat=2.00&maxCarat=2.50
    &color=D,E,F&withImage=true&sort=price_asc&page=0&size=24
```

**Response**
```json
{
  "items": [ { "...product object..." } ],
  "page": 0,
  "size": 24,
  "totalItems": 2,
  "totalPages": 1
}
```
- Use `totalItems` for "Diamonds (N)".
- Nothing matches → `items: []`, `totalItems: 0`.
- Wrong value (e.g. `color=Z`, lowercase `lab_grown`, `minCarat=abc`) → **400** `"Invalid value for 'color'."`
- **Reset Filters** = call again with no filter parameters.
- Keep the filters in the page URL so links and the back button work.

### GET `/api/v1/products/{slug}` — product page (public)
- **200** → product object.
- **404** → `"Product not found: {slug}"` (also for DRAFT/ARCHIVED products).

---

## 6. Categories

> For natural vs lab-grown, use the `origin` filter above, not categories.

### GET `/api/v1/categories` (public)
```json
[ { "id": "5e515fce-...", "slug": "clothing", "name": "Clothing" } ]
```
Sorted by display order, then name.

### GET `/api/v1/categories/{slug}/products` (public)
All ACTIVE products in the category as a plain list (no paging). **404** `"Category not found: {slug}"`.
For paging/filters use `GET /api/v1/products?category={slug}`.

---

## 7. My account: profile, password, checkout check

All need **login**. PUT/POST need **CSRF**.

### PUT `/api/v1/account/profile`
| Field | Required | Rules |
|---|---|---|
| fullName | yes | Max 100 |
| phone | no | 10-digit Indian mobile. Empty or missing **removes** it |

- **200** → user object. **400** → field errors. Email cannot be changed.

### POST `/api/v1/account/change-password`
```json
{ "currentPassword": "Secret@123", "newPassword": "NewSecret@456" }
```
- **200** → `{ "message": "Your password has been changed." }`. The customer stays logged in (fetch a new CSRF token), and gets a "Your password was changed" email.
- **400** → `"Your current password is incorrect."`, `"Your new password must be different from your current password."`, or field errors (new password 8–72 characters).
- **429** → `"Too many wrong passwords. Please try again in 15 minutes."` (shares the login lock)

### GET `/api/v1/account/checkout-eligibility`
Call before showing checkout.
- **200** → `{ "canCheckout": true }`
- **403** with `"code": "EMAIL_NOT_VERIFIED"`, `"detail": "Please verify your email address before placing an order."` → show a "Resend verification email" button (`POST /api/v1/auth/resend-verification`).

---

## 8. My account: addresses

All need **login**. POST/PUT/DELETE need **CSRF**. A customer only sees their own addresses; someone else's address id → **404** `"Address not found."`.

| Method | Path | Reply |
|---|---|---|
| GET | `/api/v1/account/addresses` | **200** list, default address first |
| GET | `/api/v1/account/addresses/states` | **200** list of 36 state / union territory names (for the dropdown) |
| POST | `/api/v1/account/addresses` | **201** the new address |
| PUT | `/api/v1/account/addresses/{id}` | **200** the updated address |
| PUT | `/api/v1/account/addresses/{id}/default` | **200** the address, now default |
| DELETE | `/api/v1/account/addresses/{id}` | **204** |

**Request body (POST and PUT)**

| Field | Required | Rules |
|---|---|---|
| fullName | yes | Max 100 |
| phone | yes | 10-digit Indian mobile |
| line1 | yes | Max 200 (house/flat, building, street) |
| line2 | no | Max 200 |
| landmark | no | Max 100 |
| city | yes | Max 80 |
| state | yes | One of the names from `/states` (any capitalisation; saved in the official spelling) |
| pincode | yes | 6 digits, cannot start with 0 |
| makeDefault | no | `true` makes it the default |

**Address object**
```json
{
  "id": "…",
  "fullName": "Riya Shah",
  "phone": "9876543210",
  "line1": "12 Lake View Road",
  "line2": null,
  "landmark": "Near City Mall",
  "city": "Bengaluru",
  "state": "Karnataka",
  "pincode": "560001",
  "defaultAddress": true
}
```

**Rules:** the first address becomes the default automatically; only one default; deleting the default makes the newest remaining address the default; max **10** addresses (**400** `"You can save up to 10 addresses. Please delete one first."`). Unknown state → **400** `"Choose a valid Indian state or union territory."`

---

## 9. My account: wishlist

All need **login**. PUT/DELETE need **CSRF**.

| Method | Path | Reply |
|---|---|---|
| GET | `/api/v1/account/wishlist` | **200** list of `{ product, addedAt }`, newest first |
| GET | `/api/v1/account/wishlist/ids` | **200** list of product ids, e.g. `["6f0d…", "a793…"]` |
| PUT | `/api/v1/account/wishlist/{productId}` | **204**. Adding twice does nothing |
| DELETE | `/api/v1/account/wishlist/{productId}` | **204**. Also 204 if it wasn't there |

```json
[
  { "product": { "...product object, including diamond..." }, "addedAt": "2026-10-22T04:15:30Z" }
]
```

**Rules:** only ACTIVE products can be added (**404** `"Product not found."`); products later archived disappear from the list; max **100** (**400** `"Your wishlist can hold up to 100 products. Please remove some first."`).

**Tip:** on the shop page call `/wishlist/ids` once to fill the heart icons. When a logged-out user clicks a heart, send them to login.

---

## 10. Pages the website needs

| Page | Uses |
|---|---|
| Shop `/shop` (+ shape/origin in URL) | `GET /products` with filters, `GET /account/wishlist/ids` |
| Product page `/diamond/{slug}` | `GET /products/{slug}`, wishlist PUT/DELETE |
| Register / Login | `POST /auth/register`, `POST /auth/login`, then `GET /auth/csrf` |
| Header (who is logged in) | `GET /auth/me`, `POST /auth/logout` |
| `/verify-email?token=` | Read `token` from URL → `POST /auth/verify-email` → show message |
| Forgot password | `POST /auth/forgot-password` → show the message |
| `/reset-password?token=` | New-password form → `POST /auth/reset-password` |
| Login page extras | "Forgot password?" link, "Resend verification email" button |
| My profile | `GET /auth/me`, `PUT /account/profile`, `POST /account/change-password` |
| My addresses | Address endpoints + state dropdown from `/addresses/states` |
| My wishlist | `GET /account/wishlist` |
| Before checkout | `GET /account/checkout-eligibility` |

---

## 11. Not built yet

These are planned; please don't build against them yet — the shapes may change.

| Feature | Status |
|---|---|
| Certificate number, "Verify on IGI/GIA" link, laser inscription, measurements | Next |
| Video / 360° media, similar diamonds | Planned |
| Cart, coupons | Planned |
| Checkout, Razorpay payment, delivery PIN check | Planned |
| Orders (customer "My orders", admin order management) | Planned |
| Admin: add / edit / archive diamonds, upload photos | Planned (`/api/v1/admin/**`, ADMIN only) |
| `category`, `isNew`, `createdAt` on products | Not in the response — use `diamond.origin` for natural/lab-grown |

---

## 12. All endpoints at a glance

| Method | Path | Login | CSRF | Success |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | – | – | 201 user |
| POST | `/api/v1/auth/login` | – | – | 200 user |
| GET | `/api/v1/auth/me` | ✔ | – | 200 user |
| GET | `/api/v1/auth/csrf` | – | – | 200 token |
| POST | `/api/v1/auth/logout` | ✔ | ✔ | 204 |
| POST | `/api/v1/auth/verify-email` | – | – | 200 message |
| POST | `/api/v1/auth/resend-verification` | – | – | 202 message |
| POST | `/api/v1/auth/forgot-password` | – | – | 202 message |
| POST | `/api/v1/auth/reset-password` | – | – | 200 message |
| GET | `/api/v1/products` | – | – | 200 page of products |
| GET | `/api/v1/products/{slug}` | – | – | 200 product |
| GET | `/api/v1/categories` | – | – | 200 list |
| GET | `/api/v1/categories/{slug}/products` | – | – | 200 list |
| PUT | `/api/v1/account/profile` | ✔ | ✔ | 200 user |
| POST | `/api/v1/account/change-password` | ✔ | ✔ | 200 message |
| GET | `/api/v1/account/checkout-eligibility` | ✔ | – | 200 / 403 |
| GET | `/api/v1/account/addresses` | ✔ | – | 200 list |
| GET | `/api/v1/account/addresses/states` | ✔ | – | 200 list |
| POST | `/api/v1/account/addresses` | ✔ | ✔ | 201 address |
| PUT | `/api/v1/account/addresses/{id}` | ✔ | ✔ | 200 address |
| PUT | `/api/v1/account/addresses/{id}/default` | ✔ | ✔ | 200 address |
| DELETE | `/api/v1/account/addresses/{id}` | ✔ | ✔ | 204 |
| GET | `/api/v1/account/wishlist` | ✔ | – | 200 list |
| GET | `/api/v1/account/wishlist/ids` | ✔ | – | 200 list |
| PUT | `/api/v1/account/wishlist/{productId}` | ✔ | ✔ | 204 |
| DELETE | `/api/v1/account/wishlist/{productId}` | ✔ | ✔ | 204 |
| GET | `/actuator/health` | – | – | 200 `{"status":"UP"}` |

**Test data (local):** run `backend/dev/sample-diamonds.sql` to load 12 sample diamonds (8 lab-grown, 4 natural). Image URLs in the samples are placeholders.
