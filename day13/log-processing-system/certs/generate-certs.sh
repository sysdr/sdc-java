#!/bin/bash

set -e

echo "🔐 Generating TLS certificates for distributed log processing system..."

PASSWORD="changeit"
VALIDITY_DAYS=365

# Create root CA
echo "Generating Root CA..."
openssl genrsa -out root-ca-key.pem 4096
openssl req -x509 -new -key root-ca-key.pem -days 3650 -out root-ca.pem \
  -subj "/C=US/ST=California/L=SanFrancisco/O=LogProcessing/CN=RootCA"

# Create intermediate CA
echo "Generating Intermediate CA..."
openssl genrsa -out intermediate-ca-key.pem 4096
openssl req -new -key intermediate-ca-key.pem -out intermediate-ca.csr \
  -subj "/C=US/ST=California/L=SanFrancisco/O=LogProcessing/CN=IntermediateCA"
openssl x509 -req -in intermediate-ca.csr -CA root-ca.pem -CAkey root-ca-key.pem \
  -CAcreateserial -days 1825 -out intermediate-ca.pem

# Create full chain
cat intermediate-ca.pem root-ca.pem > ca-chain.pem

# Function to generate service certificate
generate_service_cert() {
  local service=$1
  echo "Generating certificate for $service..."
  
  openssl genrsa -out ${service}-key.pem 2048
  openssl req -new -key ${service}-key.pem -out ${service}.csr \
    -subj "/C=US/ST=California/L=SanFrancisco/O=LogProcessing/CN=${service}"
  
  cat > ${service}-ext.cnf << EXTEOF
subjectAltName = DNS:${service},DNS:localhost,IP:127.0.0.1
EXTEOF
  
  openssl x509 -req -in ${service}.csr -CA intermediate-ca.pem \
    -CAkey intermediate-ca-key.pem -CAcreateserial -days ${VALIDITY_DAYS} \
    -out ${service}-cert.pem -extfile ${service}-ext.cnf
  
  # Create PKCS12 keystore
  openssl pkcs12 -export -in ${service}-cert.pem -inkey ${service}-key.pem \
    -out ${service}.p12 -name ${service} -CAfile ca-chain.pem -caname root \
    -password pass:${PASSWORD}
  
  # Convert to JKS
  keytool -importkeystore -deststorepass ${PASSWORD} -destkeypass ${PASSWORD} \
    -destkeystore ${service}.keystore.jks -srckeystore ${service}.p12 \
    -srcstoretype PKCS12 -srcstorepass ${PASSWORD} -alias ${service} -noprompt
  
  rm ${service}.csr ${service}-ext.cnf
}

# Generate certificates for each service
generate_service_cert "kafka"
generate_service_cert "log-producer"
generate_service_cert "log-consumer"
generate_service_cert "api-gateway"

# Create truststore with CA chain
echo "Creating truststore..."
# Remove existing truststore if it exists
rm -f truststore.jks
keytool -import -trustcacerts -alias root-ca -file root-ca.pem \
  -keystore truststore.jks -storepass ${PASSWORD} -noprompt
keytool -import -trustcacerts -alias intermediate-ca -file intermediate-ca.pem \
  -keystore truststore.jks -storepass ${PASSWORD} -noprompt

# Create Kafka-specific stores
cp kafka.keystore.jks kafka.server.keystore.jks
cp truststore.jks kafka.server.truststore.jks

# Copy truststore for all services
for service in log-producer log-consumer api-gateway; do
  cp truststore.jks ${service}.truststore.jks
done

# Create combined keystore and truststore for easy deployment
cp log-producer.keystore.jks keystore.jks

echo "✅ Certificate generation complete!"
echo ""
echo "Generated certificates:"
ls -lh *.jks *.pem | awk '{print $9, $5}'
echo ""
echo "Certificate validity: ${VALIDITY_DAYS} days"
echo "Keystore/Truststore password: ${PASSWORD}"
