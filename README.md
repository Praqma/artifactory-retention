# Artifactory retention

This repository shows how you can easily manage artifact and build retention in Artifactory.
For more information, check out [the blog](https://www.praqma.com/stories/artifactory-retention-policies/) for details.

It'll require a few tweaks to get it up and running for yourself, notably:

* Changing the retention scripts to point to your Artifactory server (files: `ArtifactRetention.groovy`, `BuildRetention.groovy`)
* Changing the Jenkinsfile to use credentials that exist on your master (file: `Jenkinsfile`)
* Editing the AQL queries and templates to match your needs (directories: `artifact-aql`, `templates`)
* Editing the configured repositories to match your setup (file: `template-subscription.json`)

## Table of contents

* [Artifact Retention](#artifact-retention)
  * [AQL Query Templates](#aql-query-templates)
  * [Custom AQL Queries](#custom-aql-queries)
* [Build Retention](#build-retention)
* [Running The Tetention](#running-the-retention)
* [Cleaning Up Binaries](#cleaning-up-binaries)

## Artifact Retention

Artifacts matching a specific AQL query are deleted using the Artifactory REST API.
To assign an AQL query to a repository, either:

* Subscribe to a template AQL query, or
* Write a custom AQL query for the repository

Both ways are elaborated below.

### AQL Query Templates

AQL query templates are stored in the `templates` directory.

Repositories can subscribe to them in the `template-subscription.json` file.

The `ExpandTemplates.groovy` script creates an AQL query based on a template for each repository subscribed to the template.
It replaces value placeholders with the values set in the `template-subscription.json` file and writes the result to the `artifact-aql` directory.
These are later used for running the actual cleanup.

#### Template example

Below, a repository is subscribed to the `unused` template, configuring the `repository-name` and `ttl-without-dl` values.

`template-subscription.json`:

```json
{
    "templates": {
        "unused": [
            { "repository-name": "bar-lib-local", "ttl-without-dl": "3mo" }
        ]
    }
}
```

The `unused` template deletes artifacts that have never been downloaded if they are older than a configurable age.
Note that the `repository-name` and `ttl-without-dl` values will be replaced when the actual AQL query files are generated.

`unused.json`:

```json
items.find({
    "repo": { "$eq": "repository-name" },
    "$or" : [
        {
            "$and": [
                { "stat.downloads": { "$eq":null } },
                { "updated": { "$before": "ttl-without-dl" } }
            ]
        }
    ]
}).include("repo", "name", "path", "updated", "sha256", "stat.downloads", "stat.downloaded")
```

Running the `ExpandTemplates.groovy` script will generate the following file using the template and the values in `template-subscription.json`.

`artifact-aql/template-bar-lib-local`:

```json
items.find({
    "repo": { "$eq": "bar-lib-local" },
    "$or" : [
        {
            "$and": [
                { "stat.downloads": { "$eq":null } },
                { "updated": { "$before": "3mo" } }
            ]
        }
    ]
}).include("repo", "name", "path", "updated", "sha256", "stat.downloads", "stat.downloaded")
```

The generated AQL query can then be used by `ArtifactRetention.groovy`.

### Custom AQL Queries

If you have a niche use case where using or making a template doesn't make sense, you can always create a custom AQL query for a specific repository.
Add the AQL query to the `artifact-aql` directory as `<repository-name>.json`.

#### Custom Query Example

The `foo-logs-local.aql` query deletes all files whose names don't match `*.logs`.
It's added to the `artifact-aql` directory.

`foo-logs-local.json`:

```json
items.find({
    "repo": { "$eq": "foo-logs-local" },
    "$and": [
        {"name": {"$nmatch": "*.log"}}
    ]
}).include("repo", "name", "path", "updated", "sha256", "stat.downloads", "stat.downloaded")
```

The generated AQL query can then be used by `ArtifactRetention.groovy`.

### Build Retention

Build retention works similarly to artifact retention, but it's not as fleshed out.
There's a single AQL query, `builds-without-artifacts.json`, that matches all builds that produced no artifacts or had all their artifacts removed by the artifact retention.

The `BuildRetention.groovy` script runs that AQL and deletes matching builds using the Artifactory REST API.

### Running The Retention

Running the retention consists of a few steps:

* Run `ExpandTemplates.groovy` to generate AQL queries for repositories using template subscriptions
* Run `ArtifactRetention.groovy` for each AQL query in the `artifact-aql` directory
* Run `BuildRetention.groovy` to clean up builds that had all their artifacts removed

To make things easy, there's a `Jenkinsfile` included that automates this, running retention for all repositories (in parallel and in chunks of 10).
Just make a Jenkins job that runs the retention as a cron job.

### Cleaning Up Binaries

Deleting artifacts through the REST API does just that.
It deletes the artifacts, references to binaries, but not the binaries themselves.
To delete the binaries and clear up disk space, head over to the **Artifactory Admin Panel -> Advanced -> Maintenance** and hit the **Prune Unreferenced Data** button.
There doesn't seem to be a REST API call to run this yet, so for now it's a manual task.
