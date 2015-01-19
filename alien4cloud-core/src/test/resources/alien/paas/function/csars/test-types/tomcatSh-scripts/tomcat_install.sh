#!/bin/sh

apt-get update
apt-get install unzip
wget -q -O tomcat.zip http://repository.cloudifysource.org/org/apache/tomcat/7.0.23/apache-tomcat-7.0.23.zip
mkdir tomcat
unzip tomcat.zip -d tomcat
mv tomcat/*/* tomcat
chmod -R 755 tomcat