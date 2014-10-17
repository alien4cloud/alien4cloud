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

def context = ServiceContextFactory.getServiceContext()
def config = new ConfigSlurper().parse(new File("${context.serviceDirectory}/relationshipScripts/tomcat-service.properties").toURL())

println "tomcat_postgreAddUser.groovy: Starting ... "

println "tomcat_postgreAddUser.groovy: waiting for ${config.dbServiceName}..."
def dbService = context.waitForService(args[3], 20, TimeUnit.SECONDS)

def user = config.dbUser
def passw = config.dbPassW
def name = config.dbName

println "tomcat_postgreAddUser.groovy: Invoking addUser of ${config.dbServiceName} ..."
println "tomcat_postgreAddUser.groovy: About to add user \"${user}\" to postgresql with \"${passw}\" as password and \"${name}\" as database name ..."
dbService.invoke("addUser", user as String, passw  as String, name as String)

println "tomcat_postgreAddUser.groovy: Ended "
