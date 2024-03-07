for count in {1..20}
do
  curl "http://localhost:9081/ReactiveMessaging30Demo/latest" -H "accept: */*" -H "Content-Type: text/plain" -d $count -v &
done