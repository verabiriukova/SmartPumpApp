package com.nfcfuelpump.dsp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;


import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;


import kankan.wheel.widget.OnWheelClickedListener;
import kankan.wheel.widget.WheelView;

import kankan.wheel.widget.adapters.ArrayWheelAdapter;
import kankan.wheel.widget.adapters.NumericWheelAdapter;



public class WheelEntryActivity extends Activity {

    public final static String SELECTED_VALUE = "com.nfcfuelpump.dsp.SELECTED_VALUE";
    public final static String SELECTED_TYPE = "com.nfcfuelpump.dsp.SELECTED_TYPE";

    //Wheels
    private WheelView tens;
    private WheelView ones;
    private WheelView type;
    final private String types[] =
            new String[] {"Litres", "GBP"};

    //Dialogs
    private AlertDialog invalidAmountDialog;
    private AlertDialog nfcNotFoundDialog;
    private AlertDialog nfcNotEnabledDialog;
    private AlertDialog nfcPushNotEnabledDialog;
    //NFC
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            return;
        }

        // Layout Setup //
        setContentView(R.layout.activity_wheel_entry);

        // User prompt
        TextView user_instructions = (TextView) findViewById(R.id.textView);
        user_instructions.setText("Please select how much fuel you would like to purchase today");

        //Wheels
        setUpWheels();

        //Dialogs

        /* Create a dialog for invalid amount*/
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage("You have selected a zero amount")
                .setTitle("Invalid Input")
                .setIcon(R.drawable.stat_notify_error);
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        // 3. Get the AlertDialog from create()
        invalidAmountDialog = builder.create();



        // NFC //
        // Find NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        if(mNfcAdapter==null) { //If NFC is not supported, generate an alert dialog

        /* Create dialog for NFC not supported*/
            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage("NFC hardware not detected, click OK to close application")
                    .setTitle("No NFC")
                    .setIcon(R.drawable.stat_notify_error)
                    .setCancelable(false); //back button doesn't cancel the dialog
            builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, close
                    // current activity
                     dialog.dismiss();
                     finish();
                     return;
                }
            });

            // 3. Get the AlertDialog from create()
            nfcNotFoundDialog = builder.create();

        }

        else { //Even if NFC hardware exists, may still need to prompt user to enable it
             /* Create dialog for NFC not enabled*/
            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage("Smart Pump requires NFC to be enabled. " +
                    "Click OK to go to NFC settings and enable this feature or click Cancel to quit the app.")
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

                    finish();
                    return;
                }
            });

            // 3. Get the AlertDialog from create()
            nfcNotEnabledDialog = builder.create();

            /* Create dialog for NFC Push not enabled*/
            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage("Smart Pump requires AndroidBeam to be enabled. " +
                    "Click OK to go to NFC settings and enable this feature or click Cancel to quit the app.")
                    .setTitle("AndroidBeam Disabled")
                    .setIcon(R.drawable.stat_notify_error)
                    .setCancelable(false); //back button doesn't cancel the dialog
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, close
                    // current activity
                    dialog.dismiss();
                    startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS)); //allow user to turn androidBeam on

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, close
                    // current activity
                    dialog.dismiss();
                    finish();
                    return;
                }
            });

            // 3. Get the AlertDialog from create()
            nfcPushNotEnabledDialog = builder.create();

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
        else nfcNotFoundDialog.show(); // If hardware is not supported
    }


    @Override
    protected void onStop() {
        super.onStop();
        // Must kill alert dialog when activity is stopped to prevent window leaks
        if(nfcNotFoundDialog != null)
            nfcNotFoundDialog.dismiss();
        if(invalidAmountDialog != null)
            invalidAmountDialog.dismiss();
        if(nfcNotEnabledDialog != null)
            nfcNotEnabledDialog.dismiss();
        if(nfcPushNotEnabledDialog != null)
            nfcPushNotEnabledDialog.dismiss();
    }


  /*  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wheel_entry, menu);
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
    }*/


    /* Handler for Done button*/

    public void entryDone(View DoneButton){ //called when the user presses the button

        int entered_value;
        int entered_type;
        Intent intent;


        entered_value = (tens.getCurrentItem())*10 + ones.getCurrentItem(); //read values set by wheels
        entered_type = type.getCurrentItem();
            if(entered_value != 0) {                                        //if legal value compose intent
            intent = new Intent(this, NfcTransferActivity.class);           //compose and issue an intent
            intent.putExtra(SELECTED_VALUE, entered_value);
            intent.putExtra(SELECTED_TYPE, entered_type);
            startActivity(intent);
        }
        else {

            invalidAmountDialog.show();                                    //otherwise show a warning dialog
        }



    }

    private void setUpWheels(){
        //Layout'y stuff for the wheels
        NumericWheelAdapter tensAdapter = new NumericWheelAdapter(this, 0, 9);
        tensAdapter.setTextSize(40);
        tensAdapter.setTextColor(Color.WHITE);
        NumericWheelAdapter onesAdapter = new NumericWheelAdapter(this, 0, 9);
        onesAdapter.setTextSize(40);
        onesAdapter.setTextColor(Color.WHITE);
        ArrayWheelAdapter typeAdapter = new ArrayWheelAdapter(this, types);
        typeAdapter.setTextSize(40);
        typeAdapter.setTextColor(Color.WHITE);

        //Find wheels in layout by id

        tens = (WheelView) findViewById(R.id.tens);
        tens.setViewAdapter(tensAdapter);
        tens.setVisibleItems(3);
        tens.setCyclic(true);

        ones = (WheelView) findViewById(R.id.ones);
        ones.setViewAdapter(onesAdapter);
        ones.setVisibleItems(3);
        ones.setCyclic(true);


        type = (WheelView) findViewById(R.id.type);
        type.setViewAdapter(typeAdapter);
        type.setVisibleItems(3);


        tens.setShadowColor(0xFF111111, 0x88111111 ,0x00AAAAAA);
        ones.setShadowColor(0xFF111111, 0x88111111 ,0x00AAAAAA);
        type.setShadowColor(0xFF111111, 0x88111111 ,0x00AAAAAA);
        tens.setDrawShadows(true);
        ones.setDrawShadows(true);
        type.setDrawShadows(true);

        tens.setCurrentItem(0);
        ones.setCurrentItem(0);
        type.setCurrentItem(0); //will start with whatever is located in string array at index 0

        //This allows values to be selected by clicking on the wheel rather than just scrolling
        OnWheelClickedListener click = new OnWheelClickedListener(){
            @Override
            public void onItemClicked(WheelView wheel, int itemIndex) {
                wheel.setCurrentItem(itemIndex, true);
            }
        };
        tens.addClickingListener(click);
        ones.addClickingListener(click);
        type.addClickingListener(click);



    }






}

   /*public static class InvalidEntryAlertDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Build the dialog and set up the button click handlers
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("invalid amount")
                    .setTitle("Error")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel(); //no action needed just kill the dialog
                        }
                    });
            return builder.create();
        }
    }*/

    /**
     * Adds changing listener for wheel that updates the wheel label
     * @param wheel the wheel
     * @param label the wheel label
     */
 /*   private void addChangingListener(final WheelView wheel, final String label) {
        wheel.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                wheel.setLabel(newValue != 1 ? label + "s" : label);
            }
        });
    }*/




