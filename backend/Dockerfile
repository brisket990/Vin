# --- deps + build stage -----------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- runtime stage -----------------------------------------------------------
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production

COPY package*.json ./
RUN npm ci --omit=dev

COPY --from=build /app/dist ./dist
COPY --from=build /app/src/db/migrations ./src/db/migrations

RUN mkdir -p /data/photos
VOLUME /data/photos

EXPOSE 3000
# Apply pending Drizzle migrations, then start the API.
CMD ["sh", "-c", "node dist/db/migrate.js && node dist/main.js"]
