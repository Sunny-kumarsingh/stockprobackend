#!/bin/bash
# ============================================================
# StockPro — Push Docker Images to Docker Hub
# Usage: bash push-to-dockerhub.sh <your-dockerhub-username>
# Example: bash push-to-dockerhub.sh sunnysingh
# ============================================================

DOCKERHUB_USER=${1:?"Usage: $0 <dockerhub-username>"}
VERSION="latest"

SERVICES=(
  "alert-service"
  "analytics-service"
  "api-gateway"
  "authservice"
  "eureka-service"
  "payment-service"
  "product-service"
  "purchase-service"
  "stockmovement-services"
  "supplier-service"
  "warehouse-service"
)

echo " Logging into Docker Hub..."
docker login

echo ""
echo " Pushing images as: ${DOCKERHUB_USER}/stockpro-<service>:latest"
echo ""

for svc in "${SERVICES[@]}"; do
  LOCAL_TAG="stockpro-backend-${svc}:${VERSION}"
  REMOTE_TAG="${DOCKERHUB_USER}/stockpro-${svc}:${VERSION}"

  echo " Tagging: ${LOCAL_TAG} → ${REMOTE_TAG}"
  docker tag "${LOCAL_TAG}" "${REMOTE_TAG}"

  echo "⬆  Pushing: ${REMOTE_TAG}"
  docker push "${REMOTE_TAG}"

  echo " Done: ${REMOTE_TAG}"
  echo "---"
done

echo ""
echo " All images pushed to Docker Hub!"
echo ""
echo " Railway mein ye image URLs use karo:"
for svc in "${SERVICES[@]}"; do
  echo "  ${DOCKERHUB_USER}/stockpro-${svc}:latest"
done
