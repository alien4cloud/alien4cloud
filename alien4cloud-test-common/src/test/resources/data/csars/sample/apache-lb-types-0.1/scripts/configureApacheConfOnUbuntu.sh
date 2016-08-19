#!/bin/bash

# args:
# $1 The original port in the apache2.conf (ususally 80)
# $2 The required port in the apache2.conf
# $3 Full path of the proxy_balancer.conf in the recipe (context.serviceDirectory/overrides-linux/conf/extra)
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

apache2Location=`whereis apache2`
for i in ${apache2Location}
do    
	if [ -d "$i" ] ; then
		portsConf="$i/ports.conf"		
		if [ -f "${portsConf}" ] ; then
			echo "portsConf is in ${portsConf}"					
			echo "Replacing $origPort with $newPort..."
			sudo sed -i -e "s/$origPort/$newPort/g" ${portsConf} || error_exit $? "Failed on: sudo sed -i -e $origPort/$newPort in ${portsConf}"			
			echo "End of ${portsConf} replacements"
			
			defaultFile="$i/sites-available/default"			
			sudo sed -i -e "s/$origPort/$newPort/g" ${defaultFile} || error_exit $? "Failed on: sudo sed -i -e $origPort/$newPort in ${defaultFile}"	
  
			
			pushd $i/mods-enabled || error_exit $? "Failed on: sudo sudo pushd $i/mods-enabled"
			sudo ln -f -s ../mods-available/proxy_balancer.conf proxy_balancer.conf || error_exit $? "Failed on: sudo ln -f -s ../mods-available/proxy_balancer.conf proxy_balancer.conf"
			sudo ln -f -s ../mods-available/proxy_balancer.load proxy_balancer.load || error_exit $? "Failed on: sudo ln -f -s ../mods-available/proxy_balancer.load proxy_balancer.load"
			sudo ln -f -s ../mods-available/proxy.conf proxy.conf || error_exit $? "Failed on: sudo ln -f -s ../mods-available/proxy.conf proxy.conf"
			sudo ln -f -s ../mods-available/proxy.load proxy.load || error_exit $? "Failed on: sudo ln -f -s ../mods-available/proxy.load proxy.load"
			sudo ln -f -s ../mods-available/proxy_http.load || error_exit $? "Failed on: sudo ln -f -s ../mods-available/proxy_http.load"
			popd || error_exit $? "Failed on: sudo popd"
			
			
			proxyBalancerConf="proxy_balancer.conf"
			proxyBalancerPath="$i/mods-enabled/${proxyBalancerConf}"
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
