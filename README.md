![Alien4Cloud](https://raw.githubusercontent.com/alien4cloud/alien4cloud.github.io/sources/images/alien4cloud-banner.png)

[Website](http://alien4cloud.github.io) |
[Community](http://alien4cloud.github.io/community/index.html) |
[Roadmap](http://alien4cloud.github.io/roadmap/index.html) |
[Documentation](http://alien4cloud.github.io/#/documentation/1.1.0/index.html) |
[Twitter](https://twitter.com/alien4cloud) |
[Release notes](http://alien4cloud.github.io/#/release_notes/index.html)


ALIEN 4 Cloud stands for Application LIfecycle ENablement for Cloud.

FastConnect started this project in order to help enterprises adopting the cloud for their new and existing applications in an Open way. A4C has an Open-Source model (Apache 2 License) and standardization support in mind.

## Building Alien 4 Cloud

Alien 4 Cloud is written in java for the backend and requires a JDK 8 or newer (note that we test it using JDK 8 only for now).

- make sure you have a JDK 8 installed
- make sure you have Maven installed (team is using 3.0.5)
- install Ruby
- install Python
- install Node.js to get npm command. Check here http://nodejs.org. Note that you need a recent version of npm (2.7.4) in order to build a4c.
- install bower  
```sh
$ sudo npm install -g bower
```
- install grunt  
```sh
$ sudo npm -g install grunt-cli
```
- install compass  
```sh
$ gem install compass
```
- and grunt-contrib-compass  
```sh
$ npm install grunt-contrib-compass --save-dev
```  

run the folowing command to build the project:  
```sh
$ mvn clean install -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true
```
