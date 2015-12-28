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
import java.util.concurrent.TimeUnit

println "tomcat_configureDatasource.groovy: args[0]=${args[0]}"
println "tomcat_configureDatasource.groovy: args[1]=${args[1]}"
println "tomcat_configureDatasource.groovy: args[2]=${args[2]}"
println "tomcat_configureDatasource.groovy: args[3]=${args[3]}"

def context = ServiceContextFactory.getServiceContext()
def config = new ConfigSlurper().parse(new File("${context.serviceDirectory}/relationshipScripts/tomcat-service.properties").toURL())

def instanceId = context.instanceId
def installDir = System.properties["user.home"]+ "/.cloudify/${config.serviceName}" + instanceId
def applicationWar = "${installDir}/${config.warName? config.warName : new File(warUrl).name}"

println "tomcat_configureDatasource.groovy: Starting ..."

println "tomcat_configureDatasource.groovy: This tomcat instance Id is ${instanceId}"
def catalinaBase = context.attributes.thisInstance["catalinaBase"]
println "tomcat_configureDatasource.groovy: tomcat(${instanceId}) catalinaBase ${catalinaBase}"
def envVar = context.attributes.thisInstance["envVar"]

def contextPath = context.attributes.thisInstance["contextPath"]

println "tomcat_configureDatasource.groovy: Adding PostgreSQL JDBC Driver ..."
new AntBuilder().sequential {
	echo(message: "Getting ${config.jdbcDriverName}")
	get(src:"${config.jdbcDriverUrl}/${config.jdbcDriverName}", dest:"${catalinaBase}/lib/${config.jdbcDriverName}", skipexisting:true)
}
println "tomcat_configureDatasource.groovy: PostgreSQL JDBC Driver Added"

println "tomcat_configureDatasource.groovy: Adding PostgresDS ressource to application's context ..."
File ctxConf = new File("${catalinaBase}/conf/Catalina/localhost/${contextPath}.xml")
if (ctxConf.exists()) {
	assert ctxConf.delete()
} else {
	new File(ctxConf.getParent()).mkdirs()
}
assert ctxConf.createNewFile()
ctxConf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
ctxConf.append("<Context docBase=\"${applicationWar}\" >")
ctxConf.append("	<Resource name=\"PostgresDS\" auth=\"Container\" type=\"javax.sql.DataSource\" driverClassName=\"org.postgresql.Driver\" url=\"jdbc:postgresql://[servername]:[port]/${config.dbName}\" username=\"${config.dbUser}\" password=\"${config.dbPassW}\" />")
ctxConf.append("</Context>")
println "tomcat_configureDatasource.groovy: PostgresDS ressource added to application's context"

println "tomcat_configureDatasource.groovy: Adding PostgresDS ressource to Tomcat configuration (web.xml) ..."
def oldEndOfFile = "</web-app>"
def newEndOfFile = "    <resource-ref>" + System.getProperty("line.separator") + "        <description>postgreSQL Datasource</description>" + System.getProperty("line.separator") + "        <res-ref-name>PostgresDS</res-ref-name>" + System.getProperty("line.separator") + "        <res-type>javax.sql.DataSource</res-type>" + System.getProperty("line.separator") + "        <res-auth>Container</res-auth>" + System.getProperty("line.separator") + "    </resource-ref>" + System.getProperty("line.separator") + System.getProperty("line.separator") + "${oldEndOfFile}"

def webXmlFile = new File("${catalinaBase}/conf/web.xml")
def webXmlText = webXmlFile.text
def modifiedWebXmlText = webXmlText.replace("${oldEndOfFile}", "${newEndOfFile}")
webXmlFile.text = modifiedWebXmlText
println "tomcat_configureDatasource.groovy: PostgresDS ressource added to Tomcat configuration (web.xml)"

println "tomcat_configureDatasource.groovy: waiting for ${config.dbServiceName}..."
def dbService = context.waitForService(args[3], 20, TimeUnit.SECONDS)
def dbInstances = dbService.waitForInstances(dbService.numberOfPlannedInstances, 60, TimeUnit.SECONDS) 
def dbServiceHost = dbInstances[0].hostAddress
envVar.put(config.dbHostVarName, "${dbServiceHost}")
println "tomcat_configureDatasource.groovy: ${config.dbServiceName} host is ${dbServiceHost}"
def dbServicePort = "${config.dbPortVarName}"
envVar.put(config.dbPortVarName, "${dbServicePort}")
println "tomcat_configureDatasource.groovy: ${config.dbServiceName} port is ${dbServicePort}"

println "tomcat_configureDatasource.groovy: tomcat(${instanceId}) envVar ${envVar}"

def ctxConfFile = new File("${catalinaBase}/conf/Catalina/localhost/${contextPath}.xml")
def ctxConfText = ctxConfFile.text

def oldServerStr = "[servername]"
def oldPortStr = "[port]"

def newServerStr = "${dbServiceHost}"
def newPortStr = "${dbServicePort}"

println "tomcat_configureDatasource.groovy: ${catalinaBase}/conf/Catalina/localhost/${contextPath}.xml was : ${ctxConfText}"
ctxConfFile.text = ctxConfText.replace(oldServerStr, newServerStr).replace(oldPortStr, newPortStr)
println "tomcat_configureDatasource.groovy: ${catalinaBase}/conf/Catalina/localhost/${contextPath}.xml is now : ${ctxConfFile.text}"

println "tomcat_configureDatasource.groovy: Ended"
