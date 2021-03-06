
UPDATE FLW_EV_DATABASECHANGELOGLOCK SET LOCKED = TRUE, LOCKEDBY = '192.168.68.111 (192.168.68.111)', LOCKGRANTED = '2021-12-24 16:56:42.402' WHERE ID = 1 AND LOCKED = FALSE;

ALTER TABLE FLW_CHANNEL_DEFINITION ADD TYPE_ VARCHAR(255);

ALTER TABLE FLW_CHANNEL_DEFINITION ADD IMPLEMENTATION_ VARCHAR(255);

INSERT INTO FLW_EV_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('2', 'flowable', 'org/flowable/eventregistry/db/liquibase/flowable-eventregistry-db-changelog.xml', NOW(), 2, '8:0ea825feb8e470558f0b5754352b9cda', 'addColumn tableName=FLW_CHANNEL_DEFINITION; addColumn tableName=FLW_CHANNEL_DEFINITION', '', 'EXECUTED', NULL, NULL, '4.3.5', '0361402472');

INSERT INTO FLW_EV_DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('3', 'flowable', 'org/flowable/eventregistry/db/liquibase/flowable-eventregistry-db-changelog.xml', NOW(), 3, '8:3c2bb293350b5cbe6504331980c9dcee', 'customChange', '', 'EXECUTED', NULL, NULL, '4.3.5', '0361402472');

UPDATE FLW_EV_DATABASECHANGELOGLOCK SET LOCKED = FALSE, LOCKEDBY = NULL, LOCKGRANTED = NULL WHERE ID = 1;

