import org.cloudifysource.dsl.utils.ServiceUtils

println "updateWarOnTomcat.groovy: Starting..."

assert url && !url.trim().isEmpty(), "requires url parameter"
assert contextPath && !contextPath.trim().isEmpty(), "requires contextPath parameter"

println "updateWarOnTomcat.groovy: catalinaBase: ${catalinaBase}, installDir: ${installDir}, warUrl: ${url}, contextPath: ${contextPath}"

def applicationWar = "${installDir}/${new File(url).name}"

//get the WAR file
new AntBuilder().sequential {
	if ( url.toLowerCase().startsWith("http") || url.toLowerCase().startsWith("ftp")) {
		echo(message:"Getting ${url} to ${applicationWar} ...")
		ServiceUtils.getDownloadUtil().get("${url}", "${applicationWar}", false)
	}
	else {
		echo(message:"Copying ${url} to ${applicationWar} ...")
		copy(tofile: "${applicationWar}", file:"${url}", overwrite:true)
	}
}

//configure its tomcat context
File ctxConf = new File("${catalinaBase}/conf/Catalina/localhost/${contextPath}.xml")
if (ctxConf.exists()) {
	assert ctxConf.delete()
} else {
	new File(ctxConf.getParent()).mkdirs()
}
assert ctxConf.createNewFile()
ctxConf.append("<Context docBase=\"${applicationWar}\" />")

println "updateWarOnTomcat.groovy: End"
return true