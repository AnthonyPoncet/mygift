#!/bin/sh

nohup java -jar auth-server-0.1-all.jar > auth-server.log 2>&1 &
echo $! > saved_pid.txt

nohup java -jar back-0.1-all.jar > main-server.log 2>&1 &
echo $! >> saved_pid.txt
