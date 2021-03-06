FROM fabric8/java-jboss-openjdk8-jdk:1.4.0
MAINTAINER Nils Hoffmann &lt;nils.hoffmann@isas.de&gt;
VOLUME /tmp
# receive jar file to host via argument
ARG JAR_FILE
ARG APP_NAME

ENV AB_OFF true
EXPOSE 8083
ENV JAVA_OPTIONS="-Djava.security.egd=file:/dev/./urandom"
ENV JAVA_APP_JAR=${JAR_FILE}
ENV JAVA_APP_NAME=${APP_NAME}
#this is Maven's target dir
ADD target/${JAR_FILE} /deployments/
