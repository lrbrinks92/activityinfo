#!/bin/sh

mysqldump --skip-extended-insert --no-create-db --no-create-info --quote-names=false --complete-insert \
            -uroot -proot activityinfo country adminlevel adminentity > store/mysql/geography.sql 
