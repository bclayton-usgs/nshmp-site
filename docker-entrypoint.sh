#!/bin/bash
#
# Docker entrypoint for nshmp-site-ws:
#   - Download nshmp-haz and nshmp-haz-ws
#   - Build nshmp-site-ws
#   - Deploy nshmp-site-ws
####

set -o errexit;
set -o errtrace;

env

# Import bash functions
. ${BASH_FUNCTIONS}; 

# Log file
readonly LOG_FILE="docker-entrypoint.log";

# Docker usage
readonly USAGE="
  docker run -p <PORT>:8080 -d usgs/nshmp-site-ws
";


####
# Build and deploy nshmp-site-ws.
# Globals:
#   (string) LOG_FILE - The log file
#   (string) TOMCAT_WEBAPPS - Path to Tomcat webapps directory
#   (string) WAR_PATH - Path to nshmp-haz-ws.war
#   (string) USAGE - The Docker usage
# Arguments:
#   None
# Returns:
#   None
####
main() {
  # Set trap for uncaught errors
  trap 'error_exit "${BASH_COMMAND}" "$(< ${LOG_FILE})" "${USAGE}"' ERR;

  # Download repositories
  download_repos;

  # Build nshmp-site-ws
  ./gradlew assemble 2> ${LOG_FILE};

  # Move war file
  mv ${WAR_PATH} ${TOMCAT_WEBAPPS} 2> ${LOG_FILE};

  # Run tomcat
  catalina.sh run;
}

####
# Download nshmp-haz and nshmp-haz-ws.
# Globals:
#   (string) HOME - app home 
#   (string) LOG_FILE - The log file
#   (string) NSHMP_HAZ_VERSION - nshmp-haz repository version
#   (string) NSHMP_HAZ_WS_VERSION - nshmp-haz-ws repository version
#   (string) WORKDIR - The Docker working directory
# Arguments:
#   None
# Returns:
#   None
####
download_repos() {
  cd ${HOME} 2> ${LOG_FILE};

  # Download nshmp-haz
  download_repo "usgs" "nshmp-haz" ${NSHMP_HAZ_VERSION};

  # Download nshmp-haz-ws
  download_repo "usgs" "nshmp-haz-ws" ${NSHMP_HAZ_WS_VERSION};

  cd ${WORKDIR} 2> ${LOG_FILE};
}

####
# Run main
####
main "$@";
