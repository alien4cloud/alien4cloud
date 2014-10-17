import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils

def context = ServiceContextFactory.getServiceContext()
def config = new ConfigSlurper().parse(new File("${context.serviceDirectory}/scripts/tomcat-service.properties").toURL())

def currHttpPort = config.port
def currAjpPort = config.ajpPort

println "tomcat-service.groovy(startDetection): arePortsFree http=${currHttpPort} ajp=${currAjpPort} ..."
def sleepTimeInMiliSeconds = 3300
while (ServiceUtils.arePortsFree([currHttpPort, currAjpPort])) {
 println "sleeping for ${sleepTimeInMiliSeconds} mili seconds..."
 Thread.sleep(sleepTimeInMiliSeconds)
}