import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils

def context = ServiceContextFactory.getServiceContext()
def config = new ConfigSlurper().parse(new File("${context.serviceDirectory}/scripts/apacheLB-service.properties").toURL())

currentPort = config.currentPort
println "apacheLB_startDetection.groovy: arePortsFree http=${currentPort} ..."
def sleepTimeInMiliSeconds = 3300
while (ServiceUtils.isPortFree(currentPort)) {
    println "sleeping for ${sleepTimeInMiliSeconds} mili seconds..."
    Thread.sleep(sleepTimeInMiliSeconds)
}