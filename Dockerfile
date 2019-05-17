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


####
# Application Image: usgsnshmp/nshmp-tomcat:8.5-jre8
#   - Download nshmp-haz and nshmp-haz-ws
#   - Build nshmp-site-ws
#   - Deploy nshmp-site-ws
####
FROM usgsnshmp/nshmp-tomcat:8.5-jre8

# Set author
LABEL maintainer="Brandon Clayton <bclayton@usgs.gov>"

# Repository versions
ENV NSHMP_HAZ_VERSION=master
ENV NSHMP_HAZ_WS_VERSION=master

# Current project
ENV PROJECT=nshmp-site-ws

# Set home
ENV HOME=/app

# Working directory
ENV WORKDIR=${HOME}/${PROJECT}

# nshmp-site-ws WAR path
ENV WAR_PATH=${WORKDIR}/build/libs/${PROJECT}.war

# Set working directory
WORKDIR ${WORKDIR}

# Copy project to container
COPY . ${WORKDIR}/.

# Install git
RUN yum install -y git

# Build nshmp-site-ws
ENTRYPOINT [ "bash", "docker-entrypoint.sh" ]
