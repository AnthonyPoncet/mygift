#!/bin/bash

cp mygift.db backup_db/$1.db
tar -zcvf uploads_$1.tar.gz uploads/
mv uploads_$1.tar.gz backup_db/
