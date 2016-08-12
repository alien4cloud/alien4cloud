import org.cloudifysource.utilitydomain.context.ServiceContextFactory

context = ServiceContextFactory.getServiceContext()


evaluate(new File("${context.serviceDirectory}/scripts/apacheLB_install.groovy"))
evaluate(new File("${context.serviceDirectory}/scripts/apacheLB_postInstall.groovy"))