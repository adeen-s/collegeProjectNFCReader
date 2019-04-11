package me.adeen.nfcreader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import me.adeen.nfcreader.models.Station;
import me.adeen.nfcreader.models.Trip;


public class MainActivity extends AppCompatActivity {

    boolean mWriteMode = false;
    private TextView mTextMessage;
    private TextView mStatus;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private AlertDialog dialog;
    private Button button;
    private Spinner stationSpinner;
    String uuid;
    DatabaseReference mDatabase;
    List<Station> stationsList;
    List<String> stationsStringList;
    ArrayAdapter<String> adapter;
    DatabaseReference balref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatus = (TextView) findViewById(R.id.status);
        mTextMessage = (TextView) findViewById(R.id.message);
        button = findViewById(R.id.button_read);
        stationSpinner = findViewById(R.id.stationsSpinner);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("stations");
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                stationsList = new ArrayList<>();
                stationsStringList = new ArrayList<>();
                for (DataSnapshot children : dataSnapshot.getChildren()) {
                    Station station = new Station(Objects.requireNonNull(children.getValue()).toString(), Integer.parseInt(Objects.requireNonNull(children.getKey())));
                    stationsList.add(station);
                }

                Collections.sort(stationsList, new Comparator<Station>(){
                    public int compare(Station o1, Station o2){
                        return o1.getKey() - o2.getKey();
                    }
                });

                for(int i = 0; i < stationsList.size(); i++) {
                    stationsStringList.add(stationsList.get(i).getName());
                }

                adapter = new ArrayAdapter<String>(
                        MainActivity.this, android.R.layout.simple_spinner_item, stationsStringList);

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                stationSpinner.setAdapter(adapter);
                stationSpinner.setSelection(0);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readTag();
            }
        });
    }

    protected void readTag() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(MainActivity.this);
        mNfcPendingIntent = PendingIntent.getActivity(MainActivity.this, 0,
                new Intent(MainActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        enableTagWriteMode();
        mWriteMode = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.Theme_Dialog)
                .setView(R.layout.my_dialog)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        disableTagWriteMode();
                    }

                });
        dialog = builder.create();
        dialog.show();
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        window.setAttributes(wlp);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onNewIntent(Intent intent) {
        // Tag reading mode
        if (!mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Ndef ndef = Ndef.get(detectedTag);
            try {
                ndef.connect();
                String str;
                byte[] mesg = new byte[0];
                try {
                    mesg = ndef.getNdefMessage().getRecords()[0].getPayload();
                    str = new String(mesg);
                    str = str.substring(3);
                    Log.d("TAG ", str);

                    mTextMessage.setText(str);
                    mStatus.setText("READING SUCCESSFUL");
                    mStatus.setTextColor(Color.GREEN);
                    Log.v("READING", str);
                    updateDatabase(str);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    dialog.cancel();
                    mStatus.setText("OPERATION FAILED");
                    mStatus.setTextColor(Color.RED);
                }
                dialog.cancel();
            } catch (IOException | FormatException e) {
                e.printStackTrace();
            }
        }
    }


    private void enableTagWriteMode() {
        mWriteMode = true;
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] mWriteTagFilters = new IntentFilter[]{tagDetected};
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
    }

    private void disableTagWriteMode() {
        mWriteMode = false;
        mNfcAdapter.disableForegroundDispatch(this);
    }

    private void updateDatabase(String number) {
        mDatabase = FirebaseDatabase.getInstance().getReference().child("tags").child(number);
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(MainActivity.this, "UNREGISTERED USER", Toast.LENGTH_SHORT).show();
                    mStatus.setText("UNREGISTERED");
                    mStatus.setTextColor(Color.RED);
                } else if(dataSnapshot.getValue().toString().equals("new")) {
                    Toast.makeText(MainActivity.this, "INCOMPLETE REGISTRATION", Toast.LENGTH_SHORT).show();
                    mStatus.setText("INCOMPLETE REGISTRATION");
                    mStatus.setTextColor(Color.RED);
                } else {
                    uuid = Objects.requireNonNull(dataSnapshot.getValue()).toString();
                    balref = FirebaseDatabase.getInstance().getReference().child("users").child(uuid).child("balance");
                    mDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(uuid).child("currenttrip");
                    mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                balref.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot balsnapshot) {
                                        int balance = Integer.parseInt(Objects.requireNonNull(balsnapshot.getValue()).toString());
                                        Trip trip = dataSnapshot.getValue(Trip.class);
                                        trip.setTo(stationSpinner.getSelectedItem().toString());
                                        trip.setTimeout(new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime()));
                                        int distanceunits = Math.abs(stationsStringList.indexOf(trip.getFrom()) - stationsStringList.indexOf(trip.getTo()));
                                        if (distanceunits < 2) {
                                            balance -= 5;
                                            trip.setCost(5);
                                        } else if(distanceunits < 5) {
                                            balance -= 10;
                                            trip.setCost(5);
                                        } else if(distanceunits < 10) {
                                            balance -= 15;
                                            trip.setCost(5);
                                        } else {
                                            balance -= 20;
                                            trip.setCost(5);
                                        }
                                        balref.setValue(balance);
                                        mDatabase.removeValue();
                                        mDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(uuid).child("trips").push();
                                        mDatabase.setValue(trip);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                                });
                            } else {
                                balref.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot balancesnapshot) {
                                        int balance = Integer.parseInt(Objects.requireNonNull(balancesnapshot.getValue()).toString());
                                        Log.d("MainActivity", "onDataChange: BALANCE--> " + balance);
                                        if (balance < 20) {
                                            Toast.makeText(MainActivity.this, "INSUFFICIENT BALANCE", Toast.LENGTH_SHORT).show();
                                            mStatus.setText("INSUFFICIENT BALANCE");
                                            mStatus.setTextColor(Color.RED);
                                        } else {
                                            Trip trip = new Trip(stationSpinner.getSelectedItem().toString(), new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime()), new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime()));
                                            mDatabase.setValue(trip);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

    }
}
