#!/bin/sh
echo "Installing tomcat"
apt-get update
echo "Installing unzip"
apt-get install unzip
echo "Getting tomcat"
wget -q -O tomcat.zip http://repository.cloudifysource.org/org/apache/tomcat/7.0.23/apache-tomcat-7.0.23.zip
mkdir tomcat
echo "Unzipping tomcat"
unzip tomcat.zip -d tomcat
mv tomcat/*/* tomcat
chmod -R 755 tomcat
echo "Tomcat has been installed succesfully"