#!/bin/sh
PRG="$0"

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

if [ ! -f "$APP_PID" ]; then
        echo 'no pid file found.'
else
        PID=`cat $APP_PID`
        echo "kill process $PID"
        rm -rf $APP_PID
        kill -9 $PID
fi
