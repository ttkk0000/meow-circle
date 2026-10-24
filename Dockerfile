# syntax=docker/dockerfile:1.7

# ---- Web source build (only when an operator builds this image) ----
FROM node:24-alpine AS web-build
WORKDIR /web-client
COPY web-client/package.json web-client/package-lock.json ./
RUN npm ci
COPY web-client/ ./
ARG VITE_MND_TERMS_URL
ARG VITE_MND_PRIVACY_URL
ARG VITE_MND_MAP_EMBED_TEMPLATE
RUN npm run build

# ---- build stage ----
FROM golang:1.25-alpine AS build
WORKDIR /src

# Cache modules first for better layer reuse.
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux \
    go build -trimpath -ldflags="-s -w" -o /out/server ./cmd/server
RUN CGO_ENABLED=0 GOOS=linux \
    go build -trimpath -ldflags="-s -w" -o /out/meow-worker ./cmd/worker

# ---- runtime stage ----
FROM gcr.io/distroless/static-debian12:nonroot
WORKDIR /app

# The server expects ./web (static assets) and ./migrations at the working dir.
COPY --from=build /out/server       /app/server
COPY --from=build /out/meow-worker  /app/meow-worker
COPY --from=web-build /web-client/dist /app/web-client/dist
COPY --from=build /src/web          /app/web
COPY --from=build /src/migrations   /app/migrations

ENV APP_PORT=8080
EXPOSE 8080

USER nonroot:nonroot
ENTRYPOINT ["/app/server"]
