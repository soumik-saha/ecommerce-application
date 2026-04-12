# Bulk import script

Install dependencies:

```powershell
pip install -r .\scripts\requirements.txt
```

Run against a local API:

```powershell
python .\scripts\bulk_insert.py --base-url http://localhost:8081 --admin-secret change-me-admin-secret --products 20 --users 20 --max-workers 5
```

Environment overrides are supported for `ECOM_BASE_URL`, `ECOM_ADMIN_SECRET`, `ECOM_ADMIN_EMAIL`, `ECOM_ADMIN_PASSWORD`, `ECOM_CUSTOMER_PASSWORD`, and `ECOM_FAILURE_LOG`.

