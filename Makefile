API_URL ?= http://localhost:8000/api
JSON_PATH ?= /app/data/whyuon.json

.PHONY: up down logs load-data

up:
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f backend frontend db

load-data:
	curl -fsS -X POST "$(API_URL)/import?path=$(JSON_PATH)"
