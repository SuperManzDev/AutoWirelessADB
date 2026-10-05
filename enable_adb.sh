#!/system/bin/sh
setprop service.adb.tcp.port 5555
PID=$(pidof adbd)
if [ -n "$PID" ]; then
    kill -9 $PID
fi
echo SCRIPT_DONE
