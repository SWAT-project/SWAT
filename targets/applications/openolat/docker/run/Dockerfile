FROM tomcat:10.1.40-jdk17

# ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.6.0/wait /wait
# RUN chmod a+x /wait

WORKDIR /usr/local/tomcat

RUN mkdir -p /swat/libs/java-library-path
COPY ./libs/z3/bin/libz3.so /swat/libs/java-library-path
COPY ./libs/z3/bin/com.microsoft.z3.jar /swat/libs/java-library-path
COPY ./libs/z3/bin/libz3java.so /swat/libs/java-library-path
COPY ./libs/z3/bin/z3 /swat/libs/java-library-path

RUN mkdir -p /swat/config
COPY ./swat.cfg /swat/config/swat.cfg

RUN mkdir -p /swat/executor

COPY ./build/openolat-lms-20.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
COPY ./env/tomcat/setenv.sh /usr/local/tomcat/bin/setenv.sh
COPY ./env/tomcat/server.xml /usr/local/tomcat/conf/server.xml
COPY ./env/tomcat/olat.local.properties /usr/local/tomcat/lib/olat.local.properties
COPY ./env/tomcat/log4j2.xml /usr/local/tomcat/lib/log4j2.xml
COPY ./env/tomcat/context.xml /usr/local/tomcat/conf/context.xml
COPY ./env/tomcat/ROOT.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml
COPY ./env/tomcat/logging.properties /usr/local/tomcat/conf/logging.properties

COPY ./env/tomcat/startup.sh /usr/local/tomcat/startup.sh

RUN chmod +x /usr/local/tomcat/startup.sh

CMD ["/usr/local/tomcat/startup.sh"]