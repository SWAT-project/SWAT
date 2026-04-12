#!/bin/bash -eux

DEBUG=${DEBUG:-true}

if [ $DEBUG ]; then
    export JPDA_ADDRESS="*:5005"
    export JPDA_TRANSPORT=dt_socket
    export JPDA_SUSPEND=n
fi

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/swat/libs/java-library-path
export PATH=$PATH:/swat/libs/java-library-path
env
# /wait && /usr/local/tomcat/bin/catalina.sh jpda run &
/usr/local/tomcat/bin/catalina.sh jpda run

# wait ${!}
