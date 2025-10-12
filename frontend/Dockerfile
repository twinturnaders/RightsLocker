FROM ubuntu:latest
LABEL authors="Administrator"

ENTRYPOINT ["top", "-b"]
# build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build  # outputs dist/<app-name>/browser

# serve
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/rightslocker-web/browser /usr/share/nginx/html
