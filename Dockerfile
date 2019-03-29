###########################
# Dockerfile for nshmp-site-ws
###########################

# Builder image working directory
ARG builder_workdir=/app/nshmp-site-ws

# Path to WAR file in builder image
ARG war_path=${builder_workdir}/build/libs/nshmp-site-ws.war

####
# Builder Image: Java 8
#   - Install git
#   - Download nshmp-haz and nshmp-haz-ws
#   - Build nshmp-site-ws
####
FROM openjdk:8-alpine as builder

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
RUN apk add --no-cache git curl bash

# Download all required repositories. See docker.sh
RUN cd .. && bash ${builder_workdir}/docker.sh && pwd && ls

# Build nshmp-site-ws
RUN ./gradlew assemble

####
# Application Image: Tomcat
#   - Copy WAR file from builder image
#   - Run Tomcat
####
FROM tomcat:8-alpine

# Set author
LABEL maintainer="Brandon Clayton <bclayton@usgs.gov>"

# Get WAR path
ARG war_path

# Copy WAR file from builder image
COPY --from=builder ${war_path} ${CATALINA_HOME}/webapps/.

# Run tomcat
ENTRYPOINT ${CATALINA_HOME}/bin/catalina.sh run
