
# change path to root of project
cd "$(dirname "$0")"
cd ../../..

# remove old state
rm -rf logs && mkdir logs
cd logs

java \
-Xmx16g \
-Djanala.conf=../targets/basic-tests/exectree-example/catg.conf \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/basic-tests/exectree-example/build/libs/exectree-example.jar

# Idea for debugging: -verbose