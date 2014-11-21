#!/bin/bash

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

# The existence of the usingAptGet file in the ext folder will later serve as a flag that "we" are on Ubuntu or Debian or Mint
echo "Using apt-get. Updating apt-get on one of the following : Ubuntu, Debian, Mint" > usingAptGet
sudo apt-get -y -q update || error_exit $? "Failed on: sudo apt-get -y update"
# Removing previous apache2 installation if exist
sudo apt-get -y -q purge apache2* || error_exit $? "Failed on: sudo apt-get -y -q purge apache2*"

# The following statements are used since in some cases, there are leftovers after uninstall
sudo rm -rf /etc/apache2 || error_exit $? "Failed on: sudo rm -rf /etc/apache2"
sudo rm -rf /usr/sbin/apache2 || error_exit $? "Failed on: sudo rm -rf /usr/sbin/apache2"
sudo rm -rf /usr/lib/apache2 || error_exit $? "Failed on: sudo rm -rf /usr/lib/apache2"
sudo rm -rf /usr/share/apache2 || error_exit $? "Failed on: sudo rm -rf /usr/share/apache2"

echo "Using apt-get. Installing apache2 on one of the following : Ubuntu, Debian, Mint"
sudo apt-get install -y -q apache2 || error_exit $? "Failed on: sudo apt-get install -y -q apache2"

#sudo /etc/init.d/apache2 stop
# Just in case the above doesn't work
echo "installOnUbuntu.sh: Looking for apache2 to kill them..."
ps -ef | grep -iE "apache2" | grep -vi grep
if [ $? -eq 0 ] ; then 
  echo "installOnUbuntu.sh: About to kill apache2"
  ps -ef | grep -iE "apache2" | grep -vi grep | awk '{print $2}' | xargs sudo kill -9
fi  


echo "end of installOnUbuntu.sh"
