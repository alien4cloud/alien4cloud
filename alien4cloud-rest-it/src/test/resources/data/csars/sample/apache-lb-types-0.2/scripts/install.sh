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

# The existence of the usingYum file in the ext folder will later serve as a flag that "we" are on Red Hat or CentOS or Fedora or Amazon
echo "Using yum. Updating yum on one of the following : Red Hat, CentOS, Fedora, Amazon. " > usingYum
sudo yum -y -q update || error_exit $? "Failed on: sudo yum -y -q update"
# Removing previous httpd installation if exist
sudo yum remove -y -q httpd || error_exit $? "Failed on: sudo yum remove -y -q httpd"

# The following two statements are used since in some cases, there are leftovers after uninstall
sudo rm -rf /etc/httpd || error_exit $? "Failed on: sudo rm -rf /etc/httpd"
sudo rm -rf /usr/sbin/httpd || error_exit $? "Failed on: sudo rm -rf /usr/sbin/httpd"

echo "Using yum. Installing httpd on one of the following : Red Hat, CentOS, Fedora, Amazon"
sudo yum install -y -q httpd || error_exit $? "Failed on: sudo yum install -y -q httpd"

ps -ef | grep -iE "httpd" | grep -vi grep
if [ $? -eq 0 ] ; then 
  ps -ef | grep -iE "httpd" | grep -vi grep | awk '{print $2}' | xargs sudo kill -9
fi  


