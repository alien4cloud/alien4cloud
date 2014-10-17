/**
* PostgreSQL start script
* Fastconnect (c) 2014
*/

import org.cloudifysource.utilitydomain.context.ServiceContextFactory

println "postgresql_start.groovy: starting"

def context = ServiceContextFactory.getServiceContext()
def script = "postgresql_start_centos.sh"

builder = new AntBuilder()
builder.sequential {
		echo(message:"postgresql_start: Chmodding +x ${context.serviceDirectory}/scripts ...")
		chmod(dir:"${context.serviceDirectory}/scripts", perm:"+x", includes:"*.sh")
		exec(executable: "${context.serviceDirectory}/scripts/${script}",failonerror: "true")
}

println "postgresql_start.groovy: finished"
