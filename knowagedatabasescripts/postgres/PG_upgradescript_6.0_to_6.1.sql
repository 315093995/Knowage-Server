﻿ALTER TABLE SBI_KPI_KPI ALTER DEFINITION TYPE VARCHAR(4000);
ALTER TABLE SBI_KPI_VALUE ALTER LOGICAL_KEY TYPE VARCHAR(4000);

ALTER TABLE SBI_META_MODELS add  Foreign Key (DATA_SOURCE_ID) references SBI_DATA_SOURCE (DS_ID);

delete from SBI_PRODUCT_TYPE_ENGINE where ENGINE_ID in(select ENGINE_ID from SBI_ENGINES where NAME='Mobile Chart Engine');
delete from SBI_PRODUCT_TYPE_ENGINE where ENGINE_ID in(select ENGINE_ID from SBI_ENGINES where NAME='Mobile Cockpit Engine');
delete from SBI_PRODUCT_TYPE_ENGINE where ENGINE_ID in(select ENGINE_ID from SBI_ENGINES where NAME='Mobile Report Engine');
delete from SBI_ENGINES where NAME='Mobile Chart Engine';
delete from SBI_ENGINES where NAME='Mobile Cockpit Engine';
delete from SBI_ENGINES where NAME='Mobile Report Engine';
delete from SBI_DOMAINS where VALUE_CD='MOBILE_CHART';
delete from SBI_DOMAINS where VALUE_CD='MOBILE_COCKPIT';
delete from SBI_DOMAINS where VALUE_CD='MOBILE_REPORT';
commit;

CREATE TABLE  SBI_DOSSIER_ACTIVITY(
		ID 					INTEGER NOT NULL,
		PROGRESS 			INTEGER NOT NULL,
		PPT					BYTEA,
	    DOCUMENT_ID 		INTEGER,
	    ACTIVITY 			VARCHAR(45) NOT NULL,
	    PARAMS 				VARCHAR(4000),
	 USER_IN VARCHAR(100) NULL DEFAULT NULL,
	 USER_UP VARCHAR(100) NULL DEFAULT NULL,
	 USER_DE VARCHAR(100) NULL DEFAULT NULL,
	 TIME_IN TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
	 TIME_UP TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
	 TIME_DE TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
	 SBI_VERSION_IN VARCHAR(10) NULL DEFAULT NULL,
	 SBI_VERSION_UP VARCHAR(10) NULL DEFAULT NULL,
	 SBI_VERSION_DE VARCHAR(10) NULL DEFAULT NULL,
	 ORGANIZATION VARCHAR(20) NULL DEFAULT NULL,
	    PRIMARY KEY (ID)
) WITHOUT OIDS;

ALTER TABLE SBI_DOSSIER_ACTIVITY ADD CONSTRAINT FK_SBI_PROGRESS_THREAD	FOREIGN KEY (PROGRESS) 	REFERENCES SBI_PROGRESS_THREAD(PROGRESS_THREAD_ID)			ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE SBI_DOSSIER_ACTIVITY ADD CONSTRAINT FK_SBI_OBJECTS FOREIGN KEY  (DOCUMENT_ID) REFERENCES SBI_OBJECTS (BIOBJ_ID) ON DELETE CASCADE ON UPDATE CASCADE;

UPDATE SBI_CONFIG set VALUE_CHECK = 'user' where LABEL = 'SPAGOBI.SECURITY.DEFAULT_ROLE_ON_SIGNUP';