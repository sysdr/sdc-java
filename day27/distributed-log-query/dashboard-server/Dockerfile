FROM python:3.11-slim

WORKDIR /app

COPY dashboard-server/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY dashboard-server/app.py .
COPY dashboard-server/templates ./templates
COPY dashboard-server/static ./static

EXPOSE 5000

CMD ["python", "app.py"]

