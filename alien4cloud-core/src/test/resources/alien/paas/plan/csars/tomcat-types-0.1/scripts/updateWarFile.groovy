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

println "updateWarFile.groovy: Starting..."

def context = ServiceContextFactory.getServiceContext()
def config  = new ConfigSlurper().parse(new File("${context.serviceDirectory}/scripts/tomcat-service.properties").toURL())
def instanceId = context.instanceId

def warUrl=context.attributes.thisService["warUrl"] 
println "updateWarFile.groovy: warUrl is ${warUrl}"

if (! warUrl) return "warUrl is null. So we do nothing."

def catalinaBase = context.attributes.thisInstance["catalinaBase"]
def contextPath = context.attributes.thisInstance["contextPath"]

def installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceId
def applicationWar = "${installDir}/${config.warName?: new File(warUrl).name}"

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

File ctxConf = new File("${catalinaBase}/conf/Catalina/localhost/${contextPath}.xml")
if (ctxConf.exists()) {
	assert ctxConf.delete()
} else {
	new File(ctxConf.getParent()).mkdirs()
}
assert ctxConf.createNewFile()
ctxConf.append("<Context docBase=\"${applicationWar}\" />")

println "updateWarFile.groovy: End"
return true