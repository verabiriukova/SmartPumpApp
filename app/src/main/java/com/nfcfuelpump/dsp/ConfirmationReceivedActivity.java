package com.nfcfuelpump.dsp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import kankan.wheel.widget.OnWheelClickedListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;
import kankan.wheel.widget.adapters.NumericWheelAdapter;


public class ConfirmationReceivedActivity extends Activity {


    /*
    <intent-filter>
                <action android:name="android.nfc.action.NDEF_DISCOVERED" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="vnd.android.nfc"
                    android:host="ext"
                    android:pathPrefix="/com.example:externalType"/>
            </intent-filter>
     */


    public final static String AMOUNT = "com.nfcfuelpump.dsp.AMOUNT"; //the type of response received from pump
    // ERROR, COST, AMOUNT
    public final static String COST = "com.nfcfuelpump.dsp.COST";                 // either cost of fuel or amount, 0 is error


    private TextView amountTextField;
    private TextView costTextField;



    //NFC Stuff
    private PendingIntent mPendingIntent;
    private NfcAdapter mNfcAdapter;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation_received);

        Intent received_intent = getIntent();//get the intent that has started this activity
        Bundle intentExtras = received_intent.getExtras();  // extract the information field

        /* Further processing of the information field here */

        amountTextField = (TextView) findViewById(R.id.amountField);
        costTextField = (TextView) findViewById(R.id.costField);
        amountTextField.setText(String.format( "%.2f L", intentExtras.getFloat(AMOUNT)) );
        costTextField.setText(String.format( "%.2f £", intentExtras.getFloat(COST)) );
       // amountTextField.setText(Float.toString(intentExtras.getFloat(AMOUNT)));
        //costTextField.setText(Float.toString(intentxtras.getFloat(COST)));
       // mTextView.setText(Integer.toString(bytesToInteger(response_data)));


        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); //find phone NFC adapter
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }


    //   public static int unsignedToBytes(byte b) {
    //      return b & 0xFF;
    //  }


    /* convert signed byte array to unsigned int */
    private static int bytesToInteger(byte[] bytes) {
        int size = bytes.length;
        if (size == 2) return ((bytes[1] & 0xFF) << 8) | bytes[0] & 0xFF;
        else return bytes[0] & 0xFF;
    }


    /* Prevent the user from navigating back to NFC Transfer Activity */
    @Override
    public void onBackPressed() {
        Intent mainIntent = new Intent(this, WheelEntryActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
        return;
    }








    @Override
    public void onPause() {

        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }





}

 /*   @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
  /*      handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        }
    }


*/


    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
   /* private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE && Arrays.equals(ndefRecord.getType(), expectedType)) {

                        return "Got External Record";
                }
                else return "Got trash";
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

    /*        byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mTextView.setText("Read content: " + result);
            }
        }
    }





} */