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


      stage("run tests") {
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
                      bat "docker run busybox"  
                    }
              }

            }
        }
      }
            
      stage('Build') {
            steps {
                // Get some code from a GitHub repository
                git branch: 'main', credentialsId: 'e9f00908-5174-4fa1-82cf-9ca0e3a8c845', url: 'git@github.com:vvadmin2018/demo3.git'
                //git 'https://github.com/jglick/simple-maven-project-with-tests.git'

                // To run Maven on a Windows agent, use
                bat "mvn -Dmaven.test.failure.ignore=true clean package"
            }
      }

        
      stage("build") {
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
 