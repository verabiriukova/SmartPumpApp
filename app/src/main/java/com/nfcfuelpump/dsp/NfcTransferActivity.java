package com.nfcfuelpump.dsp;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class NfcTransferActivity extends Activity implements NfcAdapter.OnNdefPushCompleteCallback{


    public final static String COST = "com.nfcfuelpump.dsp.COST"; //the type of response received from pump
                                                                                    // ERROR, COST, AMOUNT
    public final static String AMOUNT = "com.nfcfuelpump.dsp.AMOUNT";                 // either cost of fuel or amount, 0 is error



   /* Pump communication protocol specific constants */

    private final static byte EXP_PAYLOAD_LENGTH = 2; //expected size of payload in bytes
                                                    //if received payload is not equal to EXP_PAYLOAD_LENGTH
                                                    //the message is discarded as corrupt
   // private final static byte MIN_PAYLOAD_LENGTH = 1; //minimum size of payload in bytes
                                                    //if received payload is larger than MAX_PAYLOAD_LENGTH
                                                    //the message is discarded as corrupt
    private final static String cost_type = "cost";
    private final static String amount_type = "amount";
    private final static String error_type = "error";
    private final static String external_type = "externalType";
    private final static String domain = "com.example";



    /// NFC Stuff
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private Intent mainIntent;

    // Dialogs
    private AlertDialog nfcNotEnabledDialog;
    private AlertDialog nfcPushNotEnabledDialog;

    private TextView user_instructions;

    private NdefRecord myRecord;
    private NdefMessage mNdefMessage;
    private TextView user_selection;


    private boolean messageSent = false;

    private String type;
    private String responseType;

    //user selection from intent
    private int received_value;
    private int received_type;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nfc_transfer);
        user_selection = (TextView) findViewById(R.id.user_selection);
        user_instructions = (TextView) findViewById(R.id.user_instructions);
        user_instructions.setTextColor(Color.WHITE);
        user_instructions.setText("Hold the back of the phone to pump terminal");


        Intent received_intent = getIntent();
        received_value = received_intent.getIntExtra("com.nfcfuelpump.dsp.SELECTED_VALUE",0);
        received_type = received_intent.getIntExtra("com.nfcfuelpump.dsp.SELECTED_TYPE",0);
        if(received_type == 0) {
            if(received_value == 1)  user_selection.setText("Your selection: " + Integer.toString(received_value) + " Liter");
            else user_selection.setText("Your selection: " + Integer.toString(received_value) + " Liters");
            type = amount_type;
            responseType = cost_type;
        }

        else {
            user_selection.setText("Your selection: " + Integer.toString(received_value) + " GBP");
            type = cost_type;
            responseType = amount_type;
        }


        mNfcAdapter = NfcAdapter.getDefaultAdapter(this); //find phone NFC adapter


        // Intent to main activity

        mainIntent = new Intent(this, WheelEntryActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.putExtra("EXIT", true);

        //Dialogs

        /* Create a dialogs for NFC disabled and androidBeam disabled cases*/
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if(mNfcAdapter!=null) {

            //Even if NFC hardware exists, may still need to prompt user to enable it
             /* Create dialog for NFC not enabled*/
            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage("Smart Pump requires NFC to be enabled. " +
                    "Click OK to go to NFC settings and enable this feature or clock Cancel to quit the app.")
                    .setTitle("NFC Disabled")
                    .setIcon(R.drawable.stat_notify_error)
                    .setCancelable(false); //back button doesn't cancel the dialog
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, close
                    // current activity
                    dialog.dismiss();
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS)); //allow user to turn nfc on

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, close
                    // current activity
                    dialog.dismiss();
                    startActivity(mainIntent);
                }
            });

            // 3. Get the AlertDialog from create()
            nfcNotEnabledDialog = builder.create();

            /* Create dialog for NFC Push not enabled*/
            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage("Smart Pump requires AndroidBeam to be enabled. " +
                    "Click OK to go to NFC settings and enable this feature or clock Cancel to quit the app.")
                    .setTitle("AndroidBeam Disabled")
                    .setIcon(R.drawable.stat_notify_error)
                    .setCancelable(false); //back button doesn't cancel the dialog
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, close

                    dialog.dismiss();
                    startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS)); //allow user to turn androidBeam on

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, close
                    // current activity
                    dialog.dismiss();
                    startActivity(mainIntent);

                }
            });

            // 3. Get the AlertDialog from create()
            nfcPushNotEnabledDialog = builder.create();

        }



    ByteBuffer buffer = ByteBuffer.allocate(1);
    byte newResult = (byte) received_value;
    buffer.put(newResult);
    byte[] result = buffer.array();


    myRecord = NdefRecord.createExternal("www.nfc.com", type, result); //create a record
    mNdefMessage = new NdefMessage(myRecord); //create a message
    mNfcAdapter.setNdefPushMessage(mNdefMessage, this);
    mNfcAdapter.setOnNdefPushCompleteCallback(this, this);



    mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);




    }

    public void onNdefPushComplete( NfcEvent arg0) {

        mNfcAdapter.setNdefPushMessage(null, this);
        messageSent = true; //message has been sent, start listening for reply
    }

    public void onNewIntent(Intent intent) {
        if(messageSent) { //if we have already delivered our request start processing tags
           resolveIntent(intent);
        }
    }

    private void resolveIntent(Intent intent){
        String action = intent.getAction();
        boolean matchFound = false;
        NdefRecord record = null;
     //   user_selection.setText("Got Intent"); //
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) { //check if the intent was caused by an NDEF message, if not drop this intent
            NdefMessage[] msgs; //array for storing NdefMessages after parsing
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES); //get NDEF Messages from Intent
                if (rawMsgs != null) { //if the data from intent is actually Ndef Formated
                    msgs = new NdefMessage[rawMsgs.length];
                    for (int i = 0; i < rawMsgs.length; i++) { //parse the data into NdefMessages
                        msgs[i] = (NdefMessage) rawMsgs[i];
                    }

                //Look through all messages to find the relevant message
                    for(int i = 0; i < msgs.length; i++) { //go through all Ndef messages
                        record = msgs[i].getRecords()[0]; //only need to check the first record
                        if (checkForMatch(record)) {
                            matchFound = true;
                            break;
                        }
                    }

                    if(record!=null && matchFound){

                        byte[] response_data = Arrays.copyOf(record.getPayload(), record.getPayload().length); //make local copy of data



                        float processedData = (float)response_data[0] + ((float)response_data[1])/100;


                        // create an intent
                        intent = new Intent(this, ConfirmationReceivedActivity.class);

                        //Create bundle to hold information for Confirmation Activity
                        Bundle extras = new Bundle();
                        if(received_type == 0) { //if the user has selected liters
                            extras.putFloat(AMOUNT, (float)received_value); // amount comes from WheelEntryActivity
                            extras.putFloat(COST, processedData);           // cost comes from pump reply
                        }
                        else{                   // if the user has selected GBP
                            extras.putFloat(AMOUNT, processedData);         // amount comes from pump reply
                            extras.putFloat(COST, (float)received_value);   // cost comes from WheelEntryActivity

                        }
                        //Start Confirmation Activity
                        intent.putExtras(extras);
                        startActivity(intent);

                    }

                }

        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mNfcAdapter!=null) // If NFC is supported
            mNfcAdapter.disableForegroundDispatch(this); // Allow other apps to respond to NFC intents when this app is not in foreground
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mNfcAdapter!=null) { // If NFC is supported
            if(!mNfcAdapter.isEnabled()) //if NFC is not enabled
                nfcNotEnabledDialog.show();
            else if(!mNfcAdapter.isNdefPushEnabled()) //if androidBeam is not enabled
                nfcPushNotEnabledDialog.show();
            else //if NFC is fully functional
                mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null); // Redirect all NFC intents to this app when in foreground state
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Must kill alert dialog when activity is stopped to prevent window leaks
        if(nfcNotEnabledDialog != null)
            nfcNotEnabledDialog.dismiss();
        if(nfcPushNotEnabledDialog != null)
            nfcPushNotEnabledDialog.dismiss();
    }


    boolean checkForMatch(NdefRecord record){

        boolean matchFound;
        boolean legalType = false;
        boolean expectedResponseType = false;
        boolean payloadLengthOk = false;


        //check that record is coming from pump
        if(record.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE) { //check that this is an external type message
            //Convert Type field to string and parse
            String typeField = new String(record.getType(), StandardCharsets.UTF_8);
            String delims = "[:]+"; //split the type field into domain name and actual type
            String[] typeFieldTokens = typeField.split(delims);

            if (typeFieldTokens.length == 2 && typeFieldTokens[0].equals(domain)) { //check that we only had two type fields and the domain name is correct
                switch (typeFieldTokens[1]) {
                    case amount_type:
                        legalType = true;
                        break;
                    case cost_type:
                        legalType = true;
                        break;
                    case error_type:
                        legalType = true;
                        break;
                    default:
                        legalType = false;
                        break;
                }
            }
            //check that length of payload is legal
            if (record.getPayload().length == EXP_PAYLOAD_LENGTH){
                payloadLengthOk = true;

            }

            //check that the response is of the same type as the request or is an error
            if (typeFieldTokens[1].equals(responseType) || typeFieldTokens[1].equals(error_type))
                expectedResponseType = true;
        }

        matchFound = legalType && payloadLengthOk && expectedResponseType;
        return matchFound;
    }


    private static int bytesToInteger(byte[] bytes) {
        int size = bytes.length;
        if (size == 2) return ((bytes[1] & 0xFF) << 8) | bytes[0] & 0xFF;
        else return bytes[0] & 0xFF;
    }





}
