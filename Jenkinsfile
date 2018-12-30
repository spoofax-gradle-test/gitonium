def props = readProperties defaults: ['publish': 'false', 'publish.tagged.only': 'false'], file: 'jenkins.properties'
def publish = props['publish'] == 'true'
def publishTaggedOnly = props['publish.tagged.only'] == 'true'

pipeline {
  agent any

  environment {
    JENKINS_NODE_COOKIE = 'dontKillMe' // Necessary for the Gradle daemon to be kept alive.
  }

  stages {
    stage('Build') {
     steps {
        sh 'gradle build'
      }
    }

    stage('Publish') {
      when {
        expression { return publish }
        anyOf {
          not { expression { return publishTaggedOnly } }
          allOf { expression { return publishTaggedOnly }; tag "release-*" }
        }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'artifactory', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
          sh 'gradle publish -Ppublish.repository.Artifactory.username=$USERNAME -Ppublish.repository.Artifactory.password=$PASSWORD'
        }
      }
    }
  }
}
