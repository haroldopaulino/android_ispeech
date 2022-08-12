package com.speech;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity {
    TextView txt;
    Spinner
            operation,
            accountType1,
            accountType2;
    EditText
            account1,
            amount1,
            account2,
            amount2,
            notes;
    CheckBox autoExecute;
    Button start, stop;
    ListView list;

    private boolean runStatement = false;
    public LocalHomeActivityAS400Core as400CoreSchemas;

    static class LocalSettingsAlert extends SettingsAlert {
        LocalSettingsAlert(
                Activity activityInput,
                AlertDialog.Builder alertInput,
                View viewInput) {
            super(activityInput, alertInput, viewInput);
        }

        @Override
        public void callback(JSONObject inputResponse) {

        }
    }

    class LocalHomeActivityAS400Core extends AS400Core {
        public LocalHomeActivityAS400Core(Activity inputActivity,
                                          String inputSystem,
                                          String inputUsername,
                                          String inputPassword) throws SQLException {
            super(inputActivity,
                    inputSystem,
                    inputUsername,
                    inputPassword);
        }

        @Override
        public void as400Callback(String result) {
            txt.setText(result);
        }

        @Override
        public void as400Callback(Boolean connected) {
            if (!runStatement) {
                return;
            }
            try {
                String
                        operationValue = operation.getSelectedItem().toString(),
                        accountType1Value = accountType1.getSelectedItem().toString(),
                        accountType2Value = accountType2.getSelectedItem().toString();
                double account1Value = 0;
                try { account1Value = Double.parseDouble(account1.getText().toString().trim()); } catch (Exception e) {}
                double account2Value = 0;
                try { account2Value = Double.parseDouble(account2.getText().toString().trim()); } catch (Exception e) {}
                double amount1Value = 0;
                try { amount1Value = Double.parseDouble(amount1.getText().toString().trim()); } catch (Exception e) {}
                double amount2Value = 0;
                try { amount2Value = Double.parseDouble(amount2.getText().toString().trim()); } catch (Exception e) {}
                as400CoreSchemas.callSQLStoredProcedure2(new JSONObject()
                        .put("INPUT_TRANSACTION_TYPE", operationValue)
                        .put("INPUT_ACCOUNT_TYPE1", accountType1Value)
                        .put("INPUT_ACCOUNT1", account1Value)
                        .put("INPUT_AMOUNT1", amount1Value)
                        .put("INPUT_ACCOUNT_TYPE2", accountType2Value)
                        .put("INPUT_ACCOUNT2", account2Value)
                        .put("INPUT_AMOUNT2", amount2Value)
                        .put("INPUT_NOTES", notes.getText().toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            runStatement = false;
        }

        @Override
        public void as400Callback(final JSONObject results) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable(){
                    @Override
                    public void run() {
                        txt.setText(results.toString());

                        ArrayList<SpinnerItemData> contents = new ArrayList<>();

                        try {
                            JSONObject tmpJson = results.getJSONObject("RESULT");
                            Iterator<String> iter = tmpJson.keys();
                            while (iter.hasNext()) {
                                String key = iter.next();
                                try {
                                    Object value = tmpJson.get(key);
                                    contents.add(new SpinnerItemData(key + " \n" + value, 1));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        final SpinnerAdapter spinnerAdapter = new SpinnerAdapter(activity, R.layout.spinner_list_item, R.id.spinnerTextview, contents);
                        list.setAdapter(spinnerAdapter);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ClickHandler implements View.OnClickListener {
        String action;

        ClickHandler(String actionInput) {
            action = actionInput;
        }

        @Override
        public void onClick(View view) {
            switch(action) {
                case "OPEN_SETTINGS_DIALOG" :
                    openSettingsDialog();
                    break;
            }
        }
    }

    private void openSettingsDialog() {
        AlertDialog.Builder alertSettings = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        LocalSettingsAlert localSettingsAlert = new LocalSettingsAlert(
                this,
                alertSettings,
                inflater.inflate(R.layout.settings_alert, null));
        localSettingsAlert.getAlert();
    }

    public static void hideKeyboard( Activity activity ) {
        InputMethodManager imm = (InputMethodManager)activity.getSystemService( Context.INPUT_METHOD_SERVICE );
        View f = activity.getCurrentFocus();
        if( null != f && null != f.getWindowToken() && EditText.class.isAssignableFrom( f.getClass() ) )
            imm.hideSoftInputFromWindow( f.getWindowToken(), 0 );
        else
            activity.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN );
    }

    public void runSql() {
        try {
            hideKeyboard(this);
            txt.setText("WAIT FOR IT ...");
            runStatement = true;
            String system = "65.100.241.241";
            String username = "HAROLD";
            String password = "Testing1";
            as400CoreSchemas = new LocalHomeActivityAS400Core(this, system, username, password);
            as400CoreSchemas.execute();
            //as400CoreSchemas.runRawQuery("CALL HAROLD.MANAGE_ACCOUNTS('DEPOSIT', 'SAVINGS', 111, 50, '', 0, 0, 'SAVING FOR LIFE', 'M')");

            //String[] params = {"DEPOSIT", "SAVINGS", "111", "50", "", "0", "0", "SAVING FOR LIFE", "M"};
            //as400CoreSchemas.runQuery("CALL HAROLD.MANAGE_ACCOUNTS(?,?,?,?,?,?,?,?,?)", params);


            //Log.e("callSQLStoredProcedure2", as400CoreSchemas.callSQLStoredProcedure2());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.menuContainer).setOnClickListener(new ClickHandler("OPEN_SETTINGS_DIALOG"));
        txt = findViewById(R.id.txt);



        operation = findViewById(R.id.operation);
        accountType1 = findViewById(R.id.accountType1);
        accountType2 = findViewById(R.id.accountType2);
        account1 = findViewById(R.id.account1);
        amount1 = findViewById(R.id.amount1);
        account2 = findViewById(R.id.account2);
        amount2 = findViewById(R.id.amount2);
        notes = findViewById(R.id.notes);
        autoExecute = findViewById(R.id.autoExecute);

        amount2.setVisibility(View.GONE);

        list = findViewById(R.id.list);
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSpeechRecognizer();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runSql();
            }
        });
    }

    private void startSpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, 1);

        //parseReceivedText("transfer $10 from savings account 111 to checking account 123 note this is a test");
        //parseReceivedText("i need to deposit $5 into savings account 111");
        //parseReceivedText("transfer $10 from savings account 111 to checking account 123");
        //parseReceivedText("i need the balance for the savings account 111");
        //parseReceivedText("i need to withdraw $5 from the checking account 123");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                List<String> speechResults = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String result = speechResults.get(0);
                txt.setText(result);
                parseReceivedText(result);
            }
        }
    }

    private void parseReceivedText(String inputText) {
        txt.setText(inputText);
        String[] parts = inputText.split(" ");
        String operationDescription = "";
        boolean
                operationFound = false,
                accountType1Found = false,
                account1Found = false,
                amount1Found = false,
                accountType2Found = false,
                account2Found = false,
                notesFound = false;
        for (int i = 0; i < parts.length; i++) {
            if (!operationFound) {
                int selectedOperation = -1;
                switch(parts[i].toUpperCase()) {
                    case "BALANCE" : selectedOperation = 0; break;
                    case "DEPOSIT" : selectedOperation = 1; break;
                    case "WITHDRAW" : selectedOperation = 2; break;
                    case "TRANSFER" : selectedOperation = 3; break;
                }
                operationDescription = parts[i];

                if (selectedOperation != -1) {
                    operation.setSelection(selectedOperation);
                    operationFound = true;
                }
            } else {
                if (operationDescription.equalsIgnoreCase("BALANCE")) {
                    if (!accountType1Found) {
                        int accountType1Selection = -1;
                        if (parts[i].equalsIgnoreCase("CHECKING")) {
                            accountType1Selection = 0;
                        }

                        if (parts[i].equalsIgnoreCase("SAVINGS")) {
                            accountType1Selection = 1;
                        }

                        if (accountType1Selection != -1) {
                            accountType1.setSelection(accountType1Selection);
                            accountType1Found = true;
                        }
                    } else {
                        if (!account1Found) {
                            int accountNumber = -1;
                            try {
                                accountNumber = Integer.parseInt(parts[i]);
                            } catch (Exception e) {
                            }
                            if (accountNumber != -1) {
                                account1.setText(accountNumber + "");
                                account1Found = true;
                            }
                        } else {
                            if (!notesFound) {
                                int noteSelection = -1;

                                if (parts[i].equalsIgnoreCase("note")) {
                                    noteSelection = 1;
                                }

                                if (noteSelection != -1) {
                                    StringBuilder sb = new StringBuilder();
                                    for (int j = i + 1; j < parts.length; j++) {
                                        sb.append(parts[j] + " ");
                                    }
                                    notes.setText(sb.toString());
                                    notesFound = true;
                                }
                            }
                        }
                    }
                }

                if (operationDescription.equalsIgnoreCase("DEPOSIT") ||
                        operationDescription.equalsIgnoreCase("WITHDRAW")) {
                    if (!amount1Found) {
                        double amount = -1;
                        if (parts[i].indexOf("$") != -1) {
                            try {
                                amount = Double.parseDouble(parts[i].replace("$", ""));
                            } catch (Exception e) {
                            }
                            if (amount != -1) {
                                amount1.setText(amount + "");
                                amount1Found = true;
                            }
                        }
                    } else {
                        if (!accountType1Found) {
                            int accountType1Selection = -1;
                            if (parts[i].equalsIgnoreCase("CHECKING")) {
                                accountType1Selection = 0;
                            }

                            if (parts[i].equalsIgnoreCase("SAVINGS")) {
                                accountType1Selection = 1;
                            }

                            if (accountType1Selection != -1) {
                                accountType1.setSelection(accountType1Selection);
                                accountType1Found = true;
                            }
                        } else {
                            if (!account1Found) {
                                int accountNumber = -1;
                                try {
                                    accountNumber = Integer.parseInt(parts[i]);
                                } catch (Exception e) {
                                }
                                if (accountNumber != -1) {
                                    account1.setText(accountNumber + "");
                                    account1Found = true;
                                }
                            } else {
                                if (!notesFound) {
                                    int noteSelection = -1;

                                    if (parts[i].equalsIgnoreCase("note")) {
                                        noteSelection = 1;
                                    }

                                    if (noteSelection != -1) {
                                        StringBuilder sb = new StringBuilder();
                                        for (int j = i + 1; j < parts.length; j++) {
                                            sb.append(parts[j] + " ");
                                        }
                                        notes.setText(sb.toString());
                                        notesFound = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if (operationDescription.equalsIgnoreCase("TRANSFER")) {
                    if (!amount1Found) {
                        double amount = -1;
                        if (parts[i].indexOf("$") != -1) {
                            try {
                                amount = Double.parseDouble(parts[i].replace("$", ""));
                            } catch (Exception e) {
                            }
                            if (amount != -1) {
                                amount1.setText(amount + "");
                                amount2.setText(amount + "");
                                amount1Found = true;
                            }
                        }
                    } else {
                        if (!accountType1Found) {
                            int accountType1Selection = -1;
                            if (parts[i].equalsIgnoreCase("CHECKING")) {
                                accountType1Selection = 0;
                            }

                            if (parts[i].equalsIgnoreCase("SAVINGS")) {
                                accountType1Selection = 1;
                            }

                            if (accountType1Selection != -1) {
                                accountType1.setSelection(accountType1Selection);
                                accountType1Found = true;
                            }
                        } else {
                            if (!account1Found) {
                                int accountNumber = -1;
                                try {
                                    accountNumber = Integer.parseInt(parts[i]);
                                } catch (Exception e) {
                                }
                                if (accountNumber != -1) {
                                    account1.setText(accountNumber + "");
                                    account1Found = true;
                                }
                            } else {
                                if (!accountType2Found) {
                                    int accountType2Selection = -1;
                                    if (parts[i].equalsIgnoreCase("CHECKING")) {
                                        accountType2Selection = 1;
                                    }

                                    if (parts[i].equalsIgnoreCase("SAVINGS")) {
                                        accountType2Selection = 0;
                                    }

                                    if (accountType2Selection != -1) {
                                        accountType2.setSelection(accountType2Selection);
                                        accountType2Found = true;
                                    }
                                } else {
                                    if (!account2Found) {
                                        int accountNumber = -1;
                                        try {
                                            accountNumber = Integer.parseInt(parts[i]);
                                        } catch (Exception e) {
                                        }
                                        if (accountNumber != -1) {
                                            account2.setText(accountNumber + "");
                                            account2Found = true;
                                        }
                                    } else {
                                        if (!notesFound) {
                                            int noteSelection = -1;

                                            if (parts[i].equalsIgnoreCase("note")) {
                                                noteSelection = 1;
                                            }

                                            if (noteSelection != -1) {
                                                StringBuilder sb = new StringBuilder();
                                                for (int j = i + 1; j < parts.length; j++) {
                                                    sb.append(parts[j] + " ");
                                                }
                                                notes.setText(sb.toString());
                                                notesFound = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (autoExecute.isChecked()) {
            runSql();
        }
    }
}
