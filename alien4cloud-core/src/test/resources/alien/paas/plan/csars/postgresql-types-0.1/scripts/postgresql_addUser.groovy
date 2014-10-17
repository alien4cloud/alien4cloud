
import org.cloudifysource.utilitydomain.context.ServiceContextFactory

def context = ServiceContextFactory.getServiceContext()
def script = "postgresql_addUser_centos.sh"

def dbUser = args[0]
def dbPassW = args[1]
def dbName = args[2]

println "postgresql_addUser: Starting ..."

builder = new AntBuilder()
builder.sequential {
	echo(message:"postgresql_addUser: Chmodding +x ${context.serviceDirectory}/scripts ...")
	chmod(dir:"${context.serviceDirectory}/scripts", perm:"+x", includes:"*.sh")
	echo(message:"postgresql_addUser: Adding \"${dbUser}\" to postgresql with \"${dbPassW}\" as password and \"${dbName}\" as database name ...")
	exec(executable: "${context.serviceDirectory}/scripts/${script}",failonerror: "true") {
		arg(value:"${dbUser}")
		arg(value:"${dbPassW}")
		arg(value:"${dbName}")
	}
}

println "postgresql_addUser: Ended"
