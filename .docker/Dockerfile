FROM golang:1.17.0-alpine3.14 AS build

ARG JFROG_CLI_VERSION="1.39.0"

RUN apk add --no-cache --update --quiet \
      ca-certificates \
      git

RUN mkdir -p /go/src/github.com/jfrog && \
    cd /go/src/github.com/jfrog && \
    git clone https://github.com/jfrog/jfrog-cli && \
    cd /go/src/github.com/jfrog/jfrog-cli && \
    git checkout v${JFROG_CLI_VERSION} && \
    sh build/build.sh jfrog-cli

# Base on alpine once (if) we get rid of the Groovy scripts
FROM groovy:3.0.9-jdk8

USER root

COPY --from=build /go/src/github.com/jfrog/jfrog-cli/jfrog-cli \
                  /usr/bin/jfrog-cli

RUN ln -s /usr/bin/jfrog-cli /usr/bin/jfrog

CMD [ "jfrog-cli", "--version" ]
