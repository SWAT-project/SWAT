#!/bin/bash

set -e

GITHUB_ORG=informatici
OH_CORE_BRANCH=v1.12.0
OH_API_BRANCH=v1.12.0
MYSQL_DB_PROTOCOL=jdbc:mysql
MYSQL_DB_CLASS=com.mysql.cj.jdbc.Driver

pushd /home/developer/openhospital/openhospital-core

git clone --depth=1 -b ${OH_CORE_BRANCH} https://github.com/${GITHUB_ORG}/openhospital-core.git .
cp ../photo_fix.patch .
git apply *.patch
./mvnw install -DskipTests

popd

pushd /home/developer/openhospital/openhospital-api

git clone --depth=1 -b ${OH_API_BRANCH} https://github.com/${GITHUB_ORG}/openhospital-api.git .
cp ../0001-Remove-password-login-check.patch .
git apply *.patch

# Database connection must be changed in order to work in docker network
cp rsc/application.properties.dist rsc/application.properties && \
    cp rsc/database.properties.dist rsc/database.properties && \
    cp rsc/log4j.properties.dist rsc/log4j.properties && \
    cp rsc/settings.properties.dist rsc/settings.properties && \
    # sed -i 's/localhost/database/g' rsc/database.properties	&& \
    sed -i s/jdbc:mysql/${MYSQL_DB_PROTOCOL}/g rsc/database.properties && \
    printf "\njdbc.class=${MYSQL_DB_CLASS}\n" >> rsc/database.properties && \
    sed -i 's/USERSLISTLOGIN=no/USERSLISTLOGIN=yes/g' rsc/settings.properties && \
    sed -i 's/STRONGPASSWORD=yes/STRONGPASSWORD=no/g' rsc/settings.properties && \
    sed -i 's/STRONGLENGTH=10/STRONGLENGTH=3/g' rsc/settings.properties && \
    sed -i 's/log4j.logger.org.springframework=DEBUG,RollingFile/log4j.logger.org.springframework=INFO,RollingFile/g' rsc/log4j.properties && \
    sed -i 's/logger name="org.springframework.web" level="DEBUG"/logger name="org.springframework.web" level="INFO"/g' src/main/resources/logback-spring.xml && \
    sed -i 's/logger name="org.isf" level="DEBUG"/logger name="org.isf" level="INFO"/g' src/main/resources/logback-spring.xml && \
    sed -i 's/logger name="org.hibernate.SQL" level="INFO"/logger name="org.hibernate.SQL" level="WARN"/g' src/main/resources/logback-spring.xml && \
    sed -i 's/JWT_TOKEN_SECRET/eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzb21lIjoicGF5bG9hZCJ9Joh1R2dYzkRvDkqv3sygm5YyK8Gi4ShZqbhK2gxcs2U/g' rsc/application.properties
    # sed -i 's/DEBUG=no/DEBUG=yes/g' rsc/settings.properties && \
./mvnw install -DskipTests

pushd rsc
jar -u -f ../target/openhospital-api-0.0.2.jar *.properties

popd
popd

# WORKDIR /openhospital-api/target
# CMD java -cp "openhospital-api-0.0.2.jar:rsc/:static/" org.springframework.boot.loader.JarLauncher