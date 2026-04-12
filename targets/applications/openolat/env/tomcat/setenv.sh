#CATALINA_HOME=/usr/tomcat
#JRE_HOME=/usr/lib/jvm/default-jvm/jre
#CATALINA_PID=/run/openolat.pid
#CATALINA_TMPDIR=/tmp/openolat
#mkdir -p $CATALINA_TMPDIR

export JAVA_OPTS="--add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
                  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
                  --add-opens java.base/jdk.internal.vm.annotation=ALL-UNNAMED"

# -XX:MaxGCPauseMillis=750 -XX:MaxHeapSize=256g -XX:ConcGCThreads=64 -XX:InitialHeapSize=64g -XX:+DisableExplicitGC -XX:+ParallelRefProcEnabled \
export CATALINA_OPTS="-Duser.name=openolat                                   \
-Duser.timezone=Europe/Berlin                          \
-Dspring.profiles.active=myprofile                     \
-Djava.awt.headless=true                               \
-Djava.net.preferIPv4Stack=true                        \
-XX:+HeapDumpOnOutOfMemoryError                        \
-XX:HeapDumpPath=.                                     \
-javaagent:/swat/executor/symbolic-executor-all.jar \
-Dconfig.path=/swat/config/swat.cfg" # \
# -javaagent:/swat/executor/minimal-java-agent-1.0-SNAPSHOT.jar \
