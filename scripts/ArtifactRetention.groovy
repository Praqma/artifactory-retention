#! /bin/env groovy

// ------------
// PROPERTIES
// ------------

String server = 'http://localhost:8081/artifactory'

// ------------
// ARGUMENTS
// ------------

if (!args || args.size() < 3) {
  println "Usage:\n./${this.class.name}.groovy <repo-name> <spec-dir> <output-dir> [username] [password]"
  System.exit(1)
}

args = args.toList() // allows for safe index access
String argRepo = args[0].trim()
String argSpecDir = args[1].trim()
String argOutputDir = args[2].trim()
String argUser = args[3] ? args[3].trim() : ''
String argPass = args[4] ? args[4].trim() : ''

File userDir = new File(System.getProperty('user.dir'))
File specDir = new File(userDir, argSpecDir)
File outputDir = new File(userDir, argOutputDir)

// ------------
// METHODS
// ------------

int runIt (cmd, outputFile = null) {
  ProcessBuilder process = new ProcessBuilder(cmd.split(" "))
  if (outputFile) {
    process.redirectErrorStream(true)
    process.redirectOutput(java.lang.ProcessBuilder.Redirect.to(outputFile))
  } else {
    process.inheritIO()
  }

  int exit = process.start().waitFor()
  assert exit == 0
  return exit
}

// ------------
// SCRIPT
// ------------

String jfrogOptions = (argUser && argPass) ? "--url=${server} --user=${argUser} --password=${argPass}" : "--url=${server}"

println "[artifact-retention] INFO: Finding matching filespec"
File fileSpec = new File(specDir, "${argRepo}.json")
if (!fileSpec.exists()) {
  fileSpec = new File(specDir, "template-${argRepo}.json") // See if it was expanded from a template
  if (!fileSpec.exists()) {
    println "[artifact-retention] ERROR: Could not find a retention policy for repository '${argRepo}'"
    println "[artifact-retention] ERROR: Make sure one is present in ${specDir}"
    println "[artifact-retention] ERROR: If it is subscribed to a template policy,"
    println '[artifact-retention] ERROR: run expandTemplates.sh first.'
    System.exit(1)
  }
}
println "[artifact-retention] INFO: Using ${fileSpec}"

outputDir.mkdirs()
File rawJsonFile = new File(outputDir, "raw-${argRepo}.json")

println "[artifact-retention] INFO: Storing file spec results in ${rawJsonFile}"
String searchCmd = "jfrog rt search ${jfrogOptions} --spec=${fileSpec}"
println "[artifact-retention] INFO: ${searchCmd.replace(argPass, '*****')}"
String searchResult = runIt(searchCmd, rawJsonFile)
println "[artifact-retention] INFO: exit code ${searchResult}"

String deleteCmd = "jfrog rt delete ${jfrogOptions} --spec=${fileSpec}"
println "[artifact-retention] INFO: ${deleteCmd.replace(argPass, '*****')}"
String deleteResult = runIt(deleteCmd, rawJsonFile)
println "[artifact-retention] INFO: exit code ${deleteResult}"

println "[artifact-retention] INFO: Done!"
