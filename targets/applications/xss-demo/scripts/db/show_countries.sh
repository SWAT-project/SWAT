#!/bin/bash

docker exec -it xss-demo-mysql mysql -uroot -proot xss_demo -e "DESCRIBE countries; SELECT * FROM countries;"