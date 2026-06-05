FROM tomcat:8.5.50-jdk8-openjdk

ARG WAR_FILE=target/tasks.war
ARG CONTEXT=tasks

COPY ${WAR_FILE} /usr/local/tomcat/webapps/${CONTEXT}.war
