FROM openjdk:10
MAINTAINER Simon Gray <simongray@gmail.com>
ADD target/sinostudy-0.1.0-SNAPSHOT-standalone.jar /usr/src/myapp/
WORKDIR /usr/src/myapp
EXPOSE 8080
CMD java -XX:+PrintFlagsFinal -jar sinostudy-0.1.0-SNAPSHOT-standalone.jar
