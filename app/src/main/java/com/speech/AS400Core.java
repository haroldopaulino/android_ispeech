package com.speech;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.ibm.as400.access.AS400JDBCCallableStatement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AS400Core extends AsyncTask<String, Void, String> {
    public Connection ibmConnection = null;
    Activity activity;
    String system, username, password, query, connectionParameters = "";
    JSONObject optionalParameters;
    String[] queryParams;
    int[] queryTypes;
    private String runMode = "";
    private String callbackCode = "";
    private boolean connected = false;
    private JSONArray queryResults;
    private int currentSqlFetchRowCounter = 0, callbackSqlFetchRowCounter = 0;
    private boolean runTimedTask = true;
    public ArrayList<String> fileTypes = new ArrayList<>(), columnsNames;
    public ArrayList<Integer> columnsTypes;
    private ScheduledExecutorService scheduler;

    public AS400Core(Activity inputActivity,
                     String systemInput,
                     String usernameInput,
                     String passwordInput) throws SQLException {
        activity = inputActivity;
        system = systemInput;
        username = usernameInput;
        password = passwordInput;

        loadFileTypesCodes();
        startTimedProcess();
    }

    public AS400Core(Activity inputActivity,
                     String systemInput,
                     String usernameInput,
                     String passwordInput,
                     JSONObject optionalParametersInput) throws SQLException {
        activity = inputActivity;
        system = systemInput;
        username = usernameInput;
        password = passwordInput;
        setConnectionParameters(optionalParametersInput);
        loadFileTypesCodes();
        startTimedProcess();
    }

    public AS400Core(Activity inputActivity,
                     String systemInput,
                     String usernameInput,
                     String passwordInput,
                     String callbackCodeInput) throws SQLException {
        activity = inputActivity;
        system = systemInput;
        username = usernameInput;
        password = passwordInput;
        callbackCode = callbackCodeInput;

        loadFileTypesCodes();
        startTimedProcess();
    }

    public AS400Core(Activity inputActivity,
                     String systemInput,
                     String usernameInput,
                     String passwordInput,
                     JSONObject optionalParametersInput,
                     String callbackCodeInput) throws SQLException {
        activity = inputActivity;
        system = systemInput;
        username = usernameInput;
        password = passwordInput;
        callbackCode = callbackCodeInput;
        setConnectionParameters(optionalParametersInput);
        loadFileTypesCodes();
        startTimedProcess();
    }

    public void as400Callback(Boolean connected) {}

    public void as400Callback(JSONObject results) {}

    public void as400Callback(String results) {}

    public void as400Callback(ArrayList<String> columnNames,
                              ArrayList<Integer> columnTypes,
                              JSONArray results,
                              boolean finishedProcessing,
                              String callbackCodeInput) { }

    public void as400Callback(JSONObject results,
                              String callbackCodeInput) { }

    private void startTimedProcess() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate
                (() -> {
                    try {
                        switch(runMode) {
                            case "binded_query" :
                            case "raw_query" :
                                if (currentSqlFetchRowCounter > callbackSqlFetchRowCounter) {
                                    callbackSqlFetchRowCounter = queryResults.length();
                                    as400Callback(columnsNames, columnsTypes, queryResults, (runTimedTask ? false : true), callbackCode);
                                }

                                break;
                        }

                        if (!runTimedTask) {
                            scheduler.shutdown();
                        }
                    } catch (Exception e) {
                    }
                }, 0, 2, TimeUnit.SECONDS);
    }

    public void setConnectionParameters(JSONObject inputConnectionParameters) {
        optionalParameters = inputConnectionParameters;
        Iterator<String> keys = optionalParameters.keys();
        String parameterKey = "";
        connectionParameters = ";";
        while(keys.hasNext()) {
            parameterKey = (String)keys.next();
            try {
                connectionParameters += parameterKey + "=" + optionalParameters.getString(parameterKey) + ";";
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadFileTypesCodes() {
        fileTypes.add("All Objects");
        fileTypes.add("Aliases");
        fileTypes.add("Logical Files");
        fileTypes.add("Materialized query table");
        fileTypes.add("Physical file");
        fileTypes.add("Constraints");
        fileTypes.add("Functions");
        fileTypes.add("Global Variables");
        fileTypes.add("Indexes");
        fileTypes.add("Stored Procedures");
        fileTypes.add("Sequences");
        fileTypes.add("SQL Packages");
        fileTypes.add("Tables");
        fileTypes.add("Triggers");
        fileTypes.add("Types");
        fileTypes.add("Views");
        fileTypes.add("XML Schema Repository");
    }

    @Override
    protected String doInBackground(String... params) {
        String mode = "";
        for (String param : params) {
            mode = param;
        }
        attemptToConnect();
        as400Callback(connected);
        try {
            switch (mode) {
                case "":
                    as400Callback(connected);
                    as400Callback(columnsNames, columnsTypes, queryResults, false, callbackCode);
                    break;
                case "run_query":
                    runQueryBackgroundProcess();
                    break;
                case "run_raw_query":
                    runRawQueryBackgroundProcess();
                    break;
                case "update_query":
                    runUpdateQueryBackgroundProcess();
                    break;
            }
        } catch (SQLException e) {
            final String errorMessage = e.getMessage();
            Handler h = new Handler(Looper.getMainLooper());
            h.post(() -> {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                alertDialogBuilder.setMessage("Error: "+errorMessage);
                alertDialogBuilder.setNegativeButton("OK",
                        (dialog, which) -> {
                            //Intent negativeActivity = new Intent(getApplicationContext(),com.example.alertdialog.NegativeActivity.class);
                            //startActivity(negativeActivity);
                            //finish();
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            });
            e.printStackTrace();
        }

        return "";
    }

    protected void onPreExecute() {

    }

    protected void onPostExecute(String result) {

    }

    public void attemptToConnect() {
        boolean attemptConnection;

        if (!system.equals("") &&
            !username.equals("") &&
            !password.equals("")) {
            attemptConnection = true;
        } else {
            attemptConnection = false;
            if (system.trim().equals("")) {
                Toast.makeText(activity, "Missing System", Toast.LENGTH_LONG).show();
            } else if (username.equals("")) {
                Toast.makeText(activity,"Username", Toast.LENGTH_LONG).show();
            } else if (password.equals("")) {
                Toast.makeText(activity,"Missing Password", Toast.LENGTH_LONG).show();
            }
        }

        if (ibmConnection == null) {
            attemptConnection = true;
        } else {
            try {
                if (ibmConnection.isClosed()) {
                    attemptConnection = true;
                } else {
                    attemptConnection = !ibmConnection.isValid(0);
                }
            } catch (SQLException e) {
                final String errorMessage = e.getMessage();
                Handler h = new Handler(Looper.getMainLooper());
                h.post(() -> {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                    alertDialogBuilder.setMessage("Error: " + errorMessage);
                    alertDialogBuilder.setNegativeButton("OK",
                            (dialog, which) -> {
                                //Intent negativeActivity = new Intent(getApplicationContext(),com.example.alertdialog.NegativeActivity.class);
                                //startActivity(negativeActivity);
                                //finish();
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                });
                ibmConnection = null;
                attemptConnection = true;
            }
        }

        if (attemptConnection) {
            try {
                DriverManager.registerDriver(new com.ibm.as400.access.AS400JDBCDriver());
                Properties p = new Properties();
                p.put("naming","system");
                p.put("progressiveStreaming",1);
                p.put("streamBufferSize",32);
                //p.put("database","");
                p.put("fullyMaterializeLobData",false);
                p.put("enableRowsetSupport",1);
                p.put("block size",0);
                p.put("block criteria",0);
                p.put("date format", "iso");
                p.put("time format", "iso");
                p.put("errors","full");
                p.put("user",username);
                p.put("password",password);
                //p.put("database","s21cf44v");
                p.put("errors","full");
                ibmConnection = DriverManager.getConnection("jdbc:as400://" + system + connectionParameters,p);
                //Log.d("jdbc:as400", "jdbc:as400://" + system + connectionParameters);
                //ibmConnection = DriverManager.getConnection("jdbc:as400://" + system + ";prompt=false;libraries=*LIBL;naming=system", username, password);
                //ibmConnection.setClientInfo("naming","system");
                connected = true;
            } catch (SQLException ex) {
                final String errorMessage = ex.getMessage();
                Handler h = new Handler(Looper.getMainLooper());
                h.post(() -> {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                    alertDialogBuilder.setMessage("Error: "+errorMessage);
                    alertDialogBuilder.setNegativeButton("OK",
                            (dialog, which) -> {
                                //Intent negativeActivity = new Intent(getApplicationContext(),com.example.alertdialog.NegativeActivity.class);
                                //startActivity(negativeActivity);
                                //finish();
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                });
                ibmConnection = null;
                connected = false;
            }
        }
    }

    public void runQuery(String inputQuery, String[] inputQueryParams) throws SQLException {
        query = inputQuery;
        queryParams = inputQueryParams;
        execute("run_query");
    }

    protected void runQueryBackgroundProcess() throws SQLException {
        runMode = "binded_query";
        runTimedTask = true;
        if (connected) {
            queryResults = new JSONArray();
            JSONObject jsonObj;
            PreparedStatement preparedStatement = ibmConnection.prepareStatement(query);
            int tmpCounter = 0;
            for (String value : queryParams) {
                tmpCounter++;
                preparedStatement.setString(tmpCounter, value);
            }
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int numberOfColumns = rsMetaData.getColumnCount();
            columnsNames = new ArrayList<>();
            for (int i = 1; i <= numberOfColumns; i++) {
                columnsNames.add(rsMetaData.getColumnName(i).trim());
                columnsTypes.add(rsMetaData.getColumnType(i));
            }
            currentSqlFetchRowCounter = 0;
            callbackSqlFetchRowCounter = 0;
            while (rs.next()) {
                currentSqlFetchRowCounter++;
                jsonObj = new JSONObject();
                for (int j = 1; j <= numberOfColumns; j++) {
                    try {
                        jsonObj.put(rsMetaData.getColumnName(j).trim(), rs.getString(j).trim());
                        jsonObj.put(rsMetaData.getColumnName(j).trim() + "__TYPE", rsMetaData.getColumnType(j));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                queryResults.put(jsonObj);
            }
            runTimedTask = false;
        }
    }

    public void runRawQuery(String inputQuery) throws SQLException {
        query = inputQuery;
        execute("run_raw_query");
    }

    public void runRawQueryBackgroundProcess() throws SQLException {
        runMode = "raw_query";
        runTimedTask = true;
        if (connected) {
            queryResults = new JSONArray();
            JSONObject jsonObj;
            PreparedStatement updateTable = ibmConnection.prepareStatement(query);
            ResultSet rs = updateTable.executeQuery();
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int numberOfColumns = rsMetaData.getColumnCount();
            columnsNames = new ArrayList<>();
            columnsTypes = new ArrayList<>();
            for (int i = 1; i <= numberOfColumns; i++) {
                columnsNames.add(rsMetaData.getColumnName(i).trim());
                columnsTypes.add(rsMetaData.getColumnType(i));
            }

            currentSqlFetchRowCounter = 0;
            callbackSqlFetchRowCounter = 0;
            while (rs.next()) {
                currentSqlFetchRowCounter++;
                jsonObj = new JSONObject();
                for (int j = 1; j <= numberOfColumns; j++) {
                    try {
                        jsonObj.put(rsMetaData.getColumnName(j).trim(), rs.getString(j).trim());
                        jsonObj.put(rsMetaData.getColumnName(j).trim() + "__TYPE", rsMetaData.getColumnType(j));
                    } catch (JSONException e) {
                    }
                }
                //Log.d("BAAAAAAAAAAA", currentSqlFetchRowCounter+"");
                queryResults.put(jsonObj);
            }
            runTimedTask = false;
        }
    }

    public void runUpdateQuery(String inputQuery, String[] inputQueryParams, int[] inputQueryTypes) throws SQLException {
        query = inputQuery;
        queryParams = inputQueryParams;
        queryTypes = inputQueryTypes;
        execute("update_query");
    }

    public void runUpdateQueryBackgroundProcess() throws SQLException {
        runMode = "update_query";
        runTimedTask = true;
        JSONObject output = new JSONObject();
        if (connected) {
            try {
                PreparedStatement preparedStatement = ibmConnection.prepareStatement(query);
                int tmpCounter = 0;
                for(String value : queryParams) {
                    tmpCounter++;

                    preparedStatement.setString(tmpCounter, value);
                }

                int preparedStatementRows = preparedStatement.executeUpdate();
                ResultSet preparedStatementKeys = preparedStatement.getGeneratedKeys();
                int preparedStatementKeySize = 0;
                if (preparedStatementKeys != null) {
                    preparedStatementKeys.last();
                    preparedStatementKeySize = preparedStatementKeys.getRow();
                }
                output.put("AFFECTED_ROWS", preparedStatementRows);
                output.put("RETURNED_KEYS", preparedStatementKeySize);
                //Log.d("SQL insert affected", "PS1 - SQL insert affected " + preparedStatementRows + " rows and returned " + preparedStatementKeySize + " keys");
                //Log.d("SQL getUpdateCount", "PS1 - getUpdateCount()="+ preparedStatement.getUpdateCount());
            } catch (Exception e) {
                try {
                    output.put("UPDATE_ERROR", e.getMessage());
                } catch (Exception ignored) { }
            }
            as400Callback(output, runMode);
            runTimedTask = false;
        }
    }

    public void updateRawQuery(String inputQuery) throws SQLException {
        execute("");
        //Connection con = DriverManager.getConnection("jdbc:as400://" + system + ";libraries=*LIBL;naming=system", username, password);
        //Statement statement = con.createStatement();
        Statement statement = ibmConnection.createStatement();
        statement.executeUpdate(inputQuery);
    }

    public void callSQLStoredProcedure2(final JSONObject object) {
        final JSONObject output = new JSONObject();
        new Thread(() -> {
            try {
                //ArrayList<Object> output = new ArrayList<>();
                //Connection con = DriverManager.getConnection("jdbc:as400://" + system + ";libraries=*LIBL;naming=system", username, password);
                //AS400JDBCCallableStatement statement = AS400JDBCCallableStatement.class.cast(con.prepareCall("CALL BRDATA.PRINT_JOB_SUBMIT(?, ?, ?, ?, ?, ?, ?, ?, ?)"));
                AS400JDBCCallableStatement statement = AS400JDBCCallableStatement.class.cast(ibmConnection.prepareCall("CALL HAROLD.MANAGE_ACCOUNTS(?,?,?,?,?,?,?,?,?)"));
                // TEST LINE //AS400JDBCCallableStatement statement = AS400JDBCCallableStatement.class.cast(con.prepareCall(inputStoredProcedureQuery));
                //"DEPOSIT", "SAVINGS", "111", "50", "", "0", "0", "SAVING FOR LIFE", "M"
                statement.setString("INPUT_TRANSACTION_TYPE", object.getString("INPUT_TRANSACTION_TYPE"));
                statement.setString("INPUT_ACCOUNT_TYPE1", object.getString("INPUT_ACCOUNT_TYPE1"));
                statement.setBigDecimal("INPUT_ACCOUNT1", new BigDecimal(object.getDouble("INPUT_ACCOUNT1")));
                statement.setBigDecimal("INPUT_AMOUNT1", new BigDecimal(object.getDouble("INPUT_AMOUNT1")));
                statement.setString("INPUT_ACCOUNT_TYPE2", object.getString("INPUT_ACCOUNT_TYPE2"));
                statement.setBigDecimal("INPUT_ACCOUNT2", new BigDecimal(object.getDouble("INPUT_ACCOUNT2")));
                statement.setBigDecimal("INPUT_AMOUNT2", new BigDecimal(object.getDouble("INPUT_AMOUNT2")));
                statement.setString("INPUT_NOTES", object.getString("INPUT_NOTES"));
                statement.registerOutParameter("JOB_RESULT", Types.VARCHAR);
                statement.execute();
                String st = statement.getString("JOB_RESULT");
                if (st != null) {
                    output.put("RESULT", new JSONObject(statement.getString("JOB_RESULT")));
                    as400Callback(output);
                } else {
                    as400Callback("Communication Problem!");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public ArrayList<Object> callSQLStoredProcedure(String inputStoredProcedureQuery,
                                                    ArrayList<Object> inputStoredProcedureQueryVariablesValues,
                                                    ArrayList<String> inputStoredProcedureQueryVariablesTypes,
                                                    ArrayList<String> inputStoredProcedureQueryVariablesINOUTTypes,
                                                    ArrayList<Object> inputStoredProcedureQueryVariablesTypesOption1,
                                                    ArrayList<Object> inputStoredProcedureQueryVariablesTypesOption2) throws SQLException {
        execute("");
        ArrayList<Object> output = new ArrayList<>();
        //Connection con = DriverManager.getConnection("jdbc:as400://" + system + ";libraries=*LIBL;naming=system", username, password);
        //AS400JDBCCallableStatement statement = AS400JDBCCallableStatement.class.cast(con.prepareCall("CALL BRDATA.PRINT_JOB_SUBMIT(?, ?, ?, ?, ?, ?, ?, ?, ?)"));
        CallableStatement statement = CallableStatement.class.cast(ibmConnection.prepareCall("CALL HAROLD.MANAGE_ACCOUNTS(?,?,?,?,?,?,?,?,?)"));
        // TEST LINE //AS400JDBCCallableStatement statement = AS400JDBCCallableStatement.class.cast(con.prepareCall(inputStoredProcedureQuery));
        //"DEPOSIT", "SAVINGS", "111", "50", "", "0", "0", "SAVING FOR LIFE", "M"
        statement.setString("INPUT_TRANSACTION_TYPE", "DEPOSIT");
        statement.setString("INPUT_ACCOUNT_TYPE1", "SAVINGS");
        statement.setDouble("INPUT_ACCOUNT1", 111);
        statement.setDouble("INPUT_AMOUNT1", 50);
        statement.setString("INPUT_ACCOUNT_TYPE2", "");
        statement.setDouble("INPUT_ACCOUNT2", 0);
        statement.setDouble("INPUT_AMOUNT2", 0);
        statement.setString("INPUT_NOTES", "SAVING FOR LIFE");
        statement.registerOutParameter("JOB_RESULT", Types.VARCHAR);
        statement.execute();

        output.add(statement.getString(0));
        /*int counter = 0;
        int paramCounter = 1;
        String variableType;
        for (Object aVariable : inputStoredProcedureQueryVariablesValues) {
            variableType = inputStoredProcedureQueryVariablesTypes.get(counter).toString().trim().toUpperCase();
            switch (variableType) {
                case "ASCII_STREAM": //setAsciiStream(String parameterName, InputStream parameterValue, int length)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setAsciiStream(paramCounter, (InputStream)aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setAsciiStream(paramCounter, (InputStream)aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                    }
                    break;
                case "BIG_DECIMAL": //setBigDecimal(String parameterName, BigDecimal parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setBigDecimal(paramCounter, (BigDecimal)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setBigDecimal(paramCounter, (BigDecimal)aVariable);
                            break;
                    }
                    break;
                case "BINARY_STREAM": //setBinaryStream(String parameterName, InputStream parameterValue, int length)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setBinaryStream(paramCounter, (InputStream)aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setBinaryStream(paramCounter, (InputStream)aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                    }
                    break;
                case "BOOLEAN": //setBoolean(String parameterName, boolean parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setBoolean(paramCounter, (boolean)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setBoolean(paramCounter, (boolean)aVariable);
                            break;
                    }
                    break;
                case "BYTE": //setByte(String parameterName, byte parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setByte(paramCounter, (byte)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setByte(paramCounter, (byte)aVariable);
                            break;
                    }
                    break;
                case "BYTES": //setBytes(String parameterName, byte[] parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setBytes(paramCounter, (byte[])aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setBytes(paramCounter, (byte[])aVariable);
                            break;
                    }
                    break;
                case "CHARACTER_STREAM": //setCharacterStream(String parameterName, Reader parameterValue, int length)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setCharacterStream(paramCounter, (Reader)aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setCharacterStream(paramCounter, (Reader)aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                    }
                    break;
                case "DATE1": //setDate(String parameterName, Date parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setDate(paramCounter, (Date)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setDate(paramCounter, (Date)aVariable);
                            break;
                    }
                    break;
                case "DATE2": //setDate(String parameterName, Date parameterValue, Calendar cal)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setDate(paramCounter, (Date)aVariable, (Calendar)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setDate(paramCounter, (Date)aVariable, (Calendar)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                    }
                    break;
                case "DOUBLE": //setDouble(String parameterName, double parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setDouble(paramCounter, (Double)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setDouble(paramCounter, (Double)aVariable);
                            break;
                    }
                    break;
                case "FLOAT": //setFloat(String parameterName, float parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setFloat(paramCounter, (Float)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setFloat(paramCounter, (Float)aVariable);
                            break;
                    }
                    break;
                case "INT": //setInt(String parameterName, int parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setInt(paramCounter, (int)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setInt(paramCounter, (int)aVariable);
                            break;
                    }
                    break;
                case "LONG": //setLong(String parameterName, long parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setLong(paramCounter, (Long)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setLong(paramCounter, (Long)aVariable);
                            break;
                    }
                    break;
                case "NULL1": //setNull(String parameterName, int sqlType)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setNull(paramCounter, (int)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setNull(paramCounter, (int)aVariable);
                            break;
                    }
                    break;
                case "NULL2": //setNull(String parameterName, int sqlType, String typeName)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setNull(paramCounter, (int)aVariable, (String)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setNull(paramCounter, (int)aVariable, (String)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                    }
                    break;
                case "OBJECT1": //setObject(String parameterName, Object parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setObject(paramCounter, aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setObject(paramCounter, aVariable);
                            break;
                    }
                    break;
                case "OBJECT2": //setObject(String parameterName, Object parameterValue, int targetSqlType)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setObject(paramCounter, aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setObject(paramCounter, aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                    }
                    break;
                case "OBJECT3": //setObject(String parameterName, Object parameterValue, int targetSqlType, int scale)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setObject(paramCounter, aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter), (int)inputStoredProcedureQueryVariablesTypesOption2.get(counter));
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setObject(paramCounter, aVariable, (int)inputStoredProcedureQueryVariablesTypesOption1.get(counter), (int)inputStoredProcedureQueryVariablesTypesOption2.get(counter));
                            break;
                    }
                    break;
                case "SHORT": //setShort(String parameterName, short parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setShort(paramCounter, (short)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setShort(paramCounter, (short)aVariable);
                            break;
                    }
                    break;
                case "STRING": //setString(String parameterName, String parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setString(paramCounter, (String)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setString(paramCounter, (String)aVariable);
                            break;
                    }
                    break;
                case "TIME1": //setTime(String parameterName, Time parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setTime(paramCounter, (Time)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setTime(paramCounter, (Time)aVariable);
                            break;
                    }
                    break;
                case "TIME2": //setTime(String parameterName, Time parameterValue, Calendar cal)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setTime(paramCounter, (Time)aVariable, (Calendar)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setTime(paramCounter, (Time)aVariable, (Calendar)inputStoredProcedureQueryVariablesTypesOption1.get(counter));
                            break;
                    }
                    break;
                case "TIMESTAMP1": //setTimestamp(String parameterName, Timestamp parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setTimestamp(paramCounter, (Timestamp)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setTimestamp(paramCounter, (Timestamp)aVariable);
                            break;
                    }
                    break;
                case "TIMESTAMP2": //setTimestamp(String parameterName, Timestamp parameterValue, Calendar cal)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setTimestamp(paramCounter, (Timestamp)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setTimestamp(paramCounter, (Timestamp)aVariable);
                            break;
                    }
                    break;
                case "URL": //setURL(String parameterName, URL parameterValue)
                    switch (inputStoredProcedureQueryVariablesINOUTTypes.get(counter).toString().trim().toUpperCase()) {
                        case "IN": statement.setURL(paramCounter, (URL)aVariable);
                            break;
                        case "OUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            break;
                        case "INOUT": statement.registerOutParameter(paramCounter, Types.CHAR);
                            statement.setURL(paramCounter, (URL)aVariable);
                            break;
                    }
                    break;
            }
            counter++;
            paramCounter++;
        }

        statement.execute();

        counter = 1;
        for (String VariablesINOUTType : inputStoredProcedureQueryVariablesINOUTTypes) {
            switch (VariablesINOUTType.trim().toUpperCase()) {
                case "OUT":
                case "INOUT": output.add(statement.getString(counter));
                    break;
            }
            counter++;
        }*/
        return output;
    }
}
