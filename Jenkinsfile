pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'artifactory', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
          sh 'gradle publish -Ppublish.repository.Artifactory.username=$USERNAME -Ppublish.repository.Artifactory.password=$PASSWORD'
        }
      }
    }
  }
}
