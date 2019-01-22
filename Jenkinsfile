node() {
  stage('checkout') {
    deleteDir()
    checkout scm
  }

  stage('expand templates') {
    docker.image('groovy').inside {
      sh "groovy ExpandTemplates.groovy"
    }
  }

  stage('prepare cleanup') {
    def aqlFiles = findFiles(glob: 'artifact-aql/*.json')
    
    // Create a map: <repositoryName:buildClosure>
    builds = aqlFiles.collectEntries { aql ->
      String repoName = aql.name.replace('.json', '').replace('template-', '')
      return [
        (repoName.toLowerCase()):{
          docker.image('groovy').inside {
            withCredentials([usernamePassword(
              credentialsId: 'artifactory-user',
              usernameVariable: 'username',
              passwordVariable: 'password'
            )]) {
              sh "groovy ArtifactRetention.groovy $repoName $username \'$password\'"
            }
          }
        }
      ]
    }.sort()
  }

  // Run the builds in parallel in chunks of 10
  chunks = builds.keySet().collate(10).collect { builds.subMap(it) }
  chunks.eachWithIndex { buildsChunk, i ->
    stage("clean repositories (${i+1}/${chunks.size()})") { 
      parallel buildsChunk
    }
  }

  // Cleanup builds without artifacts
  stage('clean empty builds') {
    docker.image('groovy').inside {
      withCredentials([usernamePassword(
        credentialsId: 'artifactory-user',
        usernameVariable: 'username',
        passwordVariable: 'password'
      )]) {
        sh "groovy BuildRetention.groovy $username \'$password\'"
      }
    }
  }
}
