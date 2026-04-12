FROM ubuntu:22.04

SHELL ["/bin/bash", "-c"]

# ARG GITHUB_ORG=informatici
# ARG OH_CORE_BRANCH=develop
# ARG OH_API_BRANCH=develop
# ARG MYSQL_DB_PROTOCOL=jdbc:mysql
# ARG MYSQL_DB_CLASS=com.mysql.cj.jdbc.Driver

ARG USER_ID=1000
ARG GROUP_ID=1000
ENV TZ=Europe/Berlin

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN groupadd -g $GROUP_ID developer \
    && useradd developer -u $USER_ID -g $GROUP_ID -m -s /bin/bash \
    && echo "developer:pass" | chpasswd \
    && mkdir -p /home/developer/openhospital/openhospital-core \
    && mkdir -p /home/developer/openhospital/openhospital-api \
    && chown -R developer:developer /home/developer \
    && chsh -s /bin/bash developer

RUN apt-get update && \
    apt-get install -y \
    git \
    wget \
    unzip \
    openjdk-17-jdk

# WORKDIR /jacoco
# RUN wget https://repo1.maven.org/maven2/org/jacoco/jacoco/0.8.8/jacoco-0.8.8.zip
# RUN unzip jacoco-0.8.8.zip

USER developer
WORKDIR /home/developer/openhospital
COPY patches/photo_fix.patch .
COPY patches/0001-Remove-password-login-check.patch .
COPY build_openhospital_backend.sh .


#WORKDIR /openhospital-core
#RUN git clone --depth=1 -b ${OH_CORE_BRANCH} https://github.com/${GITHUB_ORG}/openhospital-core.git .
#
#COPY photo_fix.patch .
#RUN git apply *.patch
#
#RUN ./mvnw install -DskipTests
#
#WORKDIR /openhospital-api
#RUN git clone --depth=1 -b ${OH_API_BRANCH} https://github.com/${GITHUB_ORG}/openhospital-api.git .
#
#COPY 0001-Remove-password-login-check.patch .
#RUN git apply *.patch
#
## Database connection must be changed in order to work in docker network
#RUN cp rsc/application.properties.dist rsc/application.properties               && \
#    cp rsc/database.properties.dist rsc/database.properties 	  		&& \
#    cp rsc/log4j.properties.dist rsc/log4j.properties 		  		&& \
#    cp rsc/settings.properties.dist rsc/settings.properties 	  		&& \
#    sed -i 's/localhost/database/g' rsc/database.properties			&& \
#    sed -i s/jdbc:mysql/${MYSQL_DB_PROTOCOL}/g rsc/database.properties		&& \
#    printf "\njdbc.class=${MYSQL_DB_CLASS}\n" >> rsc/database.properties && \
#    sed -i 's/USERSLISTLOGIN=no/USERSLISTLOGIN=yes/g' rsc/settings.properties 	&& \
#    sed -i 's/STRONGPASSWORD=yes/STRONGPASSWORD=no/g' rsc/settings.properties 	&& \
#    sed -i 's/STRONGLENGTH=10/STRONGLENGTH=3/g' rsc/settings.properties 	&& \
#    sed -i 's/DEBUG=no/DEBUG=yes/g' rsc/settings.properties 			&& \
#    sed -i 's/JWT_TOKEN_SECRET/eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzb21lIjoicGF5bG9hZCJ9Joh1R2dYzkRvDkqv3sygm5YyK8Gi4ShZqbhK2gxcs2U/g' rsc/application.properties
#RUN ./mvnw install -DskipTests
#
#WORKDIR /openhospital-api/target
#
#CMD java -cp "openhospital-api-0.0.2.jar:rsc/:static/" org.springframework.boot.loader.JarLauncher
