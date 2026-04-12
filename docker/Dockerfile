FROM gradle:8.13.0-jdk17

# Install Python and other tools
RUN apt-get update && \
    apt-get install -y python3 python3-pip python3-venv findutils unzip ant

# Set working dir
WORKDIR /app

# Copy project source
COPY . .

RUN chmod +x ./scripts/build-javasmt.sh
RUN ./scripts/build-javasmt.sh

# Copy native Z3 libs via Gradle task (assumes they go to /app/libs/java-library-path)
RUN gradle copyNativeLibs

# Build Java code (skip tests)
RUN gradle clean build -x test


# System-wide: Move native Z3 libs to where JVM can find them
RUN cp /app/libs/java-library-path/libz3*.so /usr/lib/

# Set Z3 JAR in CLASSPATH so it's visible to javaagent (NO JVM FLAGS NEEDED)
ENV CLASSPATH=/app/libs/java-library-path/com.microsoft.z3.jar