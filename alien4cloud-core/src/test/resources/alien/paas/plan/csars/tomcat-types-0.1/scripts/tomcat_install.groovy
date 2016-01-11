/*******************************************************************************
* Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils

def context = ServiceContextFactory.getServiceContext()
def config = new ConfigSlurper().parse(new File("${context.serviceDirectory}/scripts/tomcat-service.properties").toURL())
def instanceId = context.instanceId

println "tomcat_install.groovy: Installing tomcat..."

// Load the configuration
def catalinaHome = context.attributes.thisInstance["catalinaHome"]
def catalinaBase = context.attributes.thisInstance["catalinaBase"]
def contextPath = context.attributes.thisInstance["contextPath"]
def warUrl = context.attributes.thisService["warUrl"]

def installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceId
def applicationWar = "${installDir}/${config.warName? config.warName : new File(warUrl).name}"

//download apache tomcat
new AntBuilder().sequential {
	mkdir(dir:"${installDir}")
	
	if ( config.downloadPath.toLowerCase().startsWith("http") || config.downloadPath.toLowerCase().startsWith("ftp")) {
		echo(message:"Getting ${config.downloadPath} to ${installDir}/${config.zipName} ...")
		ServiceUtils.getDownloadUtil().get("${config.downloadPath}", "${installDir}/${config.zipName}", true, "${config.hashDownloadPath}")
	}		
	else {
		echo(message:"Copying ${context.serviceDirectory}/scripts/${config.downloadPath} to ${installDir}/${config.zipName} ...")
		copy(tofile: "${installDir}/${config.zipName}", file:"${context.serviceDirectory}/scripts/${config.downloadPath}", overwrite:false)
	}
	unzip(src:"${installDir}/${config.zipName}", dest:"${installDir}", overwrite:true)
	echo(message:"Moving ${installDir}/${config.name} to ${catalinaHome} ...")
	move(file:"${installDir}/${config.name}", tofile:"${catalinaHome}")
	chmod(dir:"${catalinaHome}/bin", perm:'+x', includes:"*.sh")
}

if ( warUrl ) {
	new AntBuilder().sequential {
		if ( warUrl.toLowerCase().startsWith("http") || warUrl.toLowerCase().startsWith("ftp")) {
			echo(message:"Getting ${warUrl} to ${applicationWar} ...")
			ServiceUtils.getDownloadUtil().get("${warUrl}", "${applicationWar}", false)
		}
		else {
			echo(message:"Copying ${context.serviceDirectory}/scripts/${warUrl} to ${applicationWar} ...")
			copy(tofile: "${applicationWar}", file:"${context.serviceDirectory}/scripts/${warUrl}", overwrite:true)
		}
	}
}

File ctxConf = new File("${catalinaBase}/conf/Catalina/localhost/${contextPath}.xml")
if (ctxConf.exists()) {
	assert ctxConf.delete()
} else {
	new File(ctxConf.getParent()).mkdirs()
}
assert ctxConf.createNewFile()
ctxConf.append("<Context docBase=\"${applicationWar}\" />")

portIncrement = 0
if (context.isLocalCloud()) {
  portIncrement = instanceId - 1
  println "tomcat_install.groovy: Replacing default tomcat port with port ${config.port + portIncrement}"
}

def serverXmlFile = new File("${catalinaBase}/conf/server.xml") 
def serverXmlText = serverXmlFile.text
portReplacementStr = "port=\"${config.port + portIncrement}\""
ajpPortReplacementStr = "port=\"${config.ajpPort + portIncrement}\""
shutdownPortReplacementStr = "port=\"${config.shutdownPort + portIncrement}\""
serverXmlText = serverXmlText.replace("port=\"8080\"", portReplacementStr) 
serverXmlText = serverXmlText.replace("port=\"8009\"", ajpPortReplacementStr) 
serverXmlText = serverXmlText.replace("port=\"8005\"", shutdownPortReplacementStr) 
serverXmlText = serverXmlText.replace('unpackWARs="true"', 'unpackWARs="false"')
serverXmlFile.write(serverXmlText)


println "tomcat_install.groovy: Tomcat installation ended"
