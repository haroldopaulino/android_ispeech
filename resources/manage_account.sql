/*
call HAROLD.MANAGE_ACCOUNTS('DEPOSIT', 'SAVINGS', 111, 50, '', 0, 0, 'SAVING FOR LIFE', '');
call HAROLD.MANAGE_ACCOUNTS('TRANSFER', 'SAVINGS', 111, 5, 'CHECKING', 123, 5, 'GREAT TRANSFER', '');
call HAROLD.MANAGE_ACCOUNTS('WITHDRAW', 'CHECKING', 111, 100, '', 0, 0, 'NEED CASH TO GO TO THE MOVIES', '');

--SELECT * FROM ACC_TRAN;
--SELECT * FROM CHECKING;
--SELECT * FROM SAVINGS;
--DELETE FROM ACC_TRAN;
*/
CREATE OR REPLACE PROCEDURE HAROLD.MANAGE_ACCOUNTS ( 
	IN INPUT_TRANSACTION_TYPE VARCHAR(20) CCSID 37 , 
	IN INPUT_ACCOUNT_TYPE1 VARCHAR(20) , 
	IN INPUT_ACCOUNT1 DECIMAL(5, 0) , 
	IN INPUT_AMOUNT1 DECIMAL(5, 2) , 
	IN INPUT_ACCOUNT_TYPE2 VARCHAR(20) , 
	IN INPUT_ACCOUNT2 DECIMAL(5, 0) , 
	IN INPUT_AMOUNT2 DECIMAL(5, 2) ,
	IN INPUT_NOTES VARCHAR(1000) ,
	OUT JOB_RESULT VARCHAR(1000) CCSID 37 ) 
	LANGUAGE SQL
    --DYNAMIC RESULT SETS 1
	SPECIFIC HAROLD.MANAGE_ACCOUNTS 
	NOT DETERMINISTIC 
	MODIFIES SQL DATA 
	CALLED ON NULL INPUT 
	COMMIT ON RETURN YES 
	NEW SAVEPOINT LEVEL 
	CONCURRENT ACCESS RESOLUTION USE CURRENTLY COMMITTED 
	SET OPTION  ALWBLK = *ALLREAD , 
	ALWCPYDTA = *OPTIMIZE , 
	COMMIT = *NONE , 
	DECRESULT = (31, 31, 00) , 
	DFTRDBCOL = *NONE , 
	DYNDFTCOL = *NO , 
	DYNUSRPRF = *USER , 
	SRTSEQ = *HEX   
	BEGIN 
  
DECLARE IS_VALID_ACCOUNT_1 INT; 
DECLARE IS_VALID_ACCOUNT_2 INT;
DECLARE ACCOUNT1_RECORD_COUNT INT;
DECLARE ACCOUNT2_RECORD_COUNT INT;
DECLARE RETURN_VALUE VARCHAR ( 1000 ) CCSID 37 ; 
DECLARE TMP_STRING VARCHAR ( 1000 ) CCSID 37 ;

SET TMP_STRING = '';

IF (INPUT_ACCOUNT1 <> 0)
	THEN
        IF (TRIM(INPUT_ACCOUNT_TYPE1) = 'SAVINGS')
            THEN
                SET ACCOUNT1_RECORD_COUNT = (SELECT COUNT(*) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1 FETCH FIRST 1 ROW ONLY);
                IF (ACCOUNT1_RECORD_COUNT = 1)
                    THEN
                        SET IS_VALID_ACCOUNT_1 = 1;
                    ELSE
                        SET IS_VALID_ACCOUNT_1 = 0;
                END IF;
            ELSE
                SET ACCOUNT1_RECORD_COUNT = (SELECT COUNT(*) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1 FETCH FIRST 1 ROW ONLY);
                IF (ACCOUNT1_RECORD_COUNT = 1)
                    THEN
                        SET IS_VALID_ACCOUNT_1 = 1;
                    ELSE
                        SET IS_VALID_ACCOUNT_1 = 0;
                END IF;
         END IF;
	ELSE
		SET IS_VALID_ACCOUNT_1 = 0;
END IF ;

IF (INPUT_ACCOUNT2 <> 0)
	THEN
        IF (TRIM(INPUT_ACCOUNT_TYPE2) = 'SAVINGS')
            THEN
                SET ACCOUNT2_RECORD_COUNT = (SELECT COUNT(*) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT2 FETCH FIRST 1 ROW ONLY);
                IF (ACCOUNT2_RECORD_COUNT = 1)
                    THEN
                        SET IS_VALID_ACCOUNT_2 = 1;
                    ELSE
                        SET IS_VALID_ACCOUNT_2 = 0;
                END IF;
            ELSE
                SET ACCOUNT2_RECORD_COUNT = (SELECT COUNT(*) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT2 FETCH FIRST 1 ROW ONLY);
                IF (ACCOUNT2_RECORD_COUNT = 1)
                    THEN
                        SET IS_VALID_ACCOUNT_2 = 1;
                    ELSE
                        SET IS_VALID_ACCOUNT_2 = 0;
                END IF;
         END IF;
	ELSE
		SET IS_VALID_ACCOUNT_2 = 0;
