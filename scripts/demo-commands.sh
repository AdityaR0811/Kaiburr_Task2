#!/bin/bash

# Kaiburr Task 1 - Demo Commands Script
# Author: Aditya R.
# This script demonstrates all API endpoints sequentially

set -e

BASE_URL="http://localhost:8080/api"
CORRELATION_ID="demo-$(date +%s)"

echo "========================================="
echo "  Kaiburr Task 1 - API Demo"
echo "  Author: Aditya R."
echo "  Base URL: $BASE_URL"
echo "  Correlation ID: $CORRELATION_ID"
echo "========================================="
echo ""

# Function to make requests with correlation ID
make_request() {
    echo "-------------------------------------------"
    echo "$1"
    echo "-------------------------------------------"
    curl -s -X "$2" "$BASE_URL$3" \
        -H "Content-Type: application/json" \
        -H "X-Correlation-Id: $CORRELATION_ID" \
        ${4:+-d "$4"} | jq '.'
    echo ""
    sleep 1
}

# 1. Create Task 1
echo "1. Creating Task: Echo Hello"
make_request "POST /api/tasks" "PUT" "/tasks" '{
  "id": "task-echo",
  "name": "Echo Hello Task",
  "owner": "Aditya R.",
  "command": "echo Hello from Kaiburr"
}'

# 2. Create Task 2
echo "2. Creating Task: System Info"
make_request "POST /api/tasks" "PUT" "/tasks" '{
  "id": "task-uname",
  "name": "System Info Task",
  "owner": "Aditya R.",
  "command": "uname -a"
}'

# 3. Create Task 3
echo "3. Creating Task: Date"
make_request "POST /api/tasks" "PUT" "/tasks" '{
  "id": "task-date",
  "name": "Date Task",
  "owner": "Aditya R.",
  "command": "date"
}'

# 4. Get All Tasks
echo "4. Getting All Tasks"
make_request "GET /api/tasks" "GET" "/tasks?page=0&size=10"

# 5. Get Task by ID
echo "5. Getting Task by ID: task-echo"
make_request "GET /api/tasks/task-echo" "GET" "/tasks/task-echo"

# 6. Search Tasks
echo "6. Searching Tasks by name: 'Task'"
make_request "GET /api/tasks/search?name=Task" "GET" "/tasks/search?name=Task"

# 7. Validate Command (Valid)
echo "7. Validating Command (Valid): echo test"
make_request "POST /api/validation/command" "POST" "/validation/command" '{
  "command": "echo test"
}'

# 8. Validate Command (Invalid)
echo "8. Validating Command (Invalid): rm -rf /"
make_request "POST /api/validation/command" "POST" "/validation/command" '{
  "command": "rm -rf /"
}'

# 9. Execute Task
echo "9. Executing Task: task-echo"
make_request "PUT /api/tasks/task-echo/executions" "PUT" "/tasks/task-echo/executions"

# 10. Execute Another Task
echo "10. Executing Task: task-date"
make_request "PUT /api/tasks/task-date/executions" "PUT" "/tasks/task-date/executions"

# 11. Get Task with Executions
echo "11. Getting Task with Execution History: task-echo"
make_request "GET /api/tasks/task-echo" "GET" "/tasks/task-echo"

# 12. Try Creating Invalid Task (should fail)
echo "12. Attempting to Create Invalid Task (should fail)"
make_request "POST /api/tasks (INVALID)" "PUT" "/tasks" '{
  "id": "task-evil",
  "name": "Evil Task",
  "owner": "Hacker",
  "command": "rm -rf /"
}' || echo "✓ Command validation rejected as expected"

# 13. Delete Task
echo "13. Deleting Task: task-uname"
curl -s -X DELETE "$BASE_URL/tasks/task-uname" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    -w "HTTP Status: %{http_code}\n"
echo ""

# 14. Try Getting Deleted Task (should 404)
echo "14. Attempting to Get Deleted Task (should 404)"
curl -s -X GET "$BASE_URL/tasks/task-uname" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    -w "HTTP Status: %{http_code}\n" | jq '.'
echo ""

# 15. Health Check
echo "15. Health Check"
echo "-------------------------------------------"
curl -s http://localhost:8080/actuator/health | jq '.'
echo ""

echo "========================================="
echo "  Demo Completed!"
echo "  Check audit.log.jsonl for audit trail"
echo "========================================="
