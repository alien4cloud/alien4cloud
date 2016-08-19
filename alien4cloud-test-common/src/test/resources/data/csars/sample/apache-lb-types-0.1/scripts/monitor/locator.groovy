//import org.cloudifysource.dsl.utils.ServiceUtils

def myPids = ServiceUtils.ProcessUtils.getPidsWithQuery("State.Name.re=httpd|apache")
println ":apacheLB-service.groovy: current PIDs: ${myPids}"
return myPids
