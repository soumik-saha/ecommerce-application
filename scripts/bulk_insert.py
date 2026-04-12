#!/usr/bin/env python3
"""Bulk insert data into the ecommerce API.

Safe defaults:
- Uses requests.Session per worker client
- Retries 3 times
- Waits on HTTP 429 using Retry-After or a configured delay
- Caps concurrency at 10 workers
- Logs failures to a separate JSONL file
"""

from __future__ import annotations

import argparse
import json
import logging
import math
import os
import random
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime
from itertools import cycle
from pathlib import Path
from typing import Any, Iterable, Optional

import requests
from requests.exceptions import RequestException

BASE_URL = os.environ.get("ECOM_BASE_URL", "http://localhost:8081")
ADMIN_SECRET = os.environ.get("ECOM_ADMIN_SECRET", "change-me-admin-secret")
ADMIN_EMAIL = os.environ.get("ECOM_ADMIN_EMAIL", "bulk-admin@example.com")
ADMIN_PASSWORD = os.environ.get("ECOM_ADMIN_PASSWORD", "Password@123")
CUSTOMER_PASSWORD = os.environ.get("ECOM_CUSTOMER_PASSWORD", "Password@123")
FAILURE_LOG = os.environ.get("ECOM_FAILURE_LOG", "bulk_import_failures.jsonl")


@dataclass
class Address:
	street: str
	city: str
	state: str
	zipcode: str
	country: str = "India"

	def to_dict(self) -> dict[str, Any]:
		return self.__dict__.copy()


@dataclass
class RegisterFixture:
	first_name: str
	last_name: str
	email: str
	phone: str
	password: str
	address: Address

	def to_dict(self) -> dict[str, Any]:
		return {
			"firstName": self.first_name,
			"lastName": self.last_name,
			"email": self.email,
			"phone": self.phone,
			"password": self.password,
			"address": self.address.to_dict(),
		}


@dataclass
class ProductFixture:
	name: str
	description: str
	price: float
	stock_quantity: int
	category: str
	image_url: str
	id: Optional[int] = None

	def to_dict(self) -> dict[str, Any]:
		return {
			"name": self.name,
			"description": self.description,
			"price": self.price,
			"stockQuantity": self.stock_quantity,
			"category": self.category,
			"imageUrl": self.image_url,
		}


@dataclass
class UserSession:
	email: str
	password: str
	user_id: int
	access_token: str
	refresh_token: str
	role: str


@dataclass
class CartLine:
	product_id: int
	quantity: int

	def to_dict(self) -> dict[str, Any]:
		return {"productId": self.product_id, "quantity": self.quantity}


class ApiError(RuntimeError):
	def __init__(self, message: str, status_code: Optional[int] = None, body: str = ""):
		super().__init__(message)
		self.status_code = status_code
		self.body = body


class FailureRecorder:
	def __init__(self, path: Path):
		self.path = path
		self.path.parent.mkdir(parents=True, exist_ok=True)
		self._lock = threading.Lock()
		self._logger = logging.getLogger("bulk_import.failures")

	def write(self, payload: dict[str, Any]) -> None:
		line = json.dumps(payload, ensure_ascii=False)
		with self._lock:
			with self.path.open("a", encoding="utf-8") as handle:
				handle.write(line + "\n")
		self._logger.error(line)


