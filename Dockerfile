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

# Get builder workdir
ARG builder_workdir

# nshmp-haz version
ARG nshmp_haz_version=master

# nshmp-haz-ws version
ARG nshmp_haz_ws_version=master

# nshmp-haz repo
ARG nshmp_haz=https://github.com/usgs/nshmp-haz/archive/${nshmp_haz_version}.tar.gz

# nshmp-haz-ws repo
ARG nshmp_haz_ws=https://github.com/usgs/nshmp-haz-ws/archive/${nshmp_haz_ws_version}.tar.gz

# Set working directory
WORKDIR ${builder_workdir} 

# Copy project over to container
COPY . ${builder_workdir}/. 

# Install curl and git
RUN apk add --no-cache git curl

# Download nshmp-haz and nshmp-haz-ws
RUN curl -L ${nshmp_haz} | tar -xz \
  && curl -L ${nshmp_haz_ws} | tar -xz \
  && mv nshmp-haz-${nshmp_haz_version} ../nshmp-haz \
  && mv nshmp-haz-ws-${nshmp_haz_ws_version} ../nshmp-haz-ws

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
