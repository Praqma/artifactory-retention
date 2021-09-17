FROM alpine:3.14.2

ARG JFROG_CLI_VERSION="2.3.0"

RUN apk add --no-cache curl

RUN curl -Lo /usr/bin/jfrog https://releases.jfrog.io/artifactory/jfrog-cli/v2/${JFROG_CLI_VERSION}/jfrog-cli-linux-amd64/jfrog && \
    chmod +x /usr/bin/jfrog

 CMD [ "jfrog", "--version" ]
