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
# Application Image: usgsnshmp/nshmp-tomcat:8.5-jre8
#   - Copy WAR file from builder image
####
FROM usgsnshmp/nshmp-tomcat:8.5-jre8

# Set author
LABEL maintainer="Brandon Clayton <bclayton@usgs.gov>"

# Get WAR path
ARG war_path

# Copy WAR file from builder image
COPY --from=builder ${war_path} ${TOMCAT_WEBAPPS}/ 
