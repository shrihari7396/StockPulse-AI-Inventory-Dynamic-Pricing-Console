#!/bin/bash

# StockPulse - Set Environment Variables for Current Shell Session
# Updated to read values from .env file instead of hardcoding

echo "Setting StockPulse AI Environment Variables from .env..."

# Define the path to the .env file (relative to project root)
ENV_FILE="../.env"

# Check if .env file exists
if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: .env file not found at $ENV_FILE"
    echo "Please create a .env file based on ../.env.template"
    exit 1
fi

# Function to parse .env file
parse_env_file() {
    local file="$1"
    
    # Skip empty lines and comments, then parse KEY=VALUE pairs
    grep -v '^\s*#' "$file" | grep -v '^\s*$' | while IFS='=' read -r key value; do
        # Remove leading/trailing whitespace
        key=$(echo "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        
        # Remove quotes if present
        if [[ $value =~ ^\".*\"$ ]] || [[ $value =~ ^\'.*\'$ ]]; then
            value="${value%\"}"
            value="${value#\"}"
        fi
        
        # Export the variable
        export "$key=$value"
        echo "Set $key = $value"
    done
}

# Load environment variables from .env file
if parse_env_file "$ENV_FILE"; then
    echo "Environment configured successfully from .env file!"
    echo ""
    echo "Active Environment Variables:"
    echo "LLM_PROVIDER   = $LLM_PROVIDER"
    echo "LLM_MODEL      = $LLM_MODEL"
    echo "LLM_BASE_URL   = $LLM_BASE_URL"
    echo "LLM_API_KEY    = (set securely from .env)"
    echo ""
    echo "You can now run: cd ../Backend && ./mvnw spring-boot:run"
else
    echo "ERROR: Failed to load environment variables from .env file"
    exit 1
fi