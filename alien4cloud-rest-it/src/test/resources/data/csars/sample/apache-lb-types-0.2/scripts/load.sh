#!/bin/bash

# args:
# $1 Number or requests
# $2 Concurrency
# $3 URL
#

requests=$1
concurrency=$2
currentUrl=$3

# args:
# $1 the error code of the last command (should be explicitly passed)
# $2 the message to print in case of an error
# 
# an error message is printed and the script exists with the provided error code
function error_exit {
	echo "$2 : error code: $1"
	exit ${1}
}

export PATH=$PATH:/usr/bin:/usr/sbin:/sbin || error_exit $? "Failed on: export PATH=$PATH:/usr/sbin:/sbin"

sudo `which ab` -v INFO -n ${requests} -c ${concurrency} ${currentUrl} || error_exit $? "Failed on: sudo which ab -v INFO -n ${requests} -c ${concurrency} ${currentUrl}"

