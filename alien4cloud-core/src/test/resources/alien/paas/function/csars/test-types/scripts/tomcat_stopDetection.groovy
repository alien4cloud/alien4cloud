import org.cloudifysource.dsl.utils.ServiceUtils

def config = new ConfigSlurper().parse(new File("${context.serviceDirectory}/scripts/tomcat-service.properties").toURL())

def currHttpPort = config.port
def currAjpPort = config.ajpPort
def sleepTimeInMiliSeconds = 3300
println "sleeping for ${sleepTimeInMiliSeconds} mili seconds..."
 Thread.sleep(sleepTimeInMiliSeconds)
 
def result = ServiceUtils.arePortsFree([currHttpPort, currAjpPort])
println "tomcat-service.groovy(stopDetection): arePortsFree http=${currHttpPort} ajp=${currAjpPort} ...${result}"
 
return result
