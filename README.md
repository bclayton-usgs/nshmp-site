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
which can bu run with:
```bash
docker run -p PORT:8080 -d nshmp/nshmp-site-ws
```

`PORT` should be replaced with an available port that is not in use. The application 
can then be accessed from:
```
http://localhost:PORT/nshmp-site-ws/basin
```

The `PORT` should be replaced with the same value to start the container.


### Building
A Docker image may additionaly be built from the source using the accompaning Dockerfile:
```bash
docker build -t IMAGE_NAME:IAMGE_TAG .
```