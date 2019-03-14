#! /bin/env groovy
import groovy.json.JsonSlurper

// ------------
// PROPERTIES
// ------------

String server = 'http://artifactory.praqma.net'

// ------------
// ARGUMENTS
// ------------

if (!args || args.size() < 3) {
  println "Usage:\n./${this.class.name}.groovy <aql-script> <output-dir> [username] [password]"
  System.exit(1)
}

args = args.toList() // allows for safe index access
String argAqlScript = args[0].trim()
String argOutputDir = args[1].trim()
String argUser = args[2] ? args[2].trim() : ''
String argPass = args[3] ? args[3].trim() : ''

File userDir = new File(System.getProperty('user.dir'))
File aqlScript = new File(userDir, argAqlScript)
File outputDir = new File(userDir, argOutputDir)

// ------------
// METHODS
// ------------

int runCommand (cmd) {
  println cmd
  def exit = new ProcessBuilder(cmd.split(" ")).inheritIO().start().waitFor()
  println "\nexit: ${exit}"
  assert exit == 0
  return exit
}

String getBaseName(File file) {
  return file.name.lastIndexOf('.').with {it != -1 ? file.name[0..<it] : file.name}
}

// ------------
// SCRIPT
// ------------

JsonSlurper json = new JsonSlurper()
String curl = argUser && argPass ? "curl -u ${argUser}:${argPass}" : "curl -n"
String retentionName = getBaseName(aqlScript)

if (!aqlScript.exists()) {
  println "ERROR: Expected ${aqlScript} to exist"
  System.exit(1)
}


// Build retention
// ----

// Fetch and parse builds from Artifactory
outputDir.mkdirs()
File rawJsonFile = new File(outputDir, "${retentionName}.json")
runCommand("$curl -H content-type:text/plain --data-binary @${aqlScript} ${server}/api/search/aql -o ${rawJsonFile}")
def buildJson = json.parse(rawJsonFile)

println "Deleting ${builds.results.size()} empty builds."
builds.results.eachWithIndex { build, index ->
    println "${index}: ${build."build.name"} (${build."build.number"})"

    // Due to reasons, url encoding slashes is _sometimes_ required. So just nuke both variants.
    def buildNames = [ 
      build."build.name".replace('%2F', '/'),
      build."build.name".replace('/', '%2F')
    ].unique()
    
    buildNames.each { name ->
      runCommand("$curl -X DELETE ${server}/api/build/${name}?buildNumbers=${build."build.number"}&artifacts=0")
    }
}

