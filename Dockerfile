FROM adoptopenjdk/openjdk11:x86_64-ubuntu-jdk-11.0.18_10

RUN apt-get update && \
    apt-get install -y sqlite3

COPY . /anteus
WORKDIR /anteus

EXPOSE 8080
# When the container starts: build, test and run the app.
CMD ./gradlew build && ./gradlew test && ./gradlew run
