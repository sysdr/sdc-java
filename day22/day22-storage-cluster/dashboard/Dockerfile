FROM node:18-alpine

WORKDIR /app

COPY dashboard/package.json .
RUN npm install

COPY dashboard/ .

EXPOSE 8888

CMD ["npm", "start"]

