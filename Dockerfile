
ARG builder_workdir=/app/nshmp-site-ws
ARG war_path=${builder_workdir}/build/libs/nshmp-site-ws.war

####################
# Builder Image
####################
FROM openjdk:8 as builder

# Get builder workdir
ARG builder_workdir

# nshmp-haz repo
ARG nshmp_haz=https://github.com/usgs/nshmp-haz.git

# nshmp-haz-ws repo
ARG nshmp_haz_ws=https://github.com/usgs/nshmp-haz-ws.git

# Set working directory
WORKDIR ${builder_workdir} 

# Copy project over to container
COPY . ${builder_workdir}/. 

# Update and install git
RUN apt-get update \
  && apt-get install -y git

# Get nshmp-haz and nshmp-haz-ws and build nshmp-site-ws
RUN git clone ${nshmp_haz} ../nshmp-haz \
  && git clone ${nshmp_haz_ws} ../nshmp-haz-ws

# Build nshmp-site-ws
RUN ./gradlew assemble 

####################
# Application Image 
####################
FROM tomcat:latest

# Get WAR path
ARG war_path

# Copy WAR file from builder image
COPY --from=builder ${war_path} ${CATALINA_HOME}/webapps/.

# Run tomcat
CMD /usr/local/tomcat/bin/catalina.sh run