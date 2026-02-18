# ── Stage 1: Build ─────────────────────────────
FROM clojure:temurin-21-tools-deps-alpine AS builder

ENV TAILWIND_VERSION=v3.4.17

# Install Tailwind (required for CSS asset generation during uberjar build)
RUN apk add curl rlwrap && curl -L -o /usr/local/bin/tailwindcss \
  https://github.com/tailwindlabs/tailwindcss/releases/download/$TAILWIND_VERSION/tailwindcss-linux-x64 \
  && chmod +x /usr/local/bin/tailwindcss

WORKDIR /build

# Cache dependency resolution
COPY deps.edn ./
RUN clojure -P -M:dev

# Copy source and build uberjar
COPY src ./src
COPY dev ./dev
COPY resources ./resources
RUN clj -M:dev uberjar

# ── Stage 2: Runtime ───────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# Copy the built uberjar from builder stage
COPY --from=builder /build/target/jar/app.jar app.jar

# Biff prod config defaults to localhost; override for Docker networking
ENV BIFF_PROFILE=prod
ENV HOST=0.0.0.0
ENV PORT=8080

# XTDB data persistence
RUN mkdir -p /app/data && chown -R app:app /app
VOLUME /app/data

USER app
EXPOSE 8080

# JVM flags for small containers
CMD ["java", \
     "-XX:+UseG1GC", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:+UseStringDeduplication", \
     "-XX:-OmitStackTraceInFastThrow", \
     "-XX:+CrashOnOutOfMemoryError", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-jar", "app.jar"]
