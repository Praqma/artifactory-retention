| maintainer |
| ---------- |
| praqma-thi |

# Artifactory retention

Repository used to manage Artifactory retention policies for artifacts, builds and releases.

*Note*: Release Bundles will only be removed from JFrog Distribution, not from the Artifactory Edge Nodes.

## Adopting this for own usage

* Change the artifactory server URL in `scripts/*.groovy`
* Set up any desired retention policies using the guide below
* Run the retention using the guide below

## Setting up a retention policy

You can either subscribe your repository to a premade retention policy or create a custom policy.

### Subscribing your repository to a premade retention policy

Add an entry for your repository in the `config/template-subscription.json` file under the template you wish to subscribe to.

#### Available templates

##### Unused

-`repo-name`: The target repository
-`ttl-without-dl`: Time to keep artifacts with zero total downloads

### Setting up a custom retention policy

If none of the premade policies suit you, add a custom AQL file under the `retention-specs/artifact/` directory named `<repository-name>.json`.
This should contain an [AQL query](https://www.jfrog.com/confluence/display/RTF/Artifactory+Query+Language) that fetches the items you want to clean up.

## Creating a new retention policy template

New templates can be defined under the `retention-specs/templates` dir.
The `expandTemplates.sh` script creates a copy of the template under `retention-specs/artifact/` for each of its subscribers defined in the `config/template-subscription.json` file. Any string in the template that matches a key passed in by a subscriber will be replaced by the subscriber's value.

## Setting up build and release retention

Build and release retention works identical to artifact retention, except that you add AQL scripts to the `retention-specs/build/` and `retention-specs/release/` dir respectively. Templates are not supported.

## Running retention

To run artifact retention, you can run the `runRetention.sh` script present in the repository:
`./runRetention.groovy artifacts <repo-name> [user] [password]`

*Note*: If you subscribed to a retention policy, you must run `./expandTemplates.sh` first.

To run build retention, you can run the `runRetention.sh` script present in the repository:
`./runRetention.groovy builds <retention-name> [user] [password]`

To run release retention, you can run the `runRetention.sh` script present in the repository:
`./runRetention.groovy builds <retention-name> [user] [password]`


## Automating the retention

The repository contains a Jenkinsfile for automating retention runs, you'll find it under the `jenkins` directory. You should be able to adopt them with minor changes.

## Validation

There are two Gradle tasks for validating the retention specs and template subscription.
The former just checks if the Json is parseable, the latter uses schema matching.

- `./gradlew validateRetentionSpecs`
- `./gradlew validateSubscriptions`
