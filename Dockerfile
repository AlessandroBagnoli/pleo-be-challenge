FROM adoptopenjdk/openjdk11:x86_64-ubuntu-jdk-11.0.18_10

COPY . /anteus
WORKDIR /anteus

EXPOSE 8080
# When the container starts: build, executes unit tests and run the app.
CMD ./gradlew build -x functional && ./gradlew test && ./gradlew run -x functional
