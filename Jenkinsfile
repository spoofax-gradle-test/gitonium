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

    stage('Deploy') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'artifactory', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
          sh 'gradle publish -Ppublish.repository.Artifactory.username=$USERNAME -Ppublish.repository.Artifactory.password=$PASSWORD'
        }
      }
    }
  }
}
