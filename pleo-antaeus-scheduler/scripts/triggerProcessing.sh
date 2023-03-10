curl --location 'http://pubsub:8085/v1/projects/pleo/topics/billing_trigger:publish' \
  --header 'Content-Type: application/json' \
  --data '{
      "messages": [
          {
              "data": "dGVzdCBtZXNzYWdl"
          }
      ]
  }'