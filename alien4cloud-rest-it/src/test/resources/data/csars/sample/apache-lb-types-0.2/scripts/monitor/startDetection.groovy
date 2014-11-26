// import org.cloudifysource.dsl.utils.ServiceUtils
// import org.cloudifysource.utilitydomain.context.ServiceContextFactory

// context = ServiceContextFactory.getServiceContext()
config = new ConfigSlurper().parse(new File("${context.serviceDirectory}/scripts/apacheLB-service.properties").toURL())
currentPort = config.currentPort
ServiceUtils.isPortOccupied(currentPort)