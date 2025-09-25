#!/bin/sh

# Environment configuration script for React frontend
# This script injects environment variables into the built React app

# Default values
REACT_APP_MAIN_API_URL=${REACT_APP_MAIN_API_URL:-"http://localhost:8080/api/v1"}
REACT_APP_CUSTOMER_API_URL=${REACT_APP_CUSTOMER_API_URL:-"http://localhost:8081/api/v1/customer"}
REACT_APP_ENVIRONMENT=${REACT_APP_ENVIRONMENT:-"development"}
REACT_APP_VERSION=${REACT_APP_VERSION:-"1.0.0"}

# Create environment configuration file
cat > /usr/share/nginx/html/env-config.js << EOF
window._env_ = {
  REACT_APP_MAIN_API_URL: "${REACT_APP_MAIN_API_URL}",
  REACT_APP_CUSTOMER_API_URL: "${REACT_APP_CUSTOMER_API_URL}",
  REACT_APP_ENVIRONMENT: "${REACT_APP_ENVIRONMENT}",
  REACT_APP_VERSION: "${REACT_APP_VERSION}"
};
EOF

echo "Environment configuration created:"
cat /usr/share/nginx/html/env-config.js
