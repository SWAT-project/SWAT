
# cd into the directory of the script
cd "$(dirname "$0")"

./gradlew clean build shadowJar
