#!/bin/bash
echo "init_sshfs: starting" >&2

# work around a sshfs bug, where it forgets to set home to the correct value
REAL_HOME=`sudo -H -u $USER sh -c 'echo $HOME'`
CONF=$REAL_HOME/.hotp.conf

if [[ ! -w "$CONF" || ! -r "$CONF" ]]; then
  echo "init_sshfs: ERROR: the conf file $CONF doesn't exist or isn't accessible! exiting." >&2
  exit 1
fi

#check that we are not root
if [ $USER == "root" ]; then
  #running under root can still be the intended behavior in case conf exists and is writable
  echo "init_sshfs: warning: running as root is dangerous and might not work" >&2
fi

# same workaround again
sudo -H -u $USER ssh -v $*
