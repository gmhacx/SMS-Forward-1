package org.tpmkranz.smsforward;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.security.Security;
import java.util.HashMap;


public class SetupActivity extends AppCompatActivity {
    protected static String SHAREDPREFSNAME = "org.tpmkranz.smsforward.SETTINGS";
    protected static String SHAREDPREFSEMAIL = "email";
    protected static String SHAREDPREFSPASSWORD = "pass";
    protected static String SHAREDPREFSSERVER = "server";
    protected static String SHAREDPREFSPORT = "port";
    protected static String SHAREDPREFSTARGET = "target";
    protected static String SHAREDPREFSPUBKEY = "pubkey";
    private ComponentName receiver;
    private PackageManager pm;
    private boolean enabled;
    private FloatingActionButton fab;
    private EditText inputEmail, inputPassword, inputServer, inputPort, inputTarget, inputPubkey;
    private SharedPreferences settings;
    private HashMap<String,String> currentSettings;


    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        inputEmail = (EditText) findViewById(R.id.input_email);
        inputPassword = (EditText) findViewById(R.id.input_password);
        inputServer = (EditText) findViewById(R.id.input_server);
        inputPort = (EditText) findViewById(R.id.input_port);
        inputTarget = (EditText) findViewById(R.id.input_target);
        inputPubkey = (EditText) findViewById(R.id.input_pubkey);
        settings = this.getSharedPreferences(SHAREDPREFSNAME, Context.MODE_PRIVATE);
        currentSettings = new HashMap<>();
        readSettings(true);
        inputEmail.setText(currentSettings.get(SHAREDPREFSEMAIL));
        inputServer.setText(currentSettings.get(SHAREDPREFSSERVER));
        inputPort.setText(currentSettings.get(SHAREDPREFSPORT));
        inputTarget.setText(currentSettings.get(SHAREDPREFSTARGET));
        inputPubkey.setText(currentSettings.get(SHAREDPREFSPUBKEY));
        receiver = new ComponentName(this, SMSListener.class);
        pm = getPackageManager();
    }


    @Override
    protected void onResume(){
        super.onResume();

        enabled = isListenerListening(receiver);
        switchUIStates(enabled);
    }

    private void readSettings(boolean fromPrefs){
        currentSettings.put(SHAREDPREFSEMAIL, fromPrefs ? settings.getString(SHAREDPREFSEMAIL, "") : inputEmail.getText().toString());
        currentSettings.put(SHAREDPREFSPASSWORD, fromPrefs || inputPassword.getText().toString().isEmpty() ? settings.getString(SHAREDPREFSPASSWORD, "") : inputPassword.getText().toString());
        currentSettings.put(SHAREDPREFSSERVER, fromPrefs ? settings.getString(SHAREDPREFSSERVER, "") : inputServer.getText().toString());
        currentSettings.put(SHAREDPREFSPORT, fromPrefs ? settings.getString(SHAREDPREFSPORT, "") : inputPort.getText().toString());
        currentSettings.put(SHAREDPREFSTARGET, fromPrefs ? settings.getString(SHAREDPREFSTARGET, "") : inputTarget.getText().toString());
        currentSettings.put(SHAREDPREFSPUBKEY, fromPrefs ? settings.getString(SHAREDPREFSPUBKEY, "") : inputPubkey.getText().toString());
    }

    private boolean haveSettingsChanged(){
        return !getInputText(inputEmail).equals(currentSettings.get(SHAREDPREFSEMAIL))
                || (!getInputText(inputPassword).equals(currentSettings.get(SHAREDPREFSPASSWORD))
                && !getInputText(inputPassword).equals(""))
                || !getInputText(inputServer).equals(currentSettings.get(SHAREDPREFSSERVER))
                || !getInputText(inputPort).equals(currentSettings.get(SHAREDPREFSPORT))
                || !getInputText(inputTarget).equals(currentSettings.get(SHAREDPREFSTARGET))
                || !getInputText(inputPubkey).equals(currentSettings.get(SHAREDPREFSPUBKEY));
    }

    private boolean areSettingsValid(){
        return getInputText(inputEmail).contains("@")
                && !(getInputText(inputPassword).isEmpty() && settings.getString(SHAREDPREFSPASSWORD, "").isEmpty())
                && getInputText(inputServer).contains(".")
                && !getInputText(inputPort).isEmpty()
                && getInputText(inputTarget).contains("@")
                && (getInputText(inputPubkey).contains("-----BEGIN PGP PUBLIC KEY BLOCK-----")
                    || getInputText(inputPubkey).isEmpty());
    }

    private boolean isListenerListening(ComponentName listener){
        return pm.getComponentEnabledSetting(listener) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private void switchUIStates(boolean state){
        Drawable fabImage = getResources().getDrawable(state ? R.drawable.pause : R.drawable.play);
        fab.setImageDrawable(fabImage);
        inputEmail.setEnabled(!state);
        inputPassword.setEnabled(!state);
        inputServer.setEnabled(!state);
        inputPort.setEnabled(!state);
        inputTarget.setEnabled(!state);
        inputPubkey.setEnabled(!state);
    }

    private String getInputText(EditText input){
        return input.getText().toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void enableBroadcastReceiver(View view) {
        if (areSettingsValid()) {
            if (haveSettingsChanged()) {
                writeSettings();
                askUserToTestSettings();
            }else{
                pm.setComponentEnabledSetting(receiver,
                        (enabled ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED),
                        PackageManager.DONT_KILL_APP);
            }
            enabled = isListenerListening(receiver);
            switchUIStates(enabled);
        }
    }

    private void writeSettings() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SHAREDPREFSEMAIL, getInputText(inputEmail));
        if (!getInputText(inputPassword).isEmpty())
            editor.putString(SHAREDPREFSPASSWORD, getInputText(inputPassword));
        editor.putString(SHAREDPREFSSERVER, getInputText(inputServer));
        editor.putString(SHAREDPREFSPORT, getInputText(inputPort));
        editor.putString(SHAREDPREFSTARGET, getInputText(inputTarget));
        editor.putString(SHAREDPREFSPUBKEY, getInputText(inputPubkey));
        editor.apply();
        readSettings(false);
    }

    private void askUserToTestSettings() {
        Snackbar
                .make(findViewById(R.id.setup_layout), R.string.snackbar_settings_changed, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_settings_confirmed, new SnackBarSettingsConfirmationListener())
                .show();
        Intent testMailIntent = new Intent(this, EmailSenderService.class);
        testMailIntent.putExtra(EmailSenderService.INTENTEXTRASUBJECT, getResources().getString(R.string.email_subject_test));
        testMailIntent.putExtra(EmailSenderService.INTENTEXTRABODY, getResources().getString(R.string.email_body_test));
        startService(testMailIntent);
    }

    public void expandIntro(View view) {
        TextView intro = (TextView) view;
        intro.setSingleLine(intro.getMaxLines() != 1);
    }

    private class SnackBarSettingsConfirmationListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            enableBroadcastReceiver(v);
        }
    }
}