class ApiClient:
	def __init__(self, base_url: str, token: Optional[str], attempts: int, rate_limit_delay: float, failures: FailureRecorder):
		self.base_url = base_url.rstrip("/")
		self.attempts = max(1, attempts)
		self.rate_limit_delay = rate_limit_delay
		self.failures = failures
		self.session = requests.Session()
		self.session.headers.update({"Accept": "application/json"})
		if token:
			self.session.headers.update({"Authorization": f"Bearer {token}"})

	def request_json(
		self,
		method: str,
		path: str,
		*,
		json_body: Any = None,
		headers: Optional[dict[str, str]] = None,
		expected: Iterable[int] = (200,),
		context: Optional[dict[str, Any]] = None,
	) -> Any:
		url = f"{self.base_url}{path}"
		expected_set = set(expected)
		context = context or {}

		for attempt in range(1, self.attempts + 1):
			try:
				kwargs: dict[str, Any] = {"timeout": 30}
				if headers:
					kwargs["headers"] = headers
				if json_body is not None:
					kwargs["json"] = json_body
				response = self.session.request(method, url, **kwargs)
			except RequestException as exc:
				if attempt >= self.attempts:
					self._record(method, path, None, context, f"request error: {exc}")
					raise ApiError(f"{method} {path} failed: {exc}") from exc
				time.sleep(self._backoff(attempt))
				continue

			if response.status_code == 429:
				if attempt >= self.attempts:
					self._record(method, path, response, context, "rate limited")
					raise ApiError(f"Rate limited on {method} {path}", 429, response.text)
				retry_after = self._retry_after(response.headers.get("Retry-After"))
				time.sleep(retry_after if retry_after is not None else self.rate_limit_delay * attempt)
				continue

			if 500 <= response.status_code < 600 and attempt < self.attempts:
				time.sleep(self._backoff(attempt))
				continue

			if response.status_code not in expected_set:
				self._record(method, path, response, context, "unexpected status")
				raise ApiError(f"Unexpected {response.status_code} for {method} {path}", response.status_code, response.text)

			if response.status_code == 204 or not response.text.strip():
				return None

			try:
				return response.json()
			except ValueError as exc:
				raise ApiError(f"Non-JSON response for {method} {path}", response.status_code, response.text) from exc

		raise ApiError(f"Retries exhausted for {method} {path}")

	def _record(self, method: str, path: str, response: Optional[requests.Response], context: dict[str, Any], error: str) -> None:
		self.failures.write({
			"timestamp": datetime.utcnow().isoformat() + "Z",
			"method": method,
			"path": path,
			"status_code": getattr(response, "status_code", None),
			"response_body": getattr(response, "text", None),
			"error": error,
			"context": context,
		})

	@staticmethod
	def _retry_after(value: Optional[str]) -> Optional[float]:
		if not value:
			return None
		try:
			return float(value)
		except ValueError:
			return None

	@staticmethod
	def _backoff(attempt: int) -> float:
		return min(10.0, 0.75 * (2 ** (attempt - 1)))


