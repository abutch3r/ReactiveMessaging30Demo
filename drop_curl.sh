for count in {1..5}
do
  curl "http://localhost:9081/ReactiveMessaging30Demo/drop" -H "accept: */*" -H "Content-Type: text/plain" -d $count -v &
done