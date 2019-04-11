####
# Dockerfile for nshmp-site-ws.
#
# Deploys nshmp-site-ws inside an usgs/centos image.
#
# Usage:
#   docker run -d -p <PORT>:8080 usgsnshmp/nshmp-site-ws
#
#   Visit: http://localhost:<PORT>/nshmp-site-ws/basin 
#
#  Example:
#   docker run -d -p 8080:8080 usgsnshmp/nshmp-site-ws
#   
#   Visit: http://localhost:8080/nshmp-site-ws/basin 
####

# Builder image working directory
ARG builder_workdir=/app/nshmp-site-ws

# Path to WAR file in builder image
ARG war_path=${builder_workdir}/build/libs/nshmp-site-ws.war

####
# Builder Image: Java 8
#   - Install git, curl, and bash
#   - Download nshmp-haz and nshmp-haz-ws with docker-builder-entrypoint.sh
#   - Build nshmp-site-ws
####
FROM openjdk:8 as builder

# Get builder working directory
ARG builder_workdir

# Repository versions
ARG NSHMP_HAZ_VERSION=master
ARG NSHMP_HAZ_WS_VERSION=master

# Set working directory
WORKDIR ${builder_workdir} 

# Copy project over to container
COPY . ${builder_workdir}/. 

# Install curl, git, and bash
RUN apt-get install -y git curl bash 

# Download all required repositories. See docker.sh
RUN cd .. && bash ${builder_workdir}/docker-builder-entrypoint.sh

# Build nshmp-site-ws
RUN ./gradlew assemble

####
# Application Image: usgs/centos 
#   - Intall Java 8 and Tomcat
#   - Copy WAR file from builder image
#   - Run Tomcat
####
FROM usgs/centos

# Set author
LABEL maintainer="Brandon Clayton <bclayton@usgs.gov>"

# Tomcat home
ENV CATALINA_HOME "/usr/local/tomcat"

# Tomcat version to download
ARG tomcat_major=8
ARG tomcat_version=8.5.39
ARG tomcat_source=http://archive.apache.org/dist/tomcat

# Install Java 8 and Tomcat 8
RUN yum install -y java-1.8.0-openjdk-devel \
  && curl -L ${tomcat_source}/tomcat-${tomcat_major}/v${tomcat_version}/bin/apache-tomcat-${tomcat_version}.tar.gz | tar -xz \
  && mv apache-tomcat-${tomcat_version} ${CATALINA_HOME}

# Get WAR path
ARG war_path

# Copy WAR file from builder image
COPY --from=builder ${war_path} ${CATALINA_HOME}/webapps/.

# Expose port
EXPOSE 8080

# Run tomcat
ENTRYPOINT ${CATALINA_HOME}/bin/catalina.sh run
