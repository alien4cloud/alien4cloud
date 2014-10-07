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


def warUrl=args[0]

def context = ServiceContextFactory.getServiceContext()
def serviceName = context.serviceName
def instanceId = context.instanceId

println "tomcat-service.groovy(updateWar custom command): warUrl is ${warUrl}..."
if (! warUrl) return "warUrl is null. So we do nothing."
context.attributes.thisService["warUrl"] = "${warUrl}"

println "tomcat-service.groovy(updateWar customCommand): invoking updateWarFile custom command ..."
def service = context.waitForService(context.serviceName, 60, TimeUnit.SECONDS)
def currentInstance = service.getInstances().find{ it.instanceId == context.instanceId }
currentInstance.invoke("updateWarFile")

println "tomcat-service.groovy(updateWar customCommand): End"
return true