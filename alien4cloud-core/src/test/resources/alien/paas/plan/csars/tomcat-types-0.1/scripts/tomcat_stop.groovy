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

println "tomcat_stop.groovy: About to stop tomcat..."

def context = ServiceContextFactory.getServiceContext()

def catalinaHome = context.attributes.thisInstance["catalinaHome"]
def catalinaBase = context.attributes.thisInstance["catalinaBase"]

new AntBuilder().sequential {
	exec(executable:"${catalinaHome}/bin/catalina.sh", osfamily:"unix") {
		env(key:"CLASSPATH", value: "") // reset CP to avoid side effects (Cloudify passes all the required files to Groovy in the classpath)
		env(key:"CATALINA_HOME", value: "${catalinaHome}")
		env(key:"CATALINA_BASE", value: "${catalinaBase}")
		arg(value:"stop")
	}
	exec(executable:"${catalinaHome}/bin/catalina.bat", osfamily:"windows"){
		env(key:"CLASSPATH", value: "") // reset CP to avoid side effects (Cloudify passes all the required files to Groovy in the classpath)
		env(key:"CATALINA_HOME", value: "${catalinaHome}")
		env(key:"CATALINA_BASE", value: "${catalinaBase}")
		arg(value:"stop")
	}
}

println "tomcat_stop.groovy: tomcat is stopped"
