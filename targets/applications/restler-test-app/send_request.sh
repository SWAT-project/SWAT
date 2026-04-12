# curl -i -H "Accept: application/json" \
#   -H "Content-Type:application/json" \
#   -H "DE-UZL-ITS-SWAT-REQUEST-SEQUENCE-NUMBER: 0" \
#   -H "DE-UZL-ITS-SWAT-SEQUENCE-UUID: 123" \
#   "http://localhost:8091/books/new?filter=None"

curl -i -H "Accept: application/json" \
  -H "Content-Type:application/json" \
  -H "DE-UZL-ITS-SWAT-REQUEST-SEQUENCE-NUMBER: 0" \
  -H "DE-UZL-ITS-SWAT-SEQUENCE-UUID: 123" \
  "http://localhost:8091/dvds/classics/1900/1800"
