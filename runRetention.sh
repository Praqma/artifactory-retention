#!/usr/bin/env sh
if [ $# -lt 2 ]
  then
    echo "Usage: runRetention.sh <builds|artifacts> <repo-name|build-retention-name> [user] [password]"
    echo "Ex.: runRetention.sh artifacts generic-local emily sEcrEt123"
    echo "Ex.: runRetention.sh builds builds-without-artifacts robert P4sSw0Rd"
    exit 1
fi

if [ "$1" != "builds" ] && [ "$1" != "artifacts" ]
  then
    echo "first arg must be 'builds' or 'artifacts' (got '$1')"
    exit 1
fi

if [ "$1" = "builds" ]
  then
    groovy scripts/BuildRetention.groovy retention-specs/build/$2.aql result/ $3 "$4"
fi

if [ "$1" = "artifacts" ]
  then
    groovy scripts/ArtifactRetention.groovy $2 retention-specs/artifact result/ $3 "$4"
fi

if [ "$1" = "releases" ]
  then
    groovy scripts/ReleaseRetention.groovy retention-specs/release/$2.aql result/ $3 "$4"
fi
