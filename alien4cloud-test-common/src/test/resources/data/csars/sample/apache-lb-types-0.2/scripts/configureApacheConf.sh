#!/bin/bash

# args:
# $1 The original port in the httpd.conf (ususally 80)
# $2 The required port in the httpd.conf
# $3 Full path of the httpd-proxy-balancer.conf in the recipe (context.serviceDirectory/overrides-linux/conf/extra)
# $4 The current application's name 
# $5 useStickysession: "true" or "false"
# $6 Full path of the recipe folder (context.serviceDirectory)
# 


origPort=$1
newPort=$2
origProxyBalancerPath=$3
applicationName=$4
useStickysession=`echo $5 | tr '[A-Z]' '[a-z]'`
serviceDirectory=$6


# args:
# $1 the error code of the last command (should be explicitly passed)
# $2 the message to print in case of an error
# 
# an error message is printed and the script exists with the provided error code
function error_exit {
	echo "$2 : error code: $1"
	exit ${1}
}


export PATH=$PATH:/usr/sbin:/sbin || error_exit $? "Failed on: export PATH=$PATH:/usr/sbin:/sbin"

httpdLocation=`whereis httpd`
for i in ${httpdLocation}
do    
	if [ -d "$i" ] ; then
		currConf="$i/conf/httpd.conf"		
		if [ -f "${currConf}" ] ; then
			echo "Conf is in ${currConf}"
			echo "Replacing $origPort with $newPort..."
			sudo sed -i -e "s/$origPort/$newPort/g" ${currConf} || error_exit $? "Failed on: sudo sed -i -e $origPort/$newPort"
			endOfFile="\/VirtualHost>"
			includeBalancer="Include conf\/extra\/httpd-proxy-balancer\.conf"
			sudo sed -i -e "s/$endOfFile/$endOfFile\r\n\r\n$includeBalancer\r\n/g" ${currConf} || error_exit $? "Failed on: sudo sed -i -e $includeBalancer ${currConf}"
			echo "End of ${currConf} replacements"
			extraFolder="$i/conf/extra"
			echo "mkdir ${extraFolder} ..."
			sudo mkdir -p ${extraFolder}
			
			
			proxyBalancerConf="httpd-proxy-balancer.conf"
			proxyBalancerPath="${extraFolder}/${proxyBalancerConf}"
			echo "Copying ${proxyBalancerConf} from ${origProxyBalancerPath} to ${proxyBalancerPath}"
			sudo cp -f ${origProxyBalancerPath}/${proxyBalancerConf} ${proxyBalancerPath} || error_exit $? "Failed on: sudo cp -f ${origProxyBalancerPath}/${proxyBalancerConf} ${proxyBalancerPath}"
			
			echo "Replacing PATH-TO-APP with ${applicationName} in ${proxyBalancerPath}"
			sudo sed -i -e "s/PATH-TO-APP/${applicationName}/g" ${proxyBalancerPath} || error_exit $? "Failed on: sudo sed -i -e PATH-TO-APP/${applicationName} ${proxyBalancerPath}"
			
			if [ "${useStickysession}" == "true" ] ; then
				echo "Replacing STICKYSESSION_PLACE_HOLDER with JSESSIONID in ${proxyBalancerPath}"
				sudo sed -i -e "s/STICKYSESSION_PLACE_HOLDER/stickysession=JSESSIONID\|jsessionid nofailover=Off/g" ${proxyBalancerPath} || error_exit $? "Failed on: useStickysession sudo sed -i -e STICKYSESSION_PLACE_HOLDER ${proxyBalancerPath}"
			else
				echo "Replacing STICKYSESSION_PLACE_HOLDER with nothing in ${proxyBalancerPath}"
				sudo sed -i -e "s/STICKYSESSION_PLACE_HOLDER//g" ${proxyBalancerPath} || error_exit $? "Failed on: without useStickysession sudo sed -i -e STICKYSESSION_PLACE_HOLDER ${proxyBalancerPath}"
			fi
			
			echo "Chmodding -R 777 $i"
			sudo chmod -R 777 $i
									
			echo "Writing ${proxyBalancerPath} location to ${serviceDirectory}/proxyBalancerPath ..."
			sudo echo "pathToBalancerConf=\"${proxyBalancerPath}\"" > ${serviceDirectory}/proxyBalancerPath || error_exit $? "Failed on: sudo echo to ${serviceDirectory}/proxyBalancerPath"
			sudo chmod 777 ${serviceDirectory}/proxyBalancerPath || error_exit $? "Failed on: 	sudo chmod 777 ${serviceDirectory}/proxyBalancerPath "	
			exit 0
		fi	
    fi	
done
