import hudson.model.User
import hudson.tasks.Mailer
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException

pipeline {

    agent any

    tools {
        // Install the Maven version configured as "M3" and add it to the path.
        maven "M3"
        git "Default"
    }

    stages {


      stage("docker play") {
        parallel {
            stage('Print Env') {
                steps {
                    bat "set"
                }
            }
             stage('Docker try') {
                steps {
                    script {
                      bat "docker info"
                      bat "docker ps"
                      
                      echo "--->>> DOCKER IMAGE PULL"
                      bat "docker rmi busybox:latest"
                      bat "docker image pull busybox:latest"

                      echo "--->>> DOCKER CONTAINER RUN"
                      bat "docker run --name mybusybox busybox"  
                       bat "docker container ls"
                      
                    }
              }

            }
        }
      }
            
      stage('Docker try') {
        steps {
            script {
                echo "--->>> DOCKER CONTAINER/IMAGE CLEANUP"
                bat "docker container stop"
                bat "docker container rm mybusybox"
                bat "docker container ls"
                bat "docker rmi busybox:latest"
                }
              }

            }

      stage('K8s try') {
        steps {
            script {
                echo "--->>> KUBERNETES VERISON"
                bat "kubectl version --short"
                }
              }

            }

      stage("Build appl") {
            steps {
                script {
                  gitCheckout()
                }
           }

        }
    }

    post {
                // If Maven was able to run the tests, even if some of the test
                // failed, record the test results and archive the jar file.
                success {
                    junit '**/target/surefire-reports/TEST-*.xml'
                    archiveArtifacts 'target/*.jar'
                }
            }
    
}

def gitCheckout() {

  checkout(
    changelog: false,
    poll: false,
    scm: [
     $class: "GitSCM",
     branches: [[name: "main"]],
     doGenerateSubmoduleConfigurations: false,
     extensions: [
       [$class: "RelativeTargetDirectory", relativeTargetDir: "vvadmin2018/demo3"],
       [$class: "CloneOption", depth: 1, honorRefspec: true, noTags: true, reference: "", shallow: true]
     ],
     submoduleCfg: [],
     userRemoteConfigs: [
       [
         credentialsId: 'e9f00908-5174-4fa1-82cf-9ca0e3a8c845',
         url: "git@github.com:vvadmin2018/demo3.git"
       ]
     ]
   ]

  )

}
 