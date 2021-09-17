# Artifactory Retention

Repository used to manage Artifactory retention policies.

## Usage

Place [file specs](https://www.jfrog.com/confluence/display/JFROG/Using+File+Specs) matching the _artifacts you wish to delete_ in `specs/` and run `runRetention.sh`.

This project relies on the [JFrog CLI](https://jfrog.com/getcli/).
You can have it installed locally or use the provided Dockerfile.

## Notes

- JFrog CLI parameters passed to `runRetention.sh` are forwarded to the JFrog CLI.
- Jenkinsfile for automating retention runs provided.

## On builds and releases

For the sake of simplicity, I don't included them in this project.
The JFrog CLI can't delete builds/releases through specs (yet?).

You _can_ search for them through [AQL](https://www.jfrog.com/confluence/display/JFROG/Artifactory+Query+Language), iterate over the results and `curl -X DELETE` their respective [API endpoints](https://www.jfrog.com/confluence/display/JFROG/Artifactory+REST+API)
