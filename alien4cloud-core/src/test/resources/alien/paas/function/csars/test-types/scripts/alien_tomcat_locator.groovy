import org.cloudifysource.dsl.utils.ServiceUtils

def myPids = ServiceUtils.ProcessUtils.getPidsWithQuery("State.Name.eq=java,Args.*.eq=org.apache.catalina.startup.Bootstrap")
println "tomcat-service.groovy: current PIDs: ${myPids}"
return myPids