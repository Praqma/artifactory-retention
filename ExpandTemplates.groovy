#! /bin/env groovy
import groovy.json.JsonSlurper

JsonSlurper json = new JsonSlurper()
def subscriptions = json.parse(new File('template-subscription.json'))

subscriptions.templates.each { template ->
  println "Expanding [${template.key}]"
  File templateSource = new File("templates/${template.key}.json")
  template.value.each { subscription ->
    String contents = templateSource.text
    subscription.each { key, value -> contents = contents.replace(key, value) }
    new File("artifact-aql/template-${subscription."repository-name"}.json").text = contents
  }
}
println "Done!"
