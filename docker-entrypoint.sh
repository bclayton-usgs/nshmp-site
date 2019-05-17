#!/bin/bash
#
# Docker entrypoint for nshmp-site-ws:
#   - Download nshmp-haz and nshmp-haz-ws
#   - Build nshmp-site-ws
#   - Deploy nshmp-site-ws
####

set -o errexit;
set -o errtrace;

readonly LOG_FILE="docker-entrypoint.log";

####
# Build and deploy nshmp-site-ws.
# Globals:
#   (string) LOG_FILE - The log file
#   (string) TOMCAT_WEBAPPS - Path to Tomcat webapps directory
#   (string) WAR_PATH - Path to nshmp-haz-ws.war
# Arguments:
#   None
# Returns:
#   None
####
main() {
  # Set trap for uncaught errors
  trap 'error_exit "${BASH_COMMAND}" "$(< ${LOG_FILE})"' ERR;

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
# Download a USGS repository from Github.
# Arguments:
#   (string) repo - The project to download
#   (string) version - The version to download
# Returns:
#   None
####
download_repo() {
  local repo=${1};
  local version=${2};
  local url="https://github.com/usgs/${repo}/archive/${version}.tar.gz";

  printf "\n Downloading [${url}] \n";
  curl -L ${url} | tar -xz 2> ${LOG_FILE} || \
      error_exit "Could not download [${url}]" "$(< ${LOG_FILE})";
  mv ${repo}-${version#v*} ${repo};
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
  download_repo "nshmp-haz" ${NSHMP_HAZ_VERSION};

  # Download nshmp-haz-ws
  download_repo "nshmp-haz-ws" ${NSHMP_HAZ_WS_VERSION};

  cd ${WORKDIR} 2> ${LOG_FILE};
}

####
# Exit script with error.
# Globals:
#   None
# Arguments:
#   (string) message - The error message
#   (string) logs - The log for the error
# Returns:
#   None
####
error_exit() {
  local usage="
    docker run -p <PORT>:8080 -d usgs/nshmp-site-ws
  ";

  local message="
    nshmp-haz Docker error:
    ${1}

    ----------
    Logs:

    ${2}

    ----------
    Usage:

    ${usage}

  ";

  printf "${message}";

  exit -1;
}

####
# Run main
####
main "$@";
