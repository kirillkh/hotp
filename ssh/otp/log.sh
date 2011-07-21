#!/bin/sh
# logs the stderr of the given command to the specified log, but also pipes it to stderr
log="$1"
shift
cmd="$*"
(sh -c "$cmd" 3>&1 1>&2 2>&3 | tee -a "$log") 3>&1 1>&2 2>&3
