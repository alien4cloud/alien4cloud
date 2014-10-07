import org.cloudifysource.utilitydomain.context.ServiceContextFactory

context = ServiceContextFactory.getServiceContext()
serviceDirectory = context.serviceDirectory

builder = new AntBuilder()
builder.sequential {
  echo(message: "init: Chmodding +x ${serviceDirectory} ...")
  chmod(dir: "${serviceDirectory}", perm:"+xr", includes:"**/*")
}