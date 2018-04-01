#!/bin/sh

# chkconfig: 45 98 02

#see man chkconfig

# Setup variables
EXEC=$(which jsvc)
SCRIPT=$(readlink -f "$0")
WORKDIR=$(dirname "$SCRIPT")
CLASS_PATH="$WORKDIR/simple-todo-list-server-assembly-0.1.jar"
#CLASS_PATH="$WORKDIR/simple-todo-list-server.jar"
CLASS=simpletodolist.server.Main
USER=foo

PID="$WORKDIR/simple-todo-list-server.pid"
LOG_DIR=$WORKDIR
#PID="/var/run/simple-todo-list-server.pid"
#LOG_DIR="/var/log/simple-todo-list-server"

LOG_OUT="$LOG_DIR/simple-todo-list-server.log"
LOG_ERR="$LOG_DIR/simple-todo-list-server.err"

JAVA_HOME="$(dirname $(dirname $(dirname $(readlink -f $(which java)))))"
LISTENING_INTERFACE=localhost
LISTENING_PORT=8080

do_exec()
{
    $EXEC -java-home $JAVA_HOME -cp $CLASS_PATH \
          -Dlistening.interface=$LISTENING_INTERFACE -Dlistening.port=$LISTENING_PORT \
          -outfile $LOG_OUT -errfile $LOG_ERR -pidfile $PID $1 $CLASS
}

case "$1" in
    start)
        do_exec
            ;;
    stop)
        do_exec "-stop"
            ;;
    restart)
        if [ -f "$PID" ]; then
            do_exec "-stop"
            do_exec
        else
            echo "service not running, will do nothing"
            exit 1
        fi
            ;;
    *)
            echo "usage: simple-todo-list-server {start|stop|restart}" >&2
            exit 3
            ;;
esac