FROM node:22-alpine AS build
WORKDIR /app

ARG VUE_APP_API_PATH
ENV VUE_APP_API_PATH=${VUE_APP_API_PATH}

COPY package*.json ./
RUN npm install --legacy-peer-deps

COPY . .

# Inject VUE_APP_API_PATH if supplied as build argument
RUN if [ -n "$VUE_APP_API_PATH" ]; then sed -i "s|VUE_APP_API_PATH=.*|VUE_APP_API_PATH=\"$VUE_APP_API_PATH\"|g" .env.production; fi

RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
