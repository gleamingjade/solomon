-- Give source connetor the authorization so that he can derive the bin logs.

CREATE USER 'debezium'@'%' IDENTIFIED BY 'dbz';

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT
ON *.* TO 'debezium'@'%';

FLUSH PRIVILEGES;