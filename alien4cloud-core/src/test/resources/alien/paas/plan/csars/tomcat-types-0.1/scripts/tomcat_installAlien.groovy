import org.cloudifysource.utilitydomain.context.ServiceContextFactory

context = ServiceContextFactory.getServiceContext()

evaluate(new File("${context.serviceDirectory}/scripts/tomcat_init.groovy"))
evaluate(new File("${context.serviceDirectory}/scripts/tomcat_install.groovy"))