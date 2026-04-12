#!/bin/bash

docker exec -it xss-demo-mysql mysql -uroot -proot xss_demo -e "DESCRIBE comments; SELECT * FROM comments;"