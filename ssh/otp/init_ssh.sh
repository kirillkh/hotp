#!/bin/bash

#Set these 3 variables to correct values
REMOTE_USER=user
HOST=host
LOCAL_PORT=1024

#Where the log will be dumped. $$ is the PID of the script. Use /dev/null to disable logging.
#LOG=/dev/null
LOG=/tmp/otp_log_$$.txt

#0 - disable all info, 1 - basic info, 2 - detailed info
DEBUG=1

#You probably don't want to touch these
SCRIPTS_DIR=`dirname $0`
HOTP="$SCRIPTS_DIR/hotp.jar"
EXPECT_SCRIPT="$SCRIPTS_DIR/otp.exp"
LOG_SCRIPT="$SCRIPTS_DIR/log.sh"


#------------------------------------------------------
info() {
  [ $DEBUG == 1 -o $DEBUG == 2  ] && echo $1 >&2
  echo $1 >> $LOG
}

[ -e $LOG ] || (touch $LOG && chmod 600 $LOG)

[ $DEBUG == 2 ] && exec 3>&2  || exec 3>/dev/null

info "init_ssh: starting"
nc -z localhost $LOCAL_PORT 1>&3 2>&3

#in case of non-zero exit status of nc, the tunnel is closed: open it first
if [ $? != 0 ]; then
  info "init_ssh: opening tunnel"
  "$EXPECT_SCRIPT" "$REMOTE_USER" "$HOST" "$LOCAL_PORT" "$HOTP" "$LOG_SCRIPT $LOG" 1>&3 2>&3
fi

info "init_ssh: starting nc"
"$LOG_SCRIPT" "$LOG" "nc localhost $LOCAL_PORT" 2>&3
info "init_ssh: finished"
