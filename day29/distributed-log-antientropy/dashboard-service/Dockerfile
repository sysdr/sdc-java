FROM node:18-alpine

WORKDIR /app

COPY dashboard-service/package.json ./
RUN npm install

COPY dashboard-service/ .

EXPOSE 8088

CMD ["npm", "start"]

