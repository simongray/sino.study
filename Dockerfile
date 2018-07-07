FROM openjdk:10
ARG jarpath
ARG jarfile
MAINTAINER Simon Gray <simongray@gmail.com>
ADD $jarpath /usr/src/myapp/
WORKDIR /usr/src/myapp
EXPOSE 8080
CMD java -XX:+PrintFlagsFinal -jar $jarfile
