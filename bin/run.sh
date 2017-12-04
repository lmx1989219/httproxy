#!/bin/sh
PRG="$0"

#JAVA_HOME=/usr/bin/java
APP_HOME=.
CLASSPATH=$APP_HOME

for i in "$APP_HOME"/lib/*.jar; do
   CLASSPATH="$CLASSPATH":"$i"
done

#DEBUG="-Xms128m -Xmx512m -Xdebug -Xrunjdwp:transport=dt_socket,address=4380,server=y,suspend=n"
JVM_ARGS="-server -Xms1024m -Xmx1024m -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+UseCMSCompactAtFullCollection -XX:+HeapDumpOnOutOfMemoryError"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`


[ -z "$APP_HOME" ] && APP_HOME=`cd "$PRGDIR" >/dev/null; pwd`
[ -z "$APP_PID" ] && APP_PID=$APP_HOME/pid


echo $CLASSPATH


if [ ! -f "$APP_PID" ]; then
        java -classpath ${CLASSPATH} -Dproxy.port=8888 com.lmx.httproxy.ProxyServer 2>&1 &
        PID=$!
        echo $PID > $APP_PID
else
        PID=`cat $APP_PID`
        echo "server is running with pid $PID."
fi