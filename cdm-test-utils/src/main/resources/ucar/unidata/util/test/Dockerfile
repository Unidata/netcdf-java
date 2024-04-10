FROM unidata/tomcat-docker:8.5-jdk11 AS base

MAINTAINER Unidata

USER root

# TODO use release version of d4ts
#ENV D4TS_WAR_URL https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/d4ts/5.4/d4ts-5.4.war
ENV D4TS_WAR_URL https://artifacts.unidata.ucar.edu/repository/unidata-snapshots/edu/ucar/d4ts/5.5-SNAPSHOT/d4ts-5.5-20240410.110406-179.war
ENV DTS_WAR_URL https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/dtswar/5.4/dtswar-5.4.war

# Install necessary packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl unzip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN curl -fSL "${D4TS_WAR_URL}" -o d4ts.war
RUN curl -fSL "${DTS_WAR_URL}" -o dts.war

RUN unzip d4ts.war -d ${CATALINA_HOME}/webapps/d4ts/ && \
    rm -f d4ts.war && \
    unzip dts.war -d ${CATALINA_HOME}/webapps/dts/ && \
    rm -f dts.war && \
    chmod 755 ${CATALINA_HOME}/bin/*.sh && \
    mkdir -p ${CATALINA_HOME}/javaUtilPrefs/.systemPrefs

EXPOSE 8080 8443

WORKDIR ${CATALINA_HOME}

# Inherited from parent container
ENTRYPOINT ["/entrypoint.sh"]

# Start container
CMD ["catalina.sh", "run"]

HEALTHCHECK --interval=10s --timeout=3s \
    CMD (curl --fail 'http://localhost:8080/d4ts/' && curl --fail 'http://localhost:8080/dts/')) || exit 1
