# syntax=docker/dockerfile:1.7

# ---- build stage ----
FROM golang:1.26-alpine AS build
WORKDIR /src

# Cache modules first for better layer reuse.
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux \
    go build -trimpath -ldflags="-s -w" -o /out/server ./cmd/server

# ---- runtime stage ----
FROM gcr.io/distroless/static-debian12:nonroot
WORKDIR /app

# The server expects ./web (static assets) and ./migrations at the working dir.
COPY --from=build /out/server       /app/server
COPY --from=build /src/web          /app/web
COPY --from=build /src/migrations   /app/migrations

ENV APP_PORT=8080
EXPOSE 8080

USER nonroot:nonroot
ENTRYPOINT ["/app/server"]
