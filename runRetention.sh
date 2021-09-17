#!/bin/bash
set -e

echo "INFO: Cleaning artifacts..."
for artifact_spec in specs/*.json; do
    jfrog rt delete --spec="$artifact_spec" $@
done
echo "INFO: Done!"
