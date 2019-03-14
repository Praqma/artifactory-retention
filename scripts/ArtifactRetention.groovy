#! /bin/env groovy
import groovy.json.JsonSlurper

// ------------
// PROPERTIES
// ------------

String server = 'http://artifactory.praqma.net'

// ------------
// ARGUMENTS
// ------------

if (!args || args.size() < 2) {
  println "Usage:\n./${this.class.name}.groovy <repo-name> <aql-dir> <output-dir> [username] [password]"
  System.exit(1)
}

args = args.toList() // allows for safe index access
String argRepo = args[0].trim()
String argAqlDir = args[1].trim()
String argOutputDir = args[2].trim()
String argUser = args[3] ? args[3].trim() : ''
String argPass = args[4] ? args[4].trim() : ''

File userDir = new File(System.getProperty('user.dir'))
File aqlDir = new File(userDir, argAqlDir)
File outputDir = new File(userDir, argOutputDir)

// ------------
// METHODS
// ------------

int runCommand (cmd) {
  println cmd
  int exit = new ProcessBuilder(cmd.split(' ')).inheritIO().start().waitFor()
  println "[artifact-retention] exit: ${exit}"
  assert exit == 0
  return exit
}

// ------------
// SCRIPT
// ------------

JsonSlurper json = new JsonSlurper()
String curl = argUser && argPass ? "curl -u ${argUser}:${argPass} --noproxy '*'" : 'curl -n'

// Get retention AQL query
File aqlFile = new File(aqlDir, "${argRepo}.aql")
if (!aqlFile.exists()) {
  // See if it was expanded from a template
  aqlFile = new File(aqlDir, "template-${argRepo}.aql")
  if (!aqlFile.exists()) {
    println "[artifact-retention] ERROR: Could not find a retention policy for ${argRepo}"
    println "[artifact-retention] ERROR: Make sure one is present in ${aqlDir}"
    println "[artifact-retention] ERROR: If ${argRepo} is subscribed to a template policy,"
    println '[artifact-retention] ERROR: run ExpandTemplates.groovy first.'
    System.exit(1)
  }
}


// Fetch and parse artifacts from Artifactory
outputDir.mkdirs()
File rawJsonFile = new File(outputDir, "raw-${argRepo}.json")
runCommand("$curl -H content-type:text/plain --data-binary @${aqlFile} ${server}/api/search/aql -o ${rawJsonFile}")
def artifacts = json.parse(rawJsonFile)

// Delete matching artifacts
println "[artifact-retention] Deleting ${artifacts.results.size()} artifacts."
artifacts.results.eachWithIndex { artifact, index ->
  String path = "${artifact.repo}/${artifact.path}/${artifact.name}"
  println "${index}: ${path}"
  runCommand("$curl -XDELETE ${server}/${path}")
}