class BulkImporter:
	def __init__(self, args: argparse.Namespace):
		self.base_url = args.base_url
		self.admin_secret = args.admin_secret
		self.admin_email = args.admin_email
		self.admin_password = args.admin_password
		self.customer_password = args.customer_password
		self.products = args.products
		self.users = args.users
		self.items_per_user = args.cart_items_per_user
		self.cart_quantity = args.cart_quantity
		self.max_workers = max(1, min(args.max_workers, 10))
		self.batch_size = max(1, min(args.batch_size, self.max_workers))
		self.attempts = max(1, args.retries)
		self.rate_limit_delay = args.rate_limit_delay
		self.failures = FailureRecorder(Path(args.failure_log))
		self.rng = random.Random(args.seed)
		self.run_id = datetime.utcnow().strftime("%Y%m%d%H%M%S") + "-" + uuid.uuid4().hex[:8]
		self._admin_session: Optional[UserSession] = None
		self.log = logging.getLogger("bulk_import")

	def run(self) -> dict[str, Any]:
		admin = self.get_admin_session()
		products = self.create_products(self.build_products())
		users = self.create_users(self.build_users())
		carts = self.build_cart_plans(users, products)
		orders = self.seed_carts_and_orders(users, carts)
		summary = {
			"run": self.run_id,
			"admin_user_id": admin.user_id,
			"products_created": len(products),
			"users_created": len(users),
			"orders_created": len(orders),
			"failure_log": str(self.failures.path),
		}
		self.log.info("Done: %s", summary)
		return summary

	def client(self, token: Optional[str] = None) -> ApiClient:
		return ApiClient(self.base_url, token, self.attempts, self.rate_limit_delay, self.failures)

	def get_admin_session(self) -> UserSession:
		if self._admin_session:
			return self._admin_session

		payload = RegisterFixture(
			first_name="Bulk",
			last_name="Admin",
			email=self.admin_email,
			phone="9876543210",
			password=self.admin_password,
			address=Address("1 Admin Street", "Bengaluru", "Karnataka", "560001"),
		).to_dict()

		public = self.client()
		try:
			auth = public.request_json(
				"POST",
				"/api/auth/register/admin",
				json_body=payload,
				headers={"X-Admin-Secret": self.admin_secret},
				expected=(201,),
				context={"entity": "admin", "step": "register"},
			)
		except ApiError as exc:
			if exc.status_code != 409:
				raise
			auth = public.request_json(
				"POST",
				"/api/auth/login",
				json_body={"email": self.admin_email, "password": self.admin_password},
				expected=(200,),
				context={"entity": "admin", "step": "login"},
			)

		self._admin_session = self._auth_to_session(auth, self.admin_password)
		return self._admin_session

	def create_products(self, fixtures: list[ProductFixture]) -> list[ProductFixture]:
		token = self.get_admin_session().access_token
		created: list[ProductFixture] = []
		for batch in self._chunks(fixtures, self.batch_size):
			with ThreadPoolExecutor(max_workers=min(self.max_workers, len(batch))) as pool:
				futures = {pool.submit(self._create_product, token, fx): fx for fx in batch}
				for future in as_completed(futures):
					item = future.result()
					if item:
						created.append(item)
		return created

	def _create_product(self, token: str, fixture: ProductFixture) -> Optional[ProductFixture]:
		client = self.client(token)
		try:
			data = client.request_json(
				"POST",
				"/api/products",
				json_body=fixture.to_dict(),
				expected=(201,),
				context={"entity": "product", "name": fixture.name},
			)
		except ApiError:
			return None
		fixture.id = int(data["id"])
		return fixture

	def create_users(self, fixtures: list[RegisterFixture]) -> list[UserSession]:
		created: list[UserSession] = []
		for batch in self._chunks(fixtures, self.batch_size):
			with ThreadPoolExecutor(max_workers=min(self.max_workers, len(batch))) as pool:
				futures = {pool.submit(self._create_user, fx): fx for fx in batch}
				for future in as_completed(futures):
					item = future.result()
					if item:
						created.append(item)
		return created

	def _create_user(self, fixture: RegisterFixture) -> Optional[UserSession]:
		client = self.client()
		try:
			auth = client.request_json(
				"POST",
				"/api/auth/register",
				json_body=fixture.to_dict(),
				expected=(201,),
				context={"entity": "user", "email": fixture.email, "step": "register"},
			)
		except ApiError as exc:
			if exc.status_code != 409:
				return None
			try:
				auth = client.request_json(
					"POST",
					"/api/auth/login",
					json_body={"email": fixture.email, "password": fixture.password},
					expected=(200,),
					context={"entity": "user", "email": fixture.email, "step": "login"},
				)
			except ApiError:
				return None
		return self._auth_to_session(auth, fixture.password)

	def build_products(self) -> list[ProductFixture]:
		total_units = max(1, self.users * self.items_per_user * self.cart_quantity)
		stock_per_product = max(50, math.ceil(total_units / max(1, self.products)) + 10)
		categories = ["Electronics", "Home", "Fashion", "Books", "Sports"]
		out: list[ProductFixture] = []
		for i in range(1, self.products + 1):
			suffix = f"{self.run_id}-{i:04d}"
			out.append(ProductFixture(
				name=f"Bulk Product {suffix}",
				description=f"Generated product {suffix} for ecommerce import.",
				price=round(self.rng.uniform(9.99, 999.99), 2),
				stock_quantity=stock_per_product,
				category=categories[(i - 1) % len(categories)],
				image_url=f"https://example.com/assets/{suffix}.png",
			))
		return out

	def build_users(self) -> list[RegisterFixture]:
		out: list[RegisterFixture] = []
		for i in range(1, self.users + 1):
			suffix = f"{self.run_id}-{i:04d}"
			out.append(RegisterFixture(
				first_name=f"Customer{i}",
				last_name="Bulk",
				email=f"customer-{suffix}@example.com",
				phone=f"98765{i:05d}"[-10:],
				password=self.customer_password,
				address=Address(f"{i} Market Street", "Bengaluru", "Karnataka", "560001"),
			))
		return out

	def build_cart_plans(self, users: list[UserSession], products: list[ProductFixture]) -> dict[str, list[CartLine]]:
		product_ids = [p.id for p in products if p.id is not None]
		if not users or not product_ids:
			return {}
		plans: dict[str, list[CartLine]] = {}
		product_iter = cycle(product_ids)
		for user in users:
			plans[user.email] = [CartLine(next(product_iter), self.cart_quantity) for _ in range(self.items_per_user)]
		return plans

	def seed_carts_and_orders(self, users: list[UserSession], plans: dict[str, list[CartLine]]) -> list[dict[str, Any]]:
		out: list[dict[str, Any]] = []
		for batch in self._chunks(users, self.batch_size):
			with ThreadPoolExecutor(max_workers=min(self.max_workers, len(batch))) as pool:
				futures = {pool.submit(self._seed_one_user, user, plans.get(user.email, [])): user for user in batch}
				for future in as_completed(futures):
					item = future.result()
					if item:
						out.append(item)
		return out

	def _seed_one_user(self, user: UserSession, lines: list[CartLine]) -> Optional[dict[str, Any]]:
		if not lines:
			self.failures.write({
				"timestamp": datetime.utcnow().isoformat() + "Z",
				"entity": "order",
				"email": user.email,
				"error": "no cart lines",
				"context": {"step": "seed"},
			})
			return None

		client = self.client(user.access_token)
		for line in lines:
			try:
				client.request_json(
					"POST",
					"/api/cart",
					json_body=line.to_dict(),
					expected=(201,),
					context={"entity": "cart", "email": user.email, "product_id": line.product_id},
				)
			except ApiError:
				return None

		try:
			order = client.request_json(
				"POST",
				"/api/orders",
				expected=(201,),
				context={"entity": "order", "email": user.email},
			)
		except ApiError:
			return None

		return {
			"email": user.email,
			"order_id": order.get("id"),
			"totalAmount": order.get("totalAmount"),
			"status": order.get("status"),
			"items": len(order.get("items") or []),
		}

	@staticmethod
	def _auth_to_session(auth: dict[str, Any], password: str) -> UserSession:
		return UserSession(
			email=str(auth["email"]),
			password=password,
			user_id=int(auth["userId"]),
			access_token=str(auth["accessToken"]),
			refresh_token=str(auth.get("refreshToken", "")),
			role=str(auth.get("role", "")),
		)

	@staticmethod
	def _chunks(items: list[Any], size: int) -> Iterable[list[Any]]:
		for i in range(0, len(items), size):
			yield items[i:i + size]


