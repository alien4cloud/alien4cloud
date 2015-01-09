import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

def context = ServiceContextFactory.getServiceContext()

println "warHostedOnTomcat_post_configure_source<${SOURCE}> start."

if (! war_file) return "warUrl is null. So we do nothing."
def warUrl = "${context.serviceDirectory}/../${war_file}"
println "warHostedOnTomcat_post_configure_source<${SOURCE}>: Target: ${TARGET}:${tomcatIp}, Source: ${SOURCE}, warUrl is ${warUrl} and contextPath is ${contextPath}..."

def command = "${TARGET}_updateWarOnTomcat"
println "warHostedOnTomcat_post_configure_source<${SOURCE}> invoking ${command} custom command on target tomcat..."
def service = context.waitForService(TARGET_HOST, 60, TimeUnit.SECONDS)
def currentInstance = service.getInstances().find{ it.instanceId == context.instanceId }
currentInstance.invoke(command, "url=${warUrl}" as String, "contextPath=${contextPath}" as String)
println "warHostedOnTomcat_post_configure_source<${SOURCE}> end"

return true