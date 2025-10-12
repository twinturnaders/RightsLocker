FROM ubuntu:latest
LABEL authors="Administrator"

ENTRYPOINT ["top", "-b"]

FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build
# serve
FROM nginx:alpine
COPY --from=build /app/dist/*/browser /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/app.conf

