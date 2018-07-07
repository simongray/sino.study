FROM openjdk:10
ARG JARPATH
ARG JARFILE
ENV JARFILE "$JARFILE"
MAINTAINER Simon Gray <simongray@gmail.com>
ADD "$JARPATH" /usr/src/myapp/
WORKDIR /usr/src/myapp
EXPOSE 8080
CMD java -XX:+PrintFlagsFinal -jar "$JARFILE"
