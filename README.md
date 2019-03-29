# nshmp-site-ws

## Usage
* /nshmp-site-ws/basin

## Obtaining the basins.geojson feature colleciton
* /nshmp-site-ws/basin/geojson

## Obtaining basin terms
* /nshmp-site-ws/basin?latitude={latitude}&longitude={longitude}&model={basinModel}
* Example: /nshmp-site-ws/basin?latitude=47.2&longitude=-122.5&model=Seattle
* Example: /nshmp-site-ws/basin?latitude=47.2&longitude=-122.5

NOTE: When "model" is not supplied in the query string, the default model is used that is defined in the usage.

## Docker
The nshmp-site-ws application may be run as a Docker container.
A public image is available on Docker Hub at
[https://hub.docker.com/r/nshmp/nshmp-site-ws](https://hub.docker.com/r/nshmp/nshmp-site-ws)
which can be run with:

```bash
docker run -p PORT:8080 -d nshmp/nshmp-site-ws

# Example
docker run -p 8080:8080 -d nshmp/nshmp-site-ws
```

`PORT` should be replaced with an available port that is not in use. The application 
can then be accessed from:

```bash
http://localhost:PORT/nshmp-site-ws/basin

# Example
http://localhost:8080/nshmp-site-ws/basin
```

The `PORT` should be replaced with the same value to start the container.


### Building
A Docker image may additionaly be built from the source using the accompanying Dockerfile:
```bash
docker build -t IMAGE_NAME:IAMGE_TAG .

# Example
docker build -t nshmp-site-ws:latest . 
```

#### Customization
When building the Docker image the version, branch, or commit may be supplied as arguments
to specify nshmp-haz and nshmp-haz-ws versions.

```bash
docker run -p PORT:8080 -d \
  nshmp_haz_version=some-version-or-branch-or-commit \
  nshmp_haz_ws_version=some-version-or-branch-or-commit \
  nshmp/nshmp-site-ws

# Example
docker run -p 8080:8080 -d \
  nshmp_haz_version=v1.1.4 \
  nshmp_haz_ws_version=v1.1.2 \
  nshmp/nshmp-site-ws
```

## Development
Docker Compose can be used for development:

```bash
# Run docker-compose.yaml file
docker-compose up -d

# Look for changes and rebuild WAR file
./gradlew assemble --continuous
```

`./gradlew assemble --continuous` looks for any changes in the source
code and rebuilds the WAR file. Docker Compose looks for any changes to the
WAR file and redeploys.