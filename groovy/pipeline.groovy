def callPost(String urlString, String queryString) {
    def url = new URL(urlString)
    def connection = url.openConnection()
    connection.setRequestMethod("POST")
    connection.doInput = true
    connection.doOutput = true
    connection.setRequestProperty("content-type", "application/json;charset=UTF-8")

    def writer = new OutputStreamWriter(connection.outputStream)
    writer.write(queryString.toString())
    writer.flush()
    writer.close()
    connection.connect()

    new groovy.json.JsonSlurper().parseText(connection.content.text)
}

def callGetJira(String urlString) {
    def url = new URL(urlString)
    def connection = url.openConnection()
    connection.setRequestMethod("GET")
    def encoded = ("ncorpan:Gringoloco1").bytes.encodeBase64().toString()
    def basicauth = "Basic ${encoded}"
    connection.setRequestProperty("Authorization", basicauth)
    connection.connect()

    new groovy.json.JsonSlurper().parseText(connection.content.text)
}

node {
    
    // GLOBAL VARIABLES
    def NAME = "springboot-corpancho-2"
    def BUILDPACKSTRING = ""
    def LINKS = ""
    def JIRALINK = ""
    def BUSINESS_INFO = ""
    /*
    deleteDir()

    stage('Sources') {
        checkout([
                $class           : 'GitSCM',
                branches         : [[name: "refs/heads/master"]],
                extensions       : [[$class: 'CleanBeforeCheckout', localBranch: "master"]],
                userRemoteConfigs: [[
                                            credentialsId: 'cbf178fa-56ee-4394-b782-36eb8932ac64',
                                            url          : "https://github.com/Nicocovi/MS-Repo"
                                    ]]
                ])
    }
    
   */
    dir("") {
        /*
        stage("Build"){
            sh "gradle build"
        }
        
        stage("Validating Config"){
            //TODO
            //Validate jira link in links.config
            def currentDir = new File(".").absolutePath
            env.WORKSPACE = pwd() // present working directory.
            def file = readFile "${env.WORKSPACE}/links.config"
            def trimmedText = file.trim().replaceAll('\t',' ').replaceAll('\r\n',' ').replaceAll(" +",";").split(";")
            echo "trimmedText: ${trimmedText}"
            int index = -1;
            for (int i=0;i<trimmedText.length;i++) {
                if (trimmedText[i].contains("jira")) {
                    index = i+1;
                    break;
                }
            }
            JIRALINK = trimmedText[index]
            String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" //website regex
            //TODO 
            //JIRALINK matches regex
            for (i = 1; i <trimmedText.size(); i = i+2) {
                LINKS = LINKS+"\""+trimmedText[i]+"\":"+"\""+trimmedText[i+1]+"\","
            }
            LINKS = LINKS.substring(0, (LINKS.length())-1)//remove last coma
            echo LINKS
        }
        */
        stage("Get Jira Information"){
            //TODO: GET /rest/api/2/project/{projectIdOrKey}
            /*
            // http://localhost:8099/
            API Call to JIRALINK
            def id = ""
            def appName = ""
            def owner = ""
            def description = ""
            def short_name = ""
            def type = ""
            Domain: XXX
            Subdomain: XXX
            Product: XXX
            Owner: XXX
            Description: XXX
            */
            //def basicinfo = "\"id\": \"XXX\", \"name\": \"XXX\", \"owner\": \"XXX\", \"description\": \"XXX\", \"short_name\": \"XXX\", \"type\": \"XXX\","
            def jiraProject = callGetJira("http://localhost:8099/rest/api/2/project/MAST")
            echo "jiraProject: ${jiraProject}"
            DOMAIN = "Finance"
            SUBDOMAIN = "Taxes"
            BUSINESS_CAPABILITY = "tax_calculation"
            BUSINESS_INFO = " \"domain\": \"${DOMAIN}\", \"subdomain\": \"${SUBDOMAIN}\", \"business_capability\": \"${BUSINESS_CAPABILITY}\" "
        
        }
        /*
        stage('Deploy') {
            def branch = ['master']
            def path = "build/libs/gs-spring-boot-0.1.0.jar"
            def manifest = "manifest.yml"
            
               if (manifest == null) {
                throw new RuntimeException('Could not map branch ' + master + ' to a manifest file')
               }
               withCredentials([[
                                     $class          : 'UsernamePasswordMultiBinding',
                                     credentialsId   : '98c5d653-dbdc-4b52-81ba-50c2ac04e4f1',
                                     usernameVariable: 'CF_USERNAME',
                                     passwordVariable: 'CF_PASSWORD'
                             ]]) {
                sh 'cf login -a https://api.run.pivotal.io -u $CF_USERNAME -p $CF_PASSWORD --skip-ssl-validation'
                sh 'cf target -o ncorpan-org -s development'
                sh 'cf push '+NAME+' -f '+manifest+' --hostname '+NAME+' -p '+path
            }
        }
        
        
        stage("Get Runtime Information"){
            APP_STATUS = sh (
                script: 'cf app '+NAME,
                returnStdout: true
            )
            LENGTH = APP_STATUS.length()
            INDEX = APP_STATUS.indexOf("#0", 0)
            APP_SHORTSTATUS = (APP_STATUS.substring(INDEX,LENGTH-1)).replaceAll("\n","  ").replaceAll("   ",";").split(";")
            echo "SHORTSTATUS: ${APP_SHORTSTATUS}"
            
            APP_BUILDPACKS_INDEX = APP_STATUS.indexOf("buildpacks", 0)
            APP_TYPE_INDEX = APP_STATUS.indexOf("type", 0)
            APP_BUILDPACKS = (APP_STATUS.substring(APP_BUILDPACKS_INDEX+11,APP_TYPE_INDEX-1)).trim().replaceAll("\n","").replaceAll(" ",";").split(";") //trim for \n
            //+11 length of 'buildpacks'
            echo "APP_BUILDPACKS: ${APP_BUILDPACKS}"
            //include buildpacks
            def iterations = APP_BUILDPACKS.size()
            def buildpacks = "  \"service\": { \"buildpacks\":["
            for (i = 0; i <iterations; i++) {
                if(i==2){
                    //buildpack contains uncodedable chars (arrows)
                }else{
                    buildpacks = buildpacks+"\""+APP_BUILDPACKS[i]+"\","
                }
            }
            buildpacks = buildpacks.substring(0, (buildpacks.length())-1) //remove last coma
            BUILDPACKSTRING = buildpacks+"]"
            echo "buildpackstring: ${BUILDPACKSTRING}"
            //TODO network policies
            CF_NETWORK_POLICIES_SOURCE = sh (
                script: 'cf network-policies --source '+NAME,
                returnStdout: true
            )
            CF_NETWORK_POLICIES = CF_NETWORK_POLICIES_SOURCE.substring((CF_NETWORK_POLICIES_SOURCE.indexOf("ports", 0)+5), (CF_NETWORK_POLICIES_SOURCE.length())-1)
            CF_NETWORK_POLICIES = CF_NETWORK_POLICIES.trim().replaceAll('\t',' ').replaceAll('\n',' ').replaceAll('\r\n',' ')replaceAll(" +",";").split(";")
            echo "CF_NETWORK_POLICIES: ${CF_NETWORK_POLICIES}"
            APP_SERVICES = ",\"provides\": ["
            for (int i=0;i<(CF_NETWORK_POLICIES.size() / 4);i++) {
                APP_SERVICES = APP_SERVICES + "{\"service_name\": \""+CF_NETWORK_POLICIES[1+i*4]+"\"},"
            }
            APP_SERVICES = APP_SERVICES.substring(0, (APP_SERVICES.length())-1) //remove last coma
            APP_SERVICES = BUILDPACKSTRING + APP_SERVICES + "]}"
            echo "APP_SERVICES: ${APP_SERVICES}"
            
            /*
            CF_NETWORK_POLICIES_DESTINATION = sh (
                script: 'cf network-policies --destination '+NAME,
                returnStdout: true
            )
            echo "CF_NETWORK_POLICIES_DESTINATION: ${CF_NETWORK_POLICIES_DESTINATION}"
            */
        /*
        }//stage
        
        
        stage("Push Documentation"){
            //TODO generate ID, ... (basic info)
            def basicinfo = "\"id\": \"14101812\", \"name\": \""+NAME+"\", \"owner\": \"Nico\", \"description\": \"bla\", \"short_name\": \"serviceAZ12\", \"type\": \"service\", \"status\": \"${APP_SHORTSTATUS[1]}\""
            def runtime = " \"runtime\": {\"ram\": \"${APP_SHORTSTATUS[4]}\", \"cpu\": \"${APP_SHORTSTATUS[3]}\", \"disk\": \"${APP_SHORTSTATUS[5]}\", \"host_type\": \"cloudfoundry\" }"
            echo "LINKS: ${LINKS}"
            def jsonstring = "{"+basicinfo+","+runtime+","+LINKS+","+APP_SERVICES+"}"
            echo "JSONSTRING: ${jsonstring}"
            
            try {
                    callPost("http://192.168.99.100:9123/document", jsonstring) //Include protocol
                } catch(e) {
                    // if no try and catch: jenkins prints an error "no content-type" but post request succeeds
                }
        }//stage
       */
    }

}
