#! /bin/env groovy
import groovy.json.JsonSlurper

// ------------
// ARGUMENTS
// ------------

if (!args) {
  println "Usage:\n./${this.class.name}.groovy [username] [password]"
}

String user = args.size() > 0 ? args[0].trim() : ''
String pass = args.size() > 1 ? args[1].trim() : ''

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

// ------------
// SCRIPT
// ------------

JsonSlurper json = new JsonSlurper()
String curl = user && pass ? "curl -u ${user}:${pass}" : "curl -n"

// Create result directory and file handles
File resultDir = new File('result')
resultDir.mkdirs()

// Fetch and parse builds from Artifactory
File rawJsonFile = new File(resultDir, 'builds-without-artifacts.json')
runCommand("$curl -H content-type:text/plain --data-binary @build-aql/builds-without-artifacts.json ${server}/api/search/aql -o ${resultDir.name}/${rawJsonFile.name}")
def builds = json.parse(rawJsonFile)

int amountToDelete = builds.results.size() * percentageToDelete
println "Deleting ${amountToDelete} out of ${builds.results.size()} empty builds."
builds.results.eachWithIndex { build, index ->
    println "${index}: ${build."build.name"} (${build."build.number"})"
    if (index >= amountToDelete) { return }

    // Due to reasons, url encoding slashes is _sometimes_ required. So just nuke both variants.
    def buildNames = [ 
      build."build.name".replace('%2F', '/'),
      build."build.name".replace('/', '%2F')
    ].unique()
    
    buildNames.each { name ->
      runCommand("$curl -X DELETE ${server}/api/build/${name}?buildNumbers=${build."build.number"}&artifacts=0")
    }
}
