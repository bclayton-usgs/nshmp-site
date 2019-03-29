#!/bin/bash

####
# Download a USGS repository from Github.
# Arguments:
#   (string) repo - The project to download
#   (string) version - The version to download
# Returns:
#   None
####
download_repo() {
  local repo=${1}
  local version=${2}
  local url="https://github.com/usgs/${repo}/archive/${version}.tar.gz"

  printf "\n Downloading [${url}] \n"
  curl -L ${url} | tar -xz
  mv ${repo}-${version#v*} ${repo}
}

# Download nshmp-haz
download_repo "nshmp-haz" ${NSHMP_HAZ_VERSION}

# Download nshmp-haz-ws
download_repo "nshmp-haz-ws" ${NSHMP_HAZ_WS_VERSION}
