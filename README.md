# ecommerce-application

## Postman: Admin Register, Login, Logout

Base URL: `http://localhost:8081`

### 1) Register Admin

- Method: `POST`
- URL: `/api/auth/register/admin`
- Headers:
  - `Content-Type: application/json`
  - `X-Admin-Secret: change-me-admin-secret`
- Body:

```json
{
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@example.com",
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

Expected: `201 Created` with `accessToken` and `role: "ADMIN"`.

### 2) Login as Admin

- Method: `POST`
- URL: `/api/auth/login`
- Headers: `Content-Type: application/json`
- Body:

```json
{
  "email": "admin@example.com",
  "password": "Password@123"
}
```

Expected: `200 OK` with `accessToken` and `role: "ADMIN"`.

### 3) Logout

- Method: `POST`
- URL: `/api/auth/logout`
- Headers:
  - `Authorization: Bearer <accessToken>`

Expected: `200 OK` with message `Logged out successfully`.

Note: JWT is stateless in this app. Logout confirms the action and the client (Postman) should clear/delete the saved token.

### 4) Audit log bulk upload

- Method: `POST`
- URL: `/api/audit-logs/batch`
- Headers:
  - `Authorization: Bearer <accessToken>`
  - `Content-Type: application/json`
  - `X-Idempotency-Key: <optional-fallback-key>`
- Body:

```json
{
  "logs": [
    {
      "entityType": "PRODUCT",
      "entityId": 1,
      "action": "UPDATE",
      "description": "Updated product price",
      "oldValue": "999.00",
      "newValue": "1299.00",
      "idempotencyKey": "audit-1700000000000-product-1-update"
    }
  ]
}
```

Expected: `200 OK` with `successCount`, `duplicateCount`, and `failureCount`.

### 5) Audit log download

- Method: `GET`
- URL: `/api/audit-logs/download`
- Headers:
  - `Authorization: Bearer <admin-accessToken>`
- Optional query params:
  - `userId`
  - `entityType`
  - `entityId`
  - `action`
  - `startDate`
  - `endDate`

Expected: `200 OK` with a CSV file download (`Content-Disposition: attachment; filename=audit-logs.csv`).

