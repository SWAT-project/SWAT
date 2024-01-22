
# change path to root of project
cd "$(dirname "$0")"
cd ../../..

# remove old state
rm -rf catg_tmp && mkdir catg_tmp
cd catg_tmp

/usr/local/openjdk-16/bin/java \
-Djanala.conf=../targets/basic-tests/type-change1/catg.conf \
-XX:-Inline \
-Xbootclasspath/a:\
../symbolic-executor/lib/symbolic-executor.jar \
-cp ../symbolic-executor/lib/symbolic-executor.jar \
-javaagent:../symbolic-executor/lib/symbolic-executor.jar \
-jar ../targets/basic-tests/type-change1/build/libs/type-change1.jar

# Idea for debugging: -verbose