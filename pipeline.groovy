import hudson.model.User
import hudson.tasks.Mailer
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException

pipeline {

    agent any

    parameters {
        string(name: 'VERSION', defaultValue: "1.1.0", description: "version to be build")
        booleanParam(name: 'executeTEST', defaultValue: true)
    }

    environment {
        MY_ENV_VARIABLE = "dev"
        CRED_FROM_JENKINS=credentials('test_k8s')
        dockerhub=credentials('dockerhub-vodchyts')
    }

    tools {
        // Install the Maven version configured as "M3" and add it to the path.
        maven "M3"

    }

    stages {


        stage('Init') {
            steps {
                script {
                    echo "--->>> VARIABLES"
                    echo "Environment ${MY_ENV_VARIABLE}"
                    echo "Credentials ${CRED_FROM_JENKINS}"
                }
            }

        }

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
                      bat "docker image pull busybox:latest"

                      echo "--->>> DOCKER CONTAINER RUN"
                      bat "docker run --name mybusybox busybox"  
                       bat "docker container ls"
                      
                    }
              }

            }

            stage('Jenkins env') {
                steps {
                    echo "Version param ${VERSION}"
                    echo "Branch name ${env.BRANCH_NAME}"
              }

            }




        }
      }
            
      stage('Docker try') {
        steps {
            script {
                echo "--->>> DOCKER CONTAINER/IMAGE CLEANUP"
                cleanDocker()
                }
              }

            }

      
      stage("Build appl") {
            when {
                expression {
                    params.executeTEST
                    }
            }
            steps {
                script {
                  gitCheckout()
                }
           }

        }
        
      stage ('Maven Verify') {
        steps {

            git branch: 'main', credentialsId: 'e9f00908-5174-4fa1-82cf-9ca0e3a8c845', url: 'git@github.com:vvadmin2018/demo3.git'
            
            bat "mvn clean verify"
            
        }
         
      }

      stage ('Maven install') {
        steps {


           git branch: 'main', credentialsId: 'e9f00908-5174-4fa1-82cf-9ca0e3a8c845', url: 'git@github.com:vvadmin2018/demo3.git'
            
            withEnv(["PATH+MAVEN=${tool 'M3'}/bin:${env.JAVA_HOME}/bin"]) {

                bat "mvn --batch-mode -V -U -e clean install -Dsurefire.useFile=false"
            }

        }
      }

      stage('Docker image') {
                steps {
                    script {

                      git branch: 'main', credentialsId: 'e9f00908-5174-4fa1-82cf-9ca0e3a8c845', url: 'git@github.com:vvadmin2018/demo3.git'

                      echo "--->>> DOCKER IMAGE BUILD FROM THE DOCKER FILE"
                      bat "docker build . -t appl_fi3_image"
                      bat "docker image inspect appl_fi3_image"
                                           
                    }
              }
      }

      stage("Docker login"){
        steps{
            withCredentials([usernamePassword(credentialsId: 'dockerhub-vodchyts', passwordVariable: 'DOCKER_REGISTRY_PWD', usernameVariable: 'DOCKER_REGISTRY_USER')]) {
                bat "docker login -u ${DOCKER_REGISTRY_USER} -p ${DOCKER_REGISTRY_PWD}"
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

def cleanDocker() {
    bat "docker container stop mybusybox"
    bat "docker container rm mybusybox"
    bat "docker container ls"
    bat "docker rmi busybox:latest"
}

