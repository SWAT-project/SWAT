# Main Image
#FROM ubuntu:20.04
FROM git.its.uni-luebeck.de:5050/research-projects/pet-hmr/knife-fuzzer/target-base:1.0.0

# Symbolic
RUN mkdir /target
RUN mkdir /config
RUN mkdir /logs
RUN mkdir /symbolic
RUN mkdir /symbolic/tmp
RUN mkdir /symbolic/tmp/tmp

COPY cache/target.jar /target/target.jar
COPY cache/swat.cfg /config/swat.cfg
COPY cache/symbolic-executor.jar /symbolic/tmp/tmp/symbolic-executor.jar

RUN apt-get -y update && apt-get -y install curl && apt-get -y clean

ARG SERVER_PORT
EXPOSE ${SERVER_PORT}




WORKDIR /
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

CMD [ "/entrypoint.sh"]