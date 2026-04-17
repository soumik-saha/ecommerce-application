# E-commerce API Reference

Base URL: `http://localhost:8081`

Swagger UI: `http://localhost:8081/swagger-ui.html`
OpenAPI JSON: `http://localhost:8081/v3/api-docs`

> Current implementation exposes the `/api/**` routes documented below.
> The security layer also permits `/api/v1/**`, but the controllers in this repository are mapped to `/api/**` only.

---

## 1) Common API conventions

### Authentication
- Stateless JWT authentication using `Authorization: Bearer <accessToken>`.
- Refresh tokens are returned by auth responses and should be stored securely by the client.
- Public endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/register/admin`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - `GET /api/products/**`
- Admin-only write endpoints:
  - `POST /api/products`
  - `PUT /api/products/{id}`
  - `DELETE /api/products/{id}`
  - `GET /api/orders`
- Authenticated endpoints:
  - `POST /api/auth/logout`
  - `/api/users/**`
  - `/api/cart/**`
  - `POST /api/orders`

### Standard headers
- `Content-Type: application/json`
- `Authorization: Bearer <accessToken>` for protected routes
- `X-Admin-Secret: <secret>` for admin registration
- `X-Idempotency-Key: <unique-key>` for idempotent batch audit submissions

### Common status codes
- `200 OK` – successful read/update/logout
- `201 Created` – successful create
- `204 No Content` – successful delete
- `400 Bad Request` – validation or request errors
- `401 Unauthorized` – missing or invalid JWT
- `403 Forbidden` – insufficient role/authority
- `404 Not Found` – resource does not exist
- `409 Conflict` – duplicate or business conflict
- `429 Too Many Requests` – rate limit exceeded

### Error response model
The application returns structured errors via `ApiErrorResponse`:

```json
{
  "timestamp": "2026-04-10T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "fieldErrors": {
    "name": "Product name is required"
  }
}
```

---

## 2) Request/response contracts

### `RegisterRequest`
Used by `/api/auth/register` and `/api/auth/register/admin`.

Fields:
- `firstName` string, required
- `lastName` string, required
- `email` string, required, must be valid email
- `phone` string, optional, Indian phone number format
- `password` string, required
- `address` object, required

`address` fields:
- `street` string, required
- `city` string, required
- `state` string, required
- `zipcode` string, required
- `country` string, required

### `AuthRequest`
Used by `/api/auth/login`.

Fields:
- `email` string, required
- `password` string, required

### `AuthResponse`
Returned by register/login/refresh.

Fields:
- `accessToken`
- `refreshToken`
- `tokenType` (`Bearer`)
- `userId`
- `email`
- `role`
- `accessTokenExpiresAt`
- `refreshTokenExpiresAt`

### `ProductRequest`
Used by create/update product.

Fields:
- `name` string, required
- `description` string, required
- `price` number, required, must be > 0
- `stockQuantity` integer, required, must be >= 0
- `category` string, required
- `imageUrl` string, required

### `ProductResponse`
Fields:
- `id`
- `name`
- `description`
- `price`
- `stockQuantity`
- `category`
- `imageUrl`
- `active`

### `UserRequest`
Used by `/api/users` create/update.

Fields:
- `firstName` string, required
- `lastName` string, required
- `email` string, required
- `phone` string, optional
- `address` object, required

### `UserResponse`
Fields:
- `id`
- `firstName`
- `lastName`
- `email`
- `phone`
- `role`
- `address`

### `CartItemRequest`
Used by `/api/cart`.

Fields:
- `productId` long, required, must be positive
- `quantity` integer, required, must be >= 1

### `CartItemResponse`
Fields:
- `product`
- `quantity`
- `price`

### `OrderResponse`
Fields:
- `id`
- `totalAmount`
- `status`
- `items`

### `OrderItemDTO`
Fields:
- `id`
- `productId`
- `quantity`
- `price`

---

## 3) API catalog

## 3.1 Auth APIs

### Register customer
- **Method:** `POST`
- **Path:** `/api/auth/register`
- **Auth:** Public
- **Body:** `RegisterRequest`
- **Success:** `201 Created`
- **Response:** `AuthResponse`

Example:
```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane@example.com",
  "phone": "9876543210",
  "password": "Password@123",
  "address": {
    "street": "MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "zipcode": "560001",
    "country": "India"
  }
}
```

### Register admin
- **Method:** `POST`
- **Path:** `/api/auth/register/admin`
- **Auth:** Public
- **Headers:** `X-Admin-Secret`
- **Body:** `RegisterRequest`
- **Success:** `201 Created`
- **Response:** `AuthResponse`
- **Notes:** Admin secret is configured by `app.auth.admin-registration-secret`

Example header:
```http
X-Admin-Secret: change-me-admin-secret
```

### Login
- **Method:** `POST`
- **Path:** `/api/auth/login`
- **Auth:** Public
- **Body:** `AuthRequest`
- **Success:** `200 OK`
- **Response:** `AuthResponse`

Example:
```json
{
  "email": "jane@example.com",
  "password": "Password@123"
}
```

### Refresh access token
- **Method:** `POST`
- **Path:** `/api/auth/refresh`
- **Auth:** Public
- **Body:**
```json
{
  "refreshToken": "<refresh-token>"
}
```
- **Success:** `200 OK`
- **Response:** `AuthResponse`

### Logout
- **Method:** `POST`
- **Path:** `/api/auth/logout`
- **Auth:** Required
- **Body:** none
- **Success:** `200 OK`
- **Response:**
```json
{
  "message": "Logged out successfully"
}
```

---

## 3.2 Product APIs

### Create product
- **Method:** `POST`
- **Path:** `/api/products`
- **Auth:** Admin only
- **Body:** `ProductRequest`
- **Success:** `201 Created`
- **Response:** `ProductResponse`

Example:
```json
{
  "name": "Wireless Headphones",
  "description": "Noise-cancelling over-ear headphones",
  "price": 4999.00,
  "stockQuantity": 250,
  "category": "Electronics",
  "imageUrl": "https://example.com/headphones.png"
}
```

### List products with pagination and filtering
- **Method:** `GET`
- **Path:** `/api/products`
- **Auth:** Public
- **Query params:**
  - `keyword` string, optional, default `""`
  - `page` integer, optional, default `0`, min `0`
  - `size` integer, optional, default `10`, min `1`, max `100`
- **Success:** `200 OK`
- **Response:** `Page<ProductResponse>`

Example:
```http
GET /api/products?keyword=phone&page=0&size=20
```

### Get product by id
- **Method:** `GET`
- **Path:** `/api/products/{id}`
- **Auth:** Public
- **Path params:** `id` long, required, positive
- **Success:** `200 OK`
- **Response:** `ProductResponse`

### Update product
- **Method:** `PUT`
- **Path:** `/api/products/{id}`
- **Auth:** Admin only
- **Path params:** `id` long, required, positive
- **Body:** `ProductRequest`
- **Success:** `200 OK`
- **Response:** message body

### Delete product
- **Method:** `DELETE`
- **Path:** `/api/products/{id}`
- **Auth:** Admin only
- **Path params:** `id` long, required, positive
- **Behavior:** soft delete (`active=false`)
- **Success:** `204 No Content`

### Search products
- **Method:** `GET`
- **Path:** `/api/products/search`
- **Auth:** Public
- **Query params:**
  - `q` string, optional (searches name + description)
  - `page` integer, optional, default `0`
  - `limit` integer, optional, default `10`
  - `keyword` string, optional (legacy exact endpoint)
- **Success:** `200 OK`
- **Response:** `Page<ProductResponse>` when using `q`, otherwise `List<ProductResponse>`

Example:
```http
GET /api/products/search?q=Auto&page=0&limit=10
GET /api/products/search?keyword=Auto
```

### Product reviews
- **Method:** `GET`
- **Path:** `/api/products/{id}/reviews`
- **Auth:** Public
- **Query params:** `page`, `size`
- **Success:** `200 OK`
- **Response:** `Page<ReviewResponse>`

---

## 3.3 User APIs

### List users
- **Method:** `GET`
- **Path:** `/api/users`
- **Auth:** Required
- **Role:** any authenticated user
- **Success:** `200 OK`
- **Response:** `List<UserResponse>`

### Create user
- **Method:** `POST`
- **Path:** `/api/users`
- **Auth:** Required
- **Body:** `UserRequest`
- **Success:** `201 Created`
- **Response:** plain text message: `User created`

### Get user by id
- **Method:** `GET`
- **Path:** `/api/users/{userId}`
- **Auth:** Required
- **Path params:** `userId` long, required, positive
- **Success:** `200 OK`
- **Response:** `UserResponse`

### Update user
- **Method:** `PUT`
- **Path:** `/api/users/{userId}`
- **Auth:** Required
- **Path params:** `userId` long, required, positive
- **Body:** `UserRequest`
- **Success:** `200 OK`
- **Response:** plain text message: `User updated`

---

## 3.4 Cart APIs

### Add item to cart
- **Method:** `POST`
- **Path:** `/api/cart`
- **Auth:** Required
- **Body:** `CartItemRequest`
- **Success:** `201 Created`
- **Failure:** `400 Bad Request` if product is out of stock / missing user / missing product

Example:
```json
{
  "productId": 101,
  "quantity": 2
}
```

### Get cart items
- **Method:** `GET`
- **Path:** `/api/cart`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `List<CartItemResponse>`

### Remove item from cart
- **Method:** `DELETE`
- **Path:** `/api/cart/items/{productId}`
- **Auth:** Required
- **Path params:** `productId` long, required
- **Success:** `204 No Content`
- **If item not found:** `404 Not Found`

---

## 3.5 Order APIs

### Create order from cart
- **Method:** `POST`
- **Path:** `/api/orders`
- **Auth:** Required
- **Headers:**
  - `X-Idempotency-Key` (optional) for safe retries
- **Body (optional):**
```json
{
  "promoCode": "SUMMER10"
}
```
- **Success:** `201 Created`
- **Response:** `OrderResponse`
- **Behavior:**
  - Reads the current user’s cart
  - Creates order items
  - Persists the order in one transaction
  - Clears cart items after order creation

### List my orders (paged)
- **Method:** `GET`
- **Path:** `/api/orders?page=&limit=`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `Page<OrderSummaryResponse>`

### Get order details
- **Method:** `GET`
- **Path:** `/api/orders/{id}`
- **Auth:** Required (owner or admin)
- **Success:** `200 OK`
- **Response:** `OrderDetailResponse`

### List all orders
- **Method:** `GET`
- **Path:** `/api/orders`
- **Auth:** Admin only
- **Success:** `200 OK`
- **Response:** `List<OrderResponse>`

---

## 3.6 Audit Log APIs

### Batch log audits
- **Method:** `POST`
- **Path:** `/api/audit-logs/batch`
- **Auth:** Required
- **Body:** `AuditLogBatchRequest`
- **Success:** `200 OK`
- **Response:** `AuditLogBatchResponse`
- **Headers:**
  - `X-Idempotency-Key` optional fallback key for the whole batch
- **Notes:**
  - Prefer sending a stable `idempotencyKey` per item in `AuditLogRequest`
  - Duplicate rows are counted in `duplicateCount` instead of being written twice

Example:
```json
{
  "logs": [
    {
      "entityType": "PRODUCT",
      "entityId": 5,
      "action": "UPDATE",
      "description": "Updated product price",
      "oldValue": "999.00",
      "newValue": "1299.00",
      "idempotencyKey": "audit-1700000000000-product-5-update"
    },
    {
      "entityType": "USER",
      "entityId": 10,
      "action": "CREATE",
      "description": "New user registered",
      "idempotencyKey": "audit-1700000000001-user-10-create"
    }
  ]
}
```

### Download audit logs as CSV
- **Method:** `GET`
- **Path:** `/api/audit-logs/download`
- **Auth:** Admin only
- **Produces:** `text/csv`
- **Query params:**
  - `userId` optional
  - `entityType` optional
  - `entityId` optional
  - `action` optional
  - `startDate` optional ISO-8601 instant
  - `endDate` optional ISO-8601 instant
- **Success:** `200 OK`
- **Response:** CSV file download with `Content-Disposition: attachment`

### Get audits by user
- **Method:** `GET`
- **Path:** `/api/audit-logs/user/{userId}`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `List<AuditLog>`

### Get audits by entity
- **Method:** `GET`
- **Path:** `/api/audit-logs/entity/{entityType}/{entityId}`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `List<AuditLog>`

### Get audits by date range
- **Method:** `GET`
- **Path:** `/api/audit-logs/range`
- **Auth:** Admin only
- **Query params:**
  - `startDate` ISO-8601 timestamp, required
  - `endDate` ISO-8601 timestamp, required
  - `page` integer, optional, default `0`
  - `size` integer, optional, default `20`
- **Success:** `200 OK`
- **Response:** `Page<AuditLog>`

---

## 3.7 Payment APIs

### Create payment
- **Method:** `POST`
- **Path:** `/api/payments/create` (alias: `/api/payments`)
- **Auth:** Required
- **Body:** `PaymentRequest`
- **Success:** `201 Created`
- **Response:** `PaymentResponse`

### Verify payment (gateway callback)
- **Method:** `POST`
- **Path:** `/api/payments/verify`
- **Auth:** Admin only
- **Body:**
```json
{
  "gatewayTransactionId": "<gateway-id>",
  "status": "SUCCESS"
}
```
- **Success:** `200 OK`
- **Response:** `PaymentResponse`

### Get payment by order
- **Method:** `GET`
- **Path:** `/api/payments/orders/{orderId}`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `PaymentResponse`

---

## 3.8 Promo APIs

### Apply promo code
- **Method:** `POST`
- **Path:** `/api/promo/apply`
- **Auth:** Required
- **Body:** `PromoApplyRequest`
- **Success:** `200 OK`
- **Response:** `PromoApplyResponse`

---

## 3.9 Wishlist APIs

### Add to wishlist
- **Method:** `POST`
- **Path:** `/api/wishlist`
- **Auth:** Required
- **Body:** `WishlistRequest`
- **Success:** `201 Created`
- **Response:** `WishlistResponse`

### Get wishlist
- **Method:** `GET`
- **Path:** `/api/wishlist`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `List<WishlistResponse>`

### Remove from wishlist
- **Method:** `DELETE`
- **Path:** `/api/wishlist/{productId}`
- **Auth:** Required
- **Success:** `204 No Content`

---

## 3.10 Recommendation APIs

### Get recommendations
- **Method:** `GET`
- **Path:** `/api/recommendations`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `List<ProductResponse>`

---

## 3.11 Notification APIs

### Get notifications
- **Method:** `GET`
- **Path:** `/api/notifications`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `List<NotificationResponse>`

### Mark notification read
- **Method:** `POST`
- **Path:** `/api/notifications/read/{id}`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `NotificationResponse`

---

## 3.12 Return APIs

### Create return request
- **Method:** `POST`
- **Path:** `/api/returns`
- **Auth:** Required
- **Body:** `ReturnCreateRequest`
- **Success:** `201 Created`
- **Response:** `ReturnResponse`

### Get my returns
- **Method:** `GET`
- **Path:** `/api/returns`
- **Auth:** Required
- **Success:** `200 OK`
- **Response:** `List<ReturnResponse>`

---

## 4) API usage examples for Postman / bulk testing

### Auth workflow
1. Register customer or admin
2. Log in and capture `accessToken`
3. Use `Authorization: Bearer <token>` for protected calls
4. Use `/api/auth/refresh` when the access token expires
5. Call `/api/auth/logout` and discard client-side tokens

### Example cURL for product create
```bash
curl -X POST http://localhost:8081/api/products \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Bulk Product 1\",\"description\":\"Demo\",\"price\":1999.00,\"stockQuantity\":50,\"category\":\"Electronics\",\"imageUrl\":\"https://example.com/p1.png\"}"
```

### Example cURL for cart add
```bash
curl -X POST http://localhost:8081/api/cart \
  -H "Authorization: Bearer <customer-token>" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":1,\"quantity\":3}"
```

---

## 5) Bulk data loading strategy

If your goal is to populate the database with large amounts of realistic data, use the following approach by environment.

### A. Best option for development and testing
Use **seed files** plus a **profile-gated loader**.

Recommended order:
1. `Address`
2. `User`
3. `Product`
4. `CartItem`
5. `Order` / `OrderItem`
6. `RefreshToken` only if you need auth-state fixtures

Why this order matters:
- `User` depends on `Address`
- `CartItem` depends on `User` and `Product`
- `OrderItem` depends on `Order` and `Product`

### B. Best option for very large volumes
Use PostgreSQL-native bulk loading:
- `COPY` from CSV for initial large datasets
- Batch inserts with chunk sizes of 500–2000 rows
- Staging tables followed by merge/upsert into production tables
- Create heavy indexes after loading large data, not before

### C. Best option for application-managed import
Add an admin-only import endpoint or a batch job that:
- accepts `JSON` or `CSV`
- validates in chunks
- deduplicates using natural keys like `email`, `sku`, or `productName+category`
- persists in a transaction per chunk
- publishes async events after import for cache refresh / search indexing

### D. Safe seeding rules for this project
- Never seed `password` in plain text; always encode through `PasswordEncoder`
- Use unique emails for users
- Avoid direct `Order` seeding unless the related `User`, `Product`, and `CartItem` records already exist
- For `Product`, keep `active=true` unless you are testing soft delete
- For cart/order fixtures, seed from service layer if you want realistic totals and stock reduction behavior

### E. Practical fixture formats
#### Users CSV
```csv
firstName,lastName,email,phone,street,city,state,zipcode,country,role
Admin,User,admin1@example.com,9876543210,MG Road,Bengaluru,Karnataka,560001,India,ADMIN
John,Doe,john@example.com,9876543211,Park Street,Kolkata,West Bengal,700016,India,CUSTOMER
```

#### Products CSV
```csv
name,description,price,stockQuantity,category,imageUrl,active
Wireless Headphones,Noise cancelling headphones,4999.00,100,Electronics,https://example.com/h1.png,true
Office Chair,Ergonomic chair,8999.00,40,Furniture,https://example.com/chair.png,true
```

#### Orders
For orders, prefer **generated data** from a service job rather than raw inserts, so totals and statuses remain consistent.

### F. Recommendation for this repository
The cleanest next step is to add one of these:
1. `src/main/resources/data.sql` for a small default dataset
2. A `CommandLineRunner` or `ApplicationRunner` under a `bootstrap` package for profile-based seed data
3. An admin-only bulk import API for runtime ingestion

For a production-grade setup, I recommend **(2) for dev/demo** and **(3) for operational imports**.

---

## 6) Notes on currently implemented modules

Implemented public modules in this repository:
- Auth
- Product
- User
- Cart
- Order

Planned modules from the broader architecture vision, but not exposed as controllers in the current codebase:
- Inventory
- Payment
- Review
- Category / Variant management beyond the current `category` field

---

## 7) Quick reference endpoint table

| Module | Method | Path | Auth |
|---|---:|---|---|
| Auth | POST | `/api/auth/register` | Public |
| Auth | POST | `/api/auth/register/admin` | Public + `X-Admin-Secret` |
| Auth | POST | `/api/auth/login` | Public |
| Auth | POST | `/api/auth/refresh` | Public |
| Auth | POST | `/api/auth/logout` | Required |
| Products | POST | `/api/products` | ADMIN |
| Products | GET | `/api/products` | Public |
| Products | GET | `/api/products/{id}` | Public |
| Products | PUT | `/api/products/{id}` | ADMIN |
| Products | DELETE | `/api/products/{id}` | ADMIN |
| Products | GET | `/api/products/search` | Public |
| Products | GET | `/api/products/{id}/reviews` | Public |
| Users | GET | `/api/users` | Required |
| Users | POST | `/api/users` | Required |
| Users | GET | `/api/users/{userId}` | Required |
| Users | PUT | `/api/users/{userId}` | Required |
| Cart | POST | `/api/cart` | Required |
| Cart | GET | `/api/cart` | Required |
| Cart | DELETE | `/api/cart/items/{productId}` | Required |
| Orders | POST | `/api/orders` | Required |
| Orders | GET | `/api/orders?page=&limit=` | Required |
| Orders | GET | `/api/orders/{id}` | Required |
| Orders | GET | `/api/orders` | ADMIN |
| Payments | POST | `/api/payments` | Required |
| Payments | POST | `/api/payments/create` | Required |
| Payments | POST | `/api/payments/verify` | ADMIN |
| Promo | POST | `/api/promo/apply` | Required |
| Wishlist | POST | `/api/wishlist` | Required |
| Wishlist | GET | `/api/wishlist` | Required |
| Wishlist | DELETE | `/api/wishlist/{productId}` | Required |
| Recommendations | GET | `/api/recommendations` | Required |
| Notifications | GET | `/api/notifications` | Required |
| Notifications | POST | `/api/notifications/read/{id}` | Required |
| Returns | POST | `/api/returns` | Required |
| Returns | GET | `/api/returns` | Required |
| Audit Logs | POST | `/api/audit-logs/batch` | Required |
| Audit Logs | GET | `/api/audit-logs/user/{userId}` | Required |
| Audit Logs | GET | `/api/audit-logs/entity/{entityType}/{entityId}` | Required |
| Audit Logs | GET | `/api/audit-logs/range` | ADMIN |

---

## 8) If you want true bulk import next
If you want, the next best code change is to add:
- `POST /api/admin/import/products`
- `POST /api/admin/import/users`
- `POST /api/admin/import/orders`
- async batch processing with validation and chunked persistence

That would let you upload large datasets safely without hand-writing SQL.
