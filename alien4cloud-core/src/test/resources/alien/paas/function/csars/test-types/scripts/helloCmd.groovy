import org.cloudifysource.dsl.utils.ServiceUtils

println "helloCmd.groovy: Starting..."

assert yourName && yourName!= "failThis", 'the name is not correct'

return "hello <${yourName}>, customHostName is <${customHostName}>, from <${context.serviceName}.${context.instanceId}>"