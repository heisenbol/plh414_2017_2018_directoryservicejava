# plh414_2017_2018_directoryservicejava
plh414 2017/2018 Directory Service Sample Java

Dumps status of available FS's, Auth Servers etc from Zookeeper

Retrieves and caches a list of available FS's from Zookeeper and watches for new FS's / FS removals


Needs external servlet-api.jar to compile

For deployment, needs file WEB-INF/config.properties

zookeeper.jar can be retrieved from /usr/share/java/zookeeper.jar and has to be put in the lib folder

log4j-1.2.jar from /usr/share/java/log4j-1.2.jar

slf4j-api.jar from /usr/share/java/slf4j-api.jar





Sample deployment script

```
#!/bin/bash

if [ "$#" -ne 7 ]; then
    echo "Illegal number of parameters"
    echo "deploy.sh ZOOKEEPER_HOST ZOOKEEPER_USERNAME ZOOKEEPER_PASSWORD  SERVERHOSTNAME SERVER_PORT SERVER_SCHEME CONTEXT"
    echo "e.g. ../deploy.sh  snf-814985.vm.okeanos.grnet.gr username password"
    exit -1
fi
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo $DIR

echo ZOOKEEPER_HOST=$1 >> "$DIR/WEB-INF/config.properties"
echo ZOOKEEPER_USER=$2 >> "$DIR/WEB-INF/config.properties"
echo ZOOKEEPER_PASSWORD=$3 >> "$DIR/WEB-INF/config.properties"
echo SERVERHOSTNAME=$4 >> "$DIR/WEB-INF/config.properties"
echo SERVER_PORT=$5 >> "$DIR/WEB-INF/config.properties"
echo SERVER_SCHEME=$6 >> "$DIR/WEB-INF/config.properties"
echo CONTEXT=$7 >> "$DIR/WEB-INF/config.properties"

cd $DIR/WEB-INF \
&& javac -Xlint:deprecation -Xlint:unchecked -cp /home/sk/Documents/isc/katanemimena2018/servlet-api.jar:lib/*:classes -d classes src/tuc/sk/*.java \
&& rsync -a -v --exclude deploy.sh --exclude .gitignore --exclude .git --delete $DIR/ root@$4:/var/lib/tomcat8/webapps/directoryservicejava \
&& ssh root@$4 "chown -R tomcat8:tomcat8 /var/lib/tomcat8/webapps"
cd $DIR
#http://snf-814985.vm.okeanos.grnet.gr:8080/directoryservicejava/status
```
