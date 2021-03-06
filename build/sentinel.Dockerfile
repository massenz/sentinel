# Created MM, 2015-06-07
#
# Builds a docker container running Play framework
# and the Sentinel application layer

# To use this Dockerfile, you must first run ``sbt docker:stage``
# then copy this file to target/docker/Dockerfile, overwriting the one
# created by sbt.
#
# Build the docker image: ``docker build -t massenz/sentinel .``
# then run the container: ``docker run -p 80:9000 -d massenz/sentinel``

# TODO(marco): automate the process


FROM java:8
MAINTAINER Marco Massenzio <marco@alertavert.com>

# Just a convenience for when entering the shell to debug
# the container
RUN alias ll="ls -l"
RUN alias la="ls -lAh"
RUN apt-get update && apt-get install -y vim

# TODO: pick the VERSION ENV var from the build
ENV VERSION="0.3" SENTINEL_BASE="/opt/sentinel" SENTINEL_CONF="/etc/sentinel"

# `sbt docker:stage` places everything inside a files/opt/docker folder.
ADD files/opt/docker /opt/sentinel

# TODO: we need to add /etc/sentinel to the CLASSPATH so
# application.conf and override.conf can be picked up post-build.
COPY files/opt/docker/conf /etc/sentinel

WORKDIR /opt/sentinel

RUN ["chown", "-R", "daemon", "/etc/sentinel"]
RUN ["chown", "-R", "daemon", "."]
USER daemon

# `/opt/docker/bin/sentinel` is generated by the sbt docker plugin and launches the Play application.
# It takes a number of options, and also expands a `/etc/sentinel/default` file.
# It is all very convoluted and poorly documented, as far as I can tell.
#
# TODO: wrap it with a more sensible way of dealing with classpath, etc.
CMD ["bin/sentinel"]
EXPOSE 9000