/*package kankan.wheel.demo.extended;

        import java.util.Calendar;

        import kankan.wheel.demo.extended.R;
        import kankan.wheel.widget.OnWheelChangedListener;
        import kankan.wheel.widget.OnWheelClickedListener;
        import kankan.wheel.widget.OnWheelScrollListener;
        import kankan.wheel.widget.WheelView;
        import kankan.wheel.widget.adapters.NumericWheelAdapter;

        import android.app.Activity;
        import android.os.Bundle;
        import android.widget.TimePicker;

public class TimeActivity extends Activity {
    // Time changed flag
    private boolean timeChanged = false;

    // Time scrolled flag
    private boolean timeScrolled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.time_layout);

        final WheelView hours = (WheelView) findViewById(R.id.hour);
        hours.setViewAdapter(new NumericWheelAdapter(this, 0, 23));

        final WheelView mins = (WheelView) findViewById(R.id.mins);
        mins.setViewAdapter(new NumericWheelAdapter(this, 0, 59, "%02d"));
        mins.setCyclic(true);

        final TimePicker picker = (TimePicker) findViewById(R.id.time);
        picker.setIs24HourView(true);

        // set current time
        Calendar c = Calendar.getInstance();
        int curHours = c.get(Calendar.HOUR_OF_DAY);
        int curMinutes = c.get(Calendar.MINUTE);

        hours.setCurrentItem(curHours);
        mins.setCurrentItem(curMinutes);

        picker.setCurrentHour(curHours);
        picker.setCurrentMinute(curMinutes);

        // add listeners
        addChangingListener(mins, "min");
        addChangingListener(hours, "hour");

        OnWheelChangedListener wheelListener = new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                if (!timeScrolled) {
                    timeChanged = true;
                    picker.setCurrentHour(hours.getCurrentItem());
                    picker.setCurrentMinute(mins.getCurrentItem());
                    timeChanged = false;
                }
            }
        };
        hours.addChangingListener(wheelListener);
        mins.addChangingListener(wheelListener);

        OnWheelClickedListener click = new OnWheelClickedListener() {
            @Override
            public void onItemClicked(WheelView wheel, int itemIndex) {
                wheel.setCurrentItem(itemIndex, true);
            }
        };
        hours.addClickingListener(click);
        mins.addClickingListener(click);

        OnWheelScrollListener scrollListener = new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
                timeScrolled = true;
            }
            @Override
            public void onScrollingFinished(WheelView wheel) {
                timeScrolled = false;
                timeChanged = true;
                picker.setCurrentHour(hours.getCurrentItem());
                picker.setCurrentMinute(mins.getCurrentItem());
                timeChanged = false;
            }
        };

        hours.addScrollingListener(scrollListener);
        mins.addScrollingListener(scrollListener);

        picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker  view, int hourOfDay, int minute) {
                if (!timeChanged) {
                    hours.setCurrentItem(hourOfDay, true);
                    mins.setCurrentItem(minute, true);
                }
            }
        });
    }

    /**
     * Adds changing listener for wheel that updates the wheel label
     * @param wheel the wheel
     * @param label the wheel label
     */
  /*  private void addChangingListener(final WheelView wheel, final String label) {
        wheel.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                //wheel.setLabel(newValue != 1 ? label + "s" : label);
            }
        });
    }
}*/