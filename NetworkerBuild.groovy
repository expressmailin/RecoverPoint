import groovy.transform.Field


def checkoutArches = ["solarissparc", "nt86"]
def cleanArches = ["solarissparc", "aixpower", "nt86"]
def mkdirsArches = ["solarissparc", "aixpower", "nt86"]
def buildArches = ["solarissparc", "aixpower", "aixpower32"]


def checkoutArchesMap = checkoutArches.collectEntries {
    ["${it}" : generateStage(it,'checkout')]
}

def cleanArchesMap = cleanArches.collectEntries {
    ["${it}" : generateStage(it,'clean')]
}

def mkdirsArchesMap = mkdirsArches.collectEntries {
    ["${it}" : generateStage(it,'mkdirs')]
}

def buildArchesMap = buildArches.collectEntries {
    ["${it}" : generateStage(it,'build')]
}

def generateStage(arch,step) {
    return {

	   if(step == 'checkout'){
        stage("stage: ${arch}") {
                echo "Build running on  ${arch}."
 				RunShCmd("perl /disks/sc_renas_nwmasterzone/build/nw_checkout.pl --ARCH=${arch} --BUILDZONE=${product} --debug=on ${branch}")
        }
	   }
	   if(step == 'clean'){
        stage("stage: ${arch}") {
                echo "Clean running on ${arch}."
                RunShCmd( "perl /disks/sc_renas_nwmasterzone/build/nwmigration/buildCleanfinal.pl --arch=${arch} --branch=${branch}" )
        }
	   }
	   if(step == 'mkdirs'){
        stage("stage: ${arch}") {
                echo "Mkdirs running on ${arch}."
                RunShCmd( "perl /disks/sc_renas_nwmasterzone/build/nwmigration/mkdirs.pl  --arch=${arch} --branch=${branch}" )
        }
	   }
	   if(step == 'build'){
        stage("stage: ${arch}") {
                echo "Build running on ${arch}."
                RunShCmd( "perl /disks/sc_renas_nwmasterzone/build/nw_build.pl --ARCH=${arch} --BUILDZONE=${product} --debug=on ${branch}" )
        }
	   }
    }
}


 def RunShCmd(String cmd) {
 
  Cmd_Status = sh (returnStatus: true, script: cmd)


}


pipeline {
    agent { label 'LAUNCHER' }
		stages {
	 stage('Prebuild') {
            steps {
                script {
				    echo "Branch Selected = '${branch}'\n"
				   }
				}
		}		
	   stage('Checkout') {
	    failFast true
            steps {
                script {
					echo "Checkout step started.\n"
                    parallel checkoutArchesMap
					echo "Checkout step completed.\n"
                }
            }
        }	
        stage('History') {
            steps {
			     echo "History step started.\n"
                 RunShCmd( "perl /disks/sc_renas_nwmasterzone/build/nw_history.pl --BUILDZONE=${product} --debug=on ${branch}" )
				 echo "History step completed.\n"
            }
        }
		stage('Clean') {
		failFast true
            steps {
                script {
				    echo "Clean step started.\n"
                    parallel cleanArchesMap
					echo "Clean step completed.\n"
                }
            }
        }
		stage('mkdirs') {
            steps {
                script {
				    echo "mkdirs step started.\n"
                    parallel mkdirsArchesMap
					echo "mkdirs step completed.\n"
                }
            }
        }
		
		stage('Build') {
            steps {
                script {
				    echo "Build step started.\n"
                    parallel buildArchesMap
					echo "Build step completed.\n"
                }
            }
        }
		
		stage("Cstyle"){
				  steps{
					   script {
					       echo "Cstyle step started.\n"
						   RunShCmd( "perl /disks/sc_renas_nwmasterzone/build/nw_cstyle.pl --ARCH=${'solarisx64'} --BUILDZONE=${product} --debug=on ${branch}" )
						  echo "Cstyle step completed.\n"
						}
					}
				}
	   stage('Output') {
            steps {
                script {
				    echo "Output step started.\n"
                    RunShCmd( "perl /disks/sc_renas_nwmasterzone/build/nw_output.pl --ARCH=${'generic'} --BUILDZONE=${product} --debug=on ${branch}" )
					echo "Output step completed.\n"
                }
            }
        }			

       stage('RelClean') {
            steps {
                script {
				    echo "RelClean step started.\n"
                    RunShCmd( "perl /disks/sc_renas_nwmasterzone/build/nw_relclean.pl --ARCH=${'generic'} --BUILDZONE=${product} --debug=on ${branch}" )
					echo "RelClean step completed.\n"
                }
            }
        }
		
	 stage('pre_package') {
	  when { 
            expression { params.REL_CAND == true }
       }
            steps {
                script {
				    echo " Add code for pre_package \n"
				   }
				}
		}	

     stage('post_package') {
	  when { 
            expression { params.REL_CAND == true }
       }
            steps {
                script {
				    echo " Add code for post_package \n"
				   }
				}
		}	
        
         stage('Images') {
            steps {
                script {
				    echo "Images step started.\n"
                    RunShCmd( "ssh build@zipi perl /disks/nasbld/master/build/nw_images.pl --ARCH=${'solarissparc'} --BUILDZONE=${product} --debug=on ${branch}" )
					echo "Images step completed.\n"
                }
            }
        }
		
		stage('CheckLog') {

            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh "exit 1"
                }
            }

    }
       	

    }
	
	post {
        success {
            echo 'SUCCESS'            
        }
        unstable {
            echo 'UNSTABLE'
        }
        failure {
            echo 'FAILURE'
        }
    }
}
