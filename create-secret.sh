#!/bin/bash
# Creates (or updates) the groceryai-secret in the groceryai namespace.
# Run this once before the first deploy and again whenever any value changes.

set -e

NAMESPACE=groceryai
SECRET_NAME=groceryai-secret

echo "GroceryAI Secret Creator"
echo "========================"
echo ""
echo "Each value will be prompted."
echo ""

read -rp "Google OAuth2 Client ID: " GOOGLE_CLIENT_ID
read -rp "Google OAuth2 Client Secret: " GOOGLE_CLIENT_SECRET
read -rp "Anthropic API Key: " ANTHROPIC_API_KEY
read -rp "Google Sheet ID (nutrition spreadsheet): " SHEET_ID

echo ""
read -rp "Sheet tab name [GroceryAI]: " SHEET_TAB
SHEET_TAB=${SHEET_TAB:-GroceryAI}

read -rp "Anthropic model [claude-haiku-4-5-20251001]: " ANTHROPIC_MODEL
ANTHROPIC_MODEL=${ANTHROPIC_MODEL:-claude-haiku-4-5-20251001}

read -rp "Anthropic max tokens [4096]: " ANTHROPIC_MAX_TOKENS
ANTHROPIC_MAX_TOKENS=${ANTHROPIC_MAX_TOKENS:-4096}

read -rp "USDA FoodData Central API Key [DEMO_KEY]: " USDA_API_KEY
USDA_API_KEY=${USDA_API_KEY:-DEMO_KEY}

# Validate required fields
for var in GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET ANTHROPIC_API_KEY SHEET_ID; do
    if [ -z "${!var}" ]; then
        echo "Error: $var is required"
        exit 1
    fi
done

echo ""
echo "Creating namespace '$NAMESPACE' if it does not exist…"
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

echo "Applying secret '$SECRET_NAME' in namespace '$NAMESPACE'…"

kubectl create secret generic "$SECRET_NAME" \
    --namespace="$NAMESPACE" \
    --from-literal=GOOGLE_CLIENT_ID="$GOOGLE_CLIENT_ID" \
    --from-literal=GOOGLE_CLIENT_SECRET="$GOOGLE_CLIENT_SECRET" \
    --from-literal=ANTHROPIC_API_KEY="$ANTHROPIC_API_KEY" \
    --from-literal=SHEET_ID="$SHEET_ID" \
    --from-literal=SHEET_TAB="$SHEET_TAB" \
    --from-literal=ANTHROPIC_MODEL="$ANTHROPIC_MODEL" \
    --from-literal=ANTHROPIC_MAX_TOKENS="$ANTHROPIC_MAX_TOKENS" \
    --from-literal=USDA_API_KEY="$USDA_API_KEY" \
    --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "✓ Secret '$SECRET_NAME' applied in namespace '$NAMESPACE'"
echo ""
echo "To verify:"
echo "  kubectl get secret $SECRET_NAME -n $NAMESPACE"
echo "  kubectl describe secret $SECRET_NAME -n $NAMESPACE"