def parse_args() -> argparse.Namespace:
	parser = argparse.ArgumentParser(description="Bulk insert ecommerce fixtures")
	parser.add_argument("--base-url", default=BASE_URL)
	parser.add_argument("--admin-secret", default=ADMIN_SECRET)
	parser.add_argument("--admin-email", default=ADMIN_EMAIL)
	parser.add_argument("--admin-password", default=ADMIN_PASSWORD)
	parser.add_argument("--customer-password", default=CUSTOMER_PASSWORD)
	parser.add_argument("--products", type=int, default=10)
	parser.add_argument("--users", type=int, default=10)
	parser.add_argument("--cart-items-per-user", type=int, default=3)
	parser.add_argument("--cart-quantity", type=int, default=1)
	parser.add_argument("--max-workers", type=int, default=5)
	parser.add_argument("--batch-size", type=int, default=5)
	parser.add_argument("--retries", type=int, default=3)
	parser.add_argument("--rate-limit-delay", type=float, default=2.0)
	parser.add_argument("--failure-log", default=FAILURE_LOG)
	parser.add_argument("--seed", type=int, default=None)
	return parser.parse_args()


def configure_logging() -> None:
	root = logging.getLogger()
	root.setLevel(logging.INFO)
	handler = logging.StreamHandler()
	handler.setFormatter(logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s"))
	root.addHandler(handler)
	logging.getLogger("requests").setLevel(logging.WARNING)


def main() -> int:
	args = parse_args()
	configure_logging()
	importer = BulkImporter(args)
	print(json.dumps(importer.run(), indent=2))
	return 0


if __name__ == "__main__":
	raise SystemExit(main())

