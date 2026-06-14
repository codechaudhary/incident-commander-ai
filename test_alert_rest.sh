#!/bin/bash

BASE_URL="http://localhost:8083/api/v1/alerts"
echo "==========================================="
echo "1. GET all alerts (Should be exactly 4)"
echo "==========================================="
ALL_ALERTS=$(curl -s "$BASE_URL")
TOTAL=$(echo "$ALL_ALERTS" | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)
echo "Total Alerts Created: $TOTAL"

echo -e "\n==========================================="
echo "2. Check Severity Parsing"
echo "==========================================="
echo "$ALL_ALERTS" | grep -Eo '"traceId":"[^"]+","severity":"[^"]+"' | sed 's/"//g' | sort

echo -e "\n==========================================="
echo "3. Extract one alertId"
echo "==========================================="
ALERT_ID=$(echo "$ALL_ALERTS" | grep -o '"alertId":"ALT-[^"]*"' | head -1 | cut -d':' -f2 | sed 's/"//g')
echo "Extracted ALERT_ID: $ALERT_ID"

echo -e "\n==========================================="
echo "4. PATCH Alert to ACKNOWLEDGED"
echo "==========================================="
curl -s -X PATCH "$BASE_URL/$ALERT_ID/status" \
     -H "Content-Type: application/json" \
     -d '{"status":"ACKNOWLEDGED"}' | grep -Eo '"status":"ACKNOWLEDGED"' || echo "Failed to ACK"

echo -e "\n==========================================="
echo "5. PATCH Alert back to OPEN (Should FAIL with 400)"
echo "==========================================="
curl -s -w "\nHTTP Status: %{http_code}\n" -X PATCH "$BASE_URL/$ALERT_ID/status" \
     -H "Content-Type: application/json" \
     -d '{"status":"OPEN"}'

echo -e "\n==========================================="
echo "6. PATCH Alert to RESOLVED"
echo "==========================================="
curl -s -X PATCH "$BASE_URL/$ALERT_ID/status" \
     -H "Content-Type: application/json" \
     -d '{"status":"RESOLVED"}' | grep -Eo '"status":"RESOLVED"' || echo "Failed to RESOLVE"

echo -e "\n==========================================="
echo "7. PATCH RESOLVED Alert to ACKNOWLEDGED (Should FAIL with 400)"
echo "==========================================="
curl -s -w "\nHTTP Status: %{http_code}\n" -X PATCH "$BASE_URL/$ALERT_ID/status" \
     -H "Content-Type: application/json" \
     -d '{"status":"ACKNOWLEDGED"}'

echo -e "\n==========================================="
echo "8. GET Filter by status=RESOLVED"
echo "==========================================="
curl -s "$BASE_URL?status=RESOLVED" | grep -Eo '"totalElements":[0-9]+'
