# Start dependencies with docker-compose

From the project root directory (same level as `docker-compose.yaml`), run:

```bash
docker compose up -d
```

**Recommendation:** After first startup, wait a few seconds for MySQL healthcheck to pass and for the broker to register with the name server before starting the application.

# Start the Spring Boot application

`docker compose` does **not** include this project's API service. Start the app locally after dependencies are ready (it connects to `localhost` endpoints defined in `application.yaml`).

From the project root, run:

```bash
mvn spring-boot:run
```

The service listens on **http://localhost:8080** by default.

# Shutdown

## Stop Spring Boot

In the terminal running `spring-boot:run`, press **Ctrl+C**.

## Stop and remove Compose containers (optional: remove volumes)

Stop containers and keep data volumes:

```bash
docker compose down
```

Stop containers and remove Compose-created volumes (MySQL / Redis data will be deleted):

```bash
docker compose down -v
```

# Test APIs with curl

Assume base URL is `http://localhost:8080` and `Content-Type` is JSON.

### Create users

```bash
curl -s -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"alice\",\"initialBalance\":1000}"
```

```bash
curl -s -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"bob\",\"initialBalance\":500}"
```

## Get balance

```bash
curl -s http://localhost:8080/users/alice/balance
```

## Create transfer

```bash
curl -s -X POST http://localhost:8080/transfers \
  -H "Content-Type: application/json" \
  -d "{\"fromUserId\":\"alice\",\"toUserId\":\"bob\",\"amount\":100}"
```

Balances are updated asynchronously through RocketMQ. If you query immediately, wait a moment and try again.

## List transfer history

```bash
curl -s "http://localhost:8080/transfers?userId=alice&page=0&size=20"
```

### Cancel transfer

Replace `{transferId}` with the `id` returned by create transfer:

```bash
curl -s -X POST http://localhost:8080/transfers/{transferId}/cancel
```

# Connect to MySQL and run SQL

Recommended way (connect directly into the MySQL container):

```bash
docker exec -it mysql mysql -u taskuser -ptaskpass taskdb
```

Then run SQL commands, for example:

```sql
SHOW TABLES;
SELECT * FROM users;
SELECT * FROM transfers;
```

# Connect to Redis

```bash
docker exec -it redis redis-cli
```

Then run Redis commands, for example:

List the user balances cache:
```redis
KEYS user-balance::*
```

Get a key
```redis
GET user-balance::alice
```

Get the TTL of a key
```redis
TTL user-balance::alice
```
