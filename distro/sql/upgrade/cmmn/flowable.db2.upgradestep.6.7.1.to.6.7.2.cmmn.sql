
UPDATE ".ACT_CMMN_DATABASECHANGELOGLOCK SET LOCKED = 1, LOCKEDBY = '192.168.68.111 (192.168.68.111)', LOCKGRANTED = TIMESTAMP('2021-12-28 10:48:40.624') WHERE ID = 1 AND LOCKED = 0;

UPDATE ".ACT_CMMN_DATABASECHANGELOGLOCK SET LOCKED = 0, LOCKEDBY = NULL, LOCKGRANTED = NULL WHERE ID = 1;

