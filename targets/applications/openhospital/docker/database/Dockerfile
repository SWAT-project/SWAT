FROM mariadb:10.2.40

ARG GITHUB_ORG=intesys
ARG OH_CORE_BRANCH=develop
ARG MYSQL_DUMP=./db/dump.sql
ENV MYSQL_DATABASE=oh
ENV MYSQL_ROOT_PASSWORD=root
ENV MYSQL_USER=isf
ENV MYSQL_PASSWORD=isf123

RUN apt-get update
RUN apt-get install git -y
RUN git clone --depth=1 -b ${OH_CORE_BRANCH} https://github.com/${GITHUB_ORG}/openhospital-core.git
WORKDIR /openhospital-core

EXPOSE 3306
 
COPY ${MYSQL_DUMP} /docker-entrypoint-initdb.d/

RUN mkdir data_en
RUN cp sql/data_en/* data_en/