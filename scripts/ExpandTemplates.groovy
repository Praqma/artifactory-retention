#! /bin/env groovy
import groovy.json.JsonSlurper

if (!args || args.size() != 3) {
  println "Usage:\n./${this.class.name}.groovy <subscription-file> <template-dir> <output-directory>"
  System.exit(1)
}

args = args.toList() // allows for safe index access
String argSubscriptionFile = args[0].trim()
String argTemplateDir = args[1].trim()
String argOutputDir = args[2].trim()

File userDir = new File(System.getProperty('user.dir'))
File subscriptionFile = new File(userDir, argSubscriptionFile)
File templateDir = new File(userDir, argTemplateDir)
File outputDir = new File(userDir, argOutputDir)

JsonSlurper json = new JsonSlurper()

def subscriptions = json.parse(subscriptionFile)
subscriptions.templates.each { template ->
  println "Expanding [${template.key}]"
  File templateSource = new File(templateDir, "${template.key}.aql")
  template.value.each { subscription ->
    String contents = templateSource.text
    subscription.each { key, value -> contents = contents.replace(key, value) }
    new File(outputDir, "template-${subscription."repository-name"}.aql").text = contents
  }
}
println "Done!"
