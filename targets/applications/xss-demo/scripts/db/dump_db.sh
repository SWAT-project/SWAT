#!/bin/bash

cd "$(dirname "$0")"

docker exec -i xss-demo-mysql mysqldump -uroot -proot xss_demo > ../../.data/db_dump.sql