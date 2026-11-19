#!/usr/bin/env bash
set -euo pipefail

# Deploy frontend to Google Cloud Run
# Usage: ./deploy.sh [PROJECT_ID] [REGION] [BACKEND_URL]

PROJECT_ID="${1:-${GCP_PROJECT_ID:-this-or-that-515022}}"
REGION="${2:-${GCP_REGION:-us-central1}}"
BACKEND_URL="${3:-${VUE_APP_API_PATH:-https://this-or-that-api-132973189078.us-central1.run.app}}"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/this-or-that/frontend"

echo "=== Deploying this-or-that Frontend to Cloud Run ==="
echo "Project ID:  ${PROJECT_ID}"
echo "Region:      ${REGION}"
echo "Backend URL: ${BACKEND_URL}"
echo "Image:       ${IMAGE}"

# Configure Docker auth
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

# Build Docker image
docker build \
  --build-arg VUE_APP_API_PATH="${BACKEND_URL}" \
  -t "${IMAGE}:latest" \
  .

# Push Docker image
docker push "${IMAGE}:latest"

# Deploy to Cloud Run
gcloud run deploy this-or-that-web \
  --image "${IMAGE}:latest" \
  --region "${REGION}" \
  --project "${PROJECT_ID}" \
  --port 8080 \
  --memory 256Mi \
  --min-instances 0 \
  --max-instances 2 \
  --allow-unauthenticated

echo "=== Deployment complete ==="
