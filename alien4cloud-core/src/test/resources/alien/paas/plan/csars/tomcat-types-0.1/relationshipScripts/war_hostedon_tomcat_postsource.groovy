import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils

def context = ServiceContextFactory.getServiceContext()

def warNodeName = args[2]
def warUrl = "../../${warNodeName}/content/webarchive.war"

println "war hosted on post_configure_source start"

def serviceName = context.serviceName
def instanceId = context.instanceId

println "tomcat-service.groovy(updateWar custom command): warUrl is ${warUrl}..."
if (! warUrl) return "warUrl is null. So we do nothing."
context.attributes.thisService["warUrl"] = "${warUrl}"

println "tomcat-service.groovy(updateWar customCommand): invoking updateWarFile custom command ..."
def service = context.waitForService(context.serviceName, 60, TimeUnit.SECONDS)
def currentInstance = service.getInstances().find{ it.instanceId == context.instanceId }
currentInstance.invoke("updateWarFile")

println "tomcat-service.groovy(updateWar customCommand): End"
return true

println "war hosted on tomcat pre_configure_target end"