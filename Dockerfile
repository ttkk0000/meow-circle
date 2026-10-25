# syntax=docker/dockerfile:1.7

# ---- Web source build (only when an operator builds this image) ----
FROM node:24-alpine AS web-build
WORKDIR /web-client
COPY web-client/package.json web-client/package-lock.json ./
RUN npm ci
COPY web-client/ ./
ARG VITE_MND_TERMS_URL
ARG VITE_MND_PRIVACY_URL
ARG VITE_MND_AMAP_JS_KEY
ARG VITE_MND_WECHAT_WEB_APP_ID
ARG VITE_MND_QQ_WEB_APP_ID
ARG VITE_MND_OAUTH_REDIRECT_URI
RUN npm run typecheck && npm run build

# ---- build stage ----
FROM golang:1.25-alpine AS build
WORKDIR /src

# Cache modules first for better layer reuse.
COPY go.mod go.sum ./
RUN go mod download

COPY cmd/ ./cmd/
COPY internal/ ./internal/
COPY migrations/ ./migrations/
COPY web/ ./web/
RUN CGO_ENABLED=0 GOOS=linux \
    go build -trimpath -ldflags="-s -w" -o /out/server ./cmd/server
RUN CGO_ENABLED=0 GOOS=linux \
    go build -trimpath -ldflags="-s -w" -o /out/meow-worker ./cmd/worker

RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /out/meow-migrate ./cmd/migrate
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /out/meow-healthcheck ./cmd/healthcheck
RUN mkdir -p /out/data/mobile-media /out/data/uploads /out/worker-data && chown -R 65532:65532 /out/data /out/worker-data

# ---- runtime stage ----
FROM gcr.io/distroless/static-debian12:nonroot
WORKDIR /app

# The server expects ./web (static assets) and ./migrations at the working dir.
COPY --from=build /out/server       /app/server
COPY --from=build /out/meow-worker  /app/meow-worker
COPY --from=build /out/meow-migrate /app/meow-migrate
COPY --from=build /out/meow-healthcheck /app/meow-healthcheck
COPY --from=build --chown=65532:65532 /out/data /app/data
COPY --from=build --chown=65532:65532 /out/worker-data /app/worker-data
COPY --from=web-build /web-client/dist /app/web-client/dist
COPY --from=build /src/web          /app/web
COPY --from=build /src/migrations   /app/migrations

ENV APP_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 CMD ["/app/meow-healthcheck"]

USER nonroot:nonroot
ENTRYPOINT ["/app/server"]
