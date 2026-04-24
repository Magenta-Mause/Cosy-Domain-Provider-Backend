# cosy-domain-provider — Backend

Spring Boot backend that manages user-owned subdomains and syncs DNS records to AWS Route 53.

## Prerequisites

- Java 21
- Maven (or use the included `./mvnw` wrapper)
- Docker & Docker Compose (optional — only needed to run the local PostgreSQL container)
- AWS CLI v2

---

## 1. Start the database

```bash
docker compose -f infrastructure/compose.yaml up -d
```

This starts a PostgreSQL 16 container on `localhost:5432` with database `cosy`, user `cosy`, password `cosy`.

If you already have PostgreSQL running locally, skip this step and update `spring.datasource.url` in `src/main/resources/application.yaml` accordingly.

---

## 2. Configure AWS credentials

The app uses the **AWS Default Credentials Provider Chain**, which means it picks up credentials in the standard order (environment variables → `~/.aws/credentials` → IAM role, etc.).

### Option A — AWS CLI (recommended for local dev)

```bash
aws configure
```

You will be prompted for:

| Prompt | Value |
|---|---|
| AWS Access Key ID | Your IAM access key |
| AWS Secret Access Key | Your IAM secret key |
| Default region name | `eu-central-1` |
| Default output format | `json` (or leave blank) |

This writes to `~/.aws/credentials` and `~/.aws/config`, which the SDK reads automatically.

### Option B — Named profile

```bash
aws configure --profile cosy
```

Then export the profile before starting the app:

```bash
export AWS_PROFILE=cosy
```

### Option C — Environment variables (CI / containers)

```bash
export AWS_ACCESS_KEY_ID=<your-key-id>
export AWS_SECRET_ACCESS_KEY=<your-secret-key>
export AWS_DEFAULT_REGION=eu-central-1
```

### Required IAM permissions

The IAM user / role needs at minimum:

```json
{
  "Effect": "Allow",
  "Action": [
    "route53:ChangeResourceRecordSets",
    "route53:ListResourceRecordSets",
    "route53:GetHostedZone"
  ],
  "Resource": "arn:aws:route53:::hostedzone/<YOUR_HOSTED_ZONE_ID>"
}
```

---

## 3. Set required environment variables

| Variable | Description |
|---|---|
| `AWS_HOSTED_ZONE_ID` | Route 53 hosted zone ID (e.g. `Z1D633PJN98FT9`) |
| `AWS_DOMAIN` | Root domain managed by the hosted zone (e.g. `example.com`) |
| `MAIL_API_URL` | Base URL of the mail-service used for transactional emails |
| `MAIL_API_KEY` | API key for the mail service |
| `FRONTEND_URL` | *(optional)* Frontend base URL for email links and OAuth redirects — defaults to `http://localhost:5173` |
| `COSY_DOMAIN_PROVIDER_JWT_SECRET_KEY` | *(optional)* 64-char hex secret for JWT signing — a default is used if omitted |
| `OAUTH_GOOGLE_CLIENT_ID` | Google OAuth2 client ID |
| `OAUTH_GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret |
| `OAUTH_GITHUB_CLIENT_ID` | GitHub OAuth2 client ID |
| `OAUTH_GITHUB_CLIENT_SECRET` | GitHub OAuth2 client secret |
| `OAUTH_DISCORD_CLIENT_ID` | Discord OAuth2 client ID |
| `OAUTH_DISCORD_CLIENT_SECRET` | Discord OAuth2 client secret |
| `OAUTH_SECURE_COOKIE` | *(optional)* Set to `true` in production (HTTPS) — defaults to `false` |
| `STRIPE_API_KEY` | Stripe secret key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `STRIPE_PRODUCT_ID` | Stripe product ID for the Plus plan |
| `STRIPE_PRICE_ID` | Stripe price ID for the Plus plan |

Export them in your shell before starting:

```bash
export AWS_HOSTED_ZONE_ID=Z1D633PJN98FT9
export AWS_DOMAIN=example.com
```

---

## 4. Run the application

```bash
./mvnw spring-boot:run
```

Or build a JAR first:

```bash
./mvnw package -DskipTests
java -jar target/cosy-domain-provider-*.jar
```

The server starts on **http://localhost:8080**.

---

## 5. Format code

```bash
./mvnw spotless:apply   # format all Java sources (Google Java Format, AOSP style)
./mvnw spotless:check   # verify formatting without changing files (runs in CI)
```

---

## API documentation

Swagger UI is available at **http://localhost:8080/api/swagger-ui/index.html** once the app is running.
