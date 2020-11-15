#!/bin/sh


nohup java -jar auth-server-0.1-SNAPSHOT.jar > auth-server.log 2>&1 &
echo $! > saved_pid.txt

nohup java -jar back-0.1-SNAPSHOT.jar > main-server.log 2>&1 &
echo $! >> saved_pid.txt
