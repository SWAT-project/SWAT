#SERVER_PORT=$(grep "server.port" ../src/main/resources/application.properties | cut -d'=' -f2)
#echo "SERVER_PORT=$SERVER_PORT"
SERVER_PORT=8091
#docker run --name restler-target-container -p $SERVER_PORT:$SERVER_PORT its/restler-target:latest
docker run -p $SERVER_PORT:$SERVER_PORT its/restler-target:latest
