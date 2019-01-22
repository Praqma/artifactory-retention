#! /bin/env groovy
import groovy.json.JsonSlurper

// ------------
// ARGUMENTS
// ------------

if (!args) {
  println "Usage:\n./${this.class.name}.groovy <repo> [username] [password]"
  System.exit(1)
}

args = args.toList() // allows for safe index access
String repo = args[0].trim()
String user = args[1] ? args[1].trim() : ''
String pass = args[2] ? args[2].trim() : ''

// ------------
// PROPERTIES
// ------------

String server = 'http://artifactory.praqma.net'
double percentageToDelete = 0.33

// ------------
// METHODS
// ------------

void runCommand (command) {
  println "[artifact-retention] ${command}"
  int exit = new ProcessBuilder(command.split(" ")).inheritIO().start().waitFor()
  println "[artifact-retention] exit: ${exit}"
  assert exit == 0
}

File getAqlQueryFile(String repo) {
  // Look for a repository-specific query
  File aqlFile = new File("artifact-aql/${repo}.json")

  if (!aqlFile.exists()) {
    // Look for a query generated from a template
    aqlFile = new File("artifact-aql/template-${repo}.json")
  }

  if (!aqlFile.exists()) {
    println "[artifact-retention] ERROR: Could not find a retention policy for ${repo}"
    println '[artifact-retention] ERROR: Make sure one is present under artifact-aql/'
    println "[artifact-retention] ERROR: If ${repo} is subscribed to a template policy,"
    println '[artifact-retention] ERROR: run ExpandTemplates.groovy first.'
    System.exit(1)
  }

  return aqlFile
}

// ------------
// SCRIPT
// ------------

JsonSlurper json = new JsonSlurper()
String curl = user && pass ? "curl -u ${user}:${pass}" : "curl -n"

// Get retention AQL query
File aqlFile = getAqlQueryFile(repo)

// Create result directory and file handles
File resultDir = new File('result')
resultDir.mkdirs()

// Fetch and parse artifacts from Artifactory
File rawJsonFile = new File(resultDir, "raw-${repo}.json")
runCommand("$curl -H content-type:text/plain --data-binary @artifact-aql/${aqlFile.name} ${server}/api/search/aql -o ${resultDir.name}/${rawJsonFile.name}")
def artifacts = json.parse(rawJsonFile)

// Delete artifacts
int amountToDelete = artifacts.results.size() * percentageToDelete
println "[artifact-retention] Deleting ${amountToDelete} out of ${artifacts.results.size()} artifacts."
artifacts.results.eachWithIndex { artifact, index ->
  String path = "${artifact.repo}/${artifact.path}/${artifact.name}"
  println "${index}: ${path}"
  if (index >= amountToDelete) { return }

  runCommand("$curl -XDELETE ${server}/${path}")
}
