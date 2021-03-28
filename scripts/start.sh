#!/bin/sh

nohup java -Dlogback.configurationFile=logback/logback.xml -jar auth-server-0.1-all.jar > auth-server.log 2>&1 &
echo $! > saved_pid.txt

nohup java -Dlogback.configurationFile=logback/logback.xml -jar back-0.1-all.jar > main-server.log 2>&1 &
echo $! >> saved_pid.txt
