for /l %%x in (1, 1, 20) do (
    start /B curl -X "POST" "http://localhost:9081/ReactiveMessaging30Demo/buffer" -H "accept: */*" -H "Content-Type: text/plain" -d "%%x" -v
)