END IF ;  

IF (IS_VALID_ACCOUNT_1 = 1 AND IS_VALID_ACCOUNT_2 = 1)
    THEN
        INSERT INTO ACC_TRAN 
			(TRANTYPE,
			ACCTYPE1,
			ACCOUNT1,
			AMOUNT1,
			ACCTYPE2,
			ACCOUNT2,
			AMOUNT2,
		    NOTES) 
		VALUES 
			(INPUT_TRANSACTION_TYPE,
			INPUT_ACCOUNT_TYPE1,
			INPUT_ACCOUNT1,
			INPUT_AMOUNT1,
			INPUT_ACCOUNT_TYPE2,
			INPUT_ACCOUNT2,
			INPUT_AMOUNT2,
		    INPUT_NOTES) ;

	IF ( TRIM ( INPUT_TRANSACTION_TYPE ) = 'TRANSFER' ) 
		THEN
            IF (TRIM(INPUT_ACCOUNT_TYPE1) = 'SAVINGS')
				THEN
                    SET TMP_STRING = '"INITIAL SAVINGS BALANCE":"$' || (SELECT CHAR(BALANCE) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
                    UPDATE SAVINGS SET BALANCE = BALANCE - INPUT_AMOUNT1 WHERE ACCOUNT = INPUT_ACCOUNT1;
                    SET TMP_STRING = TMP_STRING || ',"FINAL SAVINGS BALANCE":"$' || (SELECT CHAR(BALANCE) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
                    SET TMP_STRING = TMP_STRING || ',"INITIAL CHECKING BALANCE":"$' || (SELECT CHAR(BALANCE) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
                    UPDATE CHECKING SET BALANCE = BALANCE + INPUT_AMOUNT2 WHERE ACCOUNT = INPUT_ACCOUNT2;
                    SET TMP_STRING = TMP_STRING || ',"FINAL CHECKING BALANCE":"$' || (SELECT CHAR(BALANCE) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
				ELSE
					SET TMP_STRING = '"INITIAL CHECKING BALANCE":"$' || (SELECT CHAR(BALANCE) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
					UPDATE CHECKING SET BALANCE = BALANCE - INPUT_AMOUNT1 WHERE ACCOUNT = INPUT_ACCOUNT1;
					SET TMP_STRING = TMP_STRING || ',"FINAL CHECKING BALANCE":"$' || (SELECT CHAR(BALANCE) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
                    SET TMP_STRING = TMP_STRING || ',"INITIAL SAVINGS BALANCE":"$' || (SELECT CHAR(BALANCE) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
					UPDATE SAVINGS SET BALANCE = BALANCE + INPUT_AMOUNT2 WHERE ACCOUNT = INPUT_ACCOUNT2;
					SET TMP_STRING = TMP_STRING || ',"FINAL SAVINGS BALANCE":"$' || (SELECT CHAR(BALANCE) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
			END IF ;
            SET RETURN_VALUE = '{"result": "SUCCESS", "short_message": "Transfer completed!", "long_message": "The Transfer of $' || INPUT_AMOUNT1 || ' from the account ' ||
                                 INPUT_ACCOUNT1 || ' (' || INPUT_ACCOUNT_TYPE1 || '), to the account ' ||
                                 INPUT_ACCOUNT2 || ' (' || INPUT_ACCOUNT_TYPE2 || ') has completed successfully!" ' || '}' ; 
	END IF ; 
END IF;

IF (IS_VALID_ACCOUNT_1 = 1 AND IS_VALID_ACCOUNT_2 = 0)
    THEN
        INSERT INTO ACC_TRAN 
			(TRANTYPE,
			ACCTYPE1,
			ACCOUNT1,
			AMOUNT1,
			ACCTYPE2,
			ACCOUNT2,
			AMOUNT2,
		    NOTES) 
		VALUES 
			(INPUT_TRANSACTION_TYPE,
			INPUT_ACCOUNT_TYPE1,
			INPUT_ACCOUNT1,
			INPUT_AMOUNT1,
			INPUT_ACCOUNT_TYPE2,
			INPUT_ACCOUNT2,
			INPUT_AMOUNT2,
		    INPUT_NOTES) ;

    IF ( TRIM ( INPUT_TRANSACTION_TYPE ) = 'BALANCE' )
		THEN
            IF (TRIM(INPUT_ACCOUNT_TYPE1) = 'SAVINGS')
				THEN
					SET TMP_STRING = '$' || (SELECT CHAR(BALANCE) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1);
				ELSE
                    SET TMP_STRING = '$' || (SELECT CHAR(BALANCE) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1);
			END IF ;
            SET RETURN_VALUE = '{"result": "SUCCESS", "short_message": "' || TMP_STRING || '", ' ||
                                '"long_message": "The current balance for the account ' || INPUT_ACCOUNT1 || ' (' || INPUT_ACCOUNT_TYPE1 || ') is ' || TMP_STRING || '"}' ;
	END IF ;

	IF ( TRIM ( INPUT_TRANSACTION_TYPE ) = 'DEPOSIT' )
		THEN
            IF (TRIM(INPUT_ACCOUNT_TYPE1) = 'SAVINGS')
				THEN
					SET TMP_STRING = '"INITIAL SAVINGS BALANCE":"$' || (SELECT CHAR(BALANCE) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
                    UPDATE SAVINGS SET BALANCE = BALANCE + INPUT_AMOUNT1 WHERE ACCOUNT = INPUT_ACCOUNT1;
                    SET TMP_STRING = TMP_STRING || ',"FINAL SAVINGS BALANCE":"$' || (SELECT CHAR(BALANCE) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
				ELSE
                    SET TMP_STRING = '"INITIAL CHECKING BALANCE":"$' || (SELECT CHAR(BALANCE) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
					UPDATE CHECKING SET BALANCE = BALANCE + INPUT_AMOUNT1 WHERE ACCOUNT = INPUT_ACCOUNT1;
                    SET TMP_STRING = TMP_STRING || ',"FINAL CHECKING BALANCE":"$' || (SELECT CHAR(BALANCE) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
			END IF ;
            SET RETURN_VALUE = '{"result": "SUCCESS", "short_message": "' || LOWER(INPUT_TRANSACTION_TYPE) || ' of $' || INPUT_AMOUNT1 || ' is done!", "long_message": "The Deposit to the account ' ||
                                 INPUT_ACCOUNT1 || ' (' || INPUT_ACCOUNT_TYPE1 || ') has completed successfully!", ' || TMP_STRING || '}' ;
	END IF ;
	
	IF ( TRIM ( INPUT_TRANSACTION_TYPE ) = 'WITHDRAW' ) 
		THEN
            IF (TRIM(INPUT_ACCOUNT_TYPE1) = 'SAVINGS')
				THEN
                    SET TMP_STRING = '"INITIAL SAVINGS BALANCE":"$' || (SELECT CHAR(BALANCE) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
					UPDATE SAVINGS SET BALANCE = BALANCE - INPUT_AMOUNT1 WHERE ACCOUNT = INPUT_ACCOUNT1;
                    SET TMP_STRING = TMP_STRING || ',"FINAL SAVINGS BALANCE":"$' || (SELECT CHAR(BALANCE) FROM SAVINGS WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
				ELSE
                    SET TMP_STRING = '"INITIAL CHECKING BALANCE":"$' || (SELECT CHAR(BALANCE) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
					UPDATE CHECKING SET BALANCE = BALANCE - INPUT_AMOUNT1 WHERE ACCOUNT = INPUT_ACCOUNT1;
                    SET TMP_STRING = TMP_STRING || ',"FINAL CHECKING BALANCE":"$' || (SELECT CHAR(BALANCE) FROM CHECKING WHERE ACCOUNT = INPUT_ACCOUNT1) || '"';
			END IF ;
            SET RETURN_VALUE = '{"result": "SUCCESS", "short_message": "' || LOWER(INPUT_TRANSACTION_TYPE) || ' of $' || INPUT_AMOUNT1 || ' is done!", "long_message": "The Withdraw from the account ' ||
                                 INPUT_ACCOUNT1 || ' (' || INPUT_ACCOUNT_TYPE1 || ') has completed successfully!", ' || TMP_STRING || '}' ;
	END IF ;
END IF;


COMMIT ; 
SET JOB_RESULT = RETURN_VALUE ; 
END  ; 
  
GRANT ALTER , EXECUTE   
ON SPECIFIC PROCEDURE HAROLD.MANAGE_ACCOUNTS 
TO HAROLD ; 
  
GRANT EXECUTE   
ON SPECIFIC PROCEDURE HAROLD.MANAGE_ACCOUNTS 
TO PUBLIC ;