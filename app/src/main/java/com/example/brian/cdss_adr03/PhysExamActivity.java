package com.example.brian.cdss_adr03;

import android.app.AlertDialog;
import android.app.Activity;
import android.app.ActionBar;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class PhysExamActivity extends ActionBarActivity {

    static String EHNFile = "EHN.txt";              // Name of the EHN file for record-keeping
                                                    // TODO Actually name it, once we have Pt Info
    static File file;                               // Actual file we're writing to on device. Will
                                                    // be passed through all Activities that write
    static String PtECGFile = "28042015_8080_hi.txt";
    static String CurrentPtLogFile;                 // patient file for current patient. ie
                                                    // for the prescription form.
    static String CurrentPtMHN = "";                // medical history note for the current patient
    static String CurrentPtEHN = "";                // examination history note for current patient

    static ArrayList<ArrayList<String>> physExamAns = new ArrayList<ArrayList<String>>();

    static ArrayList<String> examHisNote = new ArrayList<String>();

    // Information collected from the patient in MainActivity
    private String status;
    private String regNo;
    private String name;
    private String phone;
    private int    age;
    private String gender;
    private String sdw;
    private String occup;
    private String address;
    private int height;
    private int weight;
    private double bmi;
    private int pulse;
    private int bpSys;
    private int bpDia;
    private float temp;
    private int spo2;

    // Information collected from patient in MedHistoryActivity
    static ArrayList<String> prevMedHis = new ArrayList<String>();
    static ArrayList<String> prevFamHis = new ArrayList<String>();
    static ArrayList<String> prevRxLab = new ArrayList<String>();

    // Information collected from patient in AdaptiveHistoryActivity
    static ArrayList<String> medHisNote = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_physexam);

        // Collect information from previous Activity
        CurrentPtLogFile = getIntent().getStringExtra("pt-log-file");
        CurrentPtMHN = getIntent().getStringExtra("pt-mhn-file");
        CurrentPtEHN = getIntent().getStringExtra("pt-ehn-file");
        status = getIntent().getStringExtra("status");
        regNo = getIntent().getStringExtra("regNo");
        name = getIntent().getStringExtra("name");
        phone = getIntent().getStringExtra("phone");
        age = getIntent().getIntExtra("age", 0);
        gender = getIntent().getStringExtra("gender");
        sdw = getIntent().getStringExtra("sdw");
        occup = getIntent().getStringExtra("occup");
        address = getIntent().getStringExtra("address");
        height = getIntent().getIntExtra("height", 0);
        weight = getIntent().getIntExtra("weight", 0);
        bmi = getIntent().getDoubleExtra("bmi", 0);
        pulse = getIntent().getIntExtra("pulse", 0);
        bpSys = getIntent().getIntExtra("bpSys", 0);
        bpDia = getIntent().getIntExtra("bpDia", 0);
        temp = getIntent().getFloatExtra("temp", 0);
        spo2 = getIntent().getIntExtra("spo2", 0);
        prevMedHis = getIntent().getStringArrayListExtra("prev-med-history");
        prevFamHis = getIntent().getStringArrayListExtra("prev-fam-history");
        prevRxLab = getIntent().getStringArrayListExtra("prev-rx-lab");
        medHisNote = getIntent().getStringArrayListExtra("med-history-note");

        final ArrayList<ArrayList<String>> currentLoc = new ArrayList<ArrayList<String>>();

        // TODO David: This is where Button functionality is setup. Take these paragraphs,
        // TODO        copy once for each Button ID you have on the page, and make sure that the
        // TODO        the Strings (in green) that you pass to genPhysExamTask exactly match what
        // TODO        you have on the CSV (use the CSV that I have kept at the folder
        // TODO        res/raw/physexamitems.csv. This is what the app is actually reading.

        // Setup ChestButton
        final Button chest = (Button)findViewById(R.id.ChestButton);
        chest.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Chest");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time
                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, chest);
            }
        });

        // Setup SkinButton
        final Button skin = (Button)findViewById(R.id.SkinButton);
        skin.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Skin");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time
                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, skin);
            }
        });

        // Setup NeckButton
        final Button neck = (Button)findViewById(R.id.NeckButton);
        neck.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Neck");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time
                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, neck);
            }
        });

        // Setup LegandkneesButton
        final Button legandknees = (Button)findViewById(R.id.LegandKneesButton);
        legandknees.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Leg and knees");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time
                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, legandknees);
            }
        });

        // Setup EyesButton
        final Button eyes = (Button)findViewById(R.id.EyesButton);
        eyes.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Eyes");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time
                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, eyes);
            }
        });

        // Setup HandsButton
        final Button hands = (Button)findViewById(R.id.HandsButton);
        hands.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location {
                    genPhysExamTask(currentLoc, "Hands");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time
                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, hands);
            }
        });

        // Setup AbdomenButton
        final Button abdomen = (Button)findViewById(R.id.AbdomenButton);
        abdomen.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Abdomen");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time

                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, abdomen);
            }
        });

        // Setup HairButton
        final Button hair = (Button)findViewById(R.id.HairButton);
        hair.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Hair");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time

                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, hair);
            }
        });

        // Setup BackButton
        final Button back = (Button)findViewById(R.id.BackButton);
        back.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Back");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time

                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, back);
            }
        });

        // Setup FootButton
        final Button foot = (Button)findViewById(R.id.FootButton);
        foot.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Foot");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time

                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, foot);
            }
        });

        // Setup ThinkingandBalanceButton
        final Button thinkingandbalance = (Button)findViewById(R.id.ThinkingandBalanceButton);
        thinkingandbalance.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Thinking and Balance");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time

                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, thinkingandbalance);
            }
        });

        // Setup NondescriptButton
        final Button nondescript = (Button)findViewById(R.id.NondescriptButton);
        nondescript.setOnClickListener(new View.OnClickListener() { // Define behavior when clicked
            @Override
            public void onClick(View v) {
                if (currentLoc.isEmpty()) { // Fill currentLoc with Items only once per location
                    genPhysExamTask(currentLoc, "Nondescript");
                }

                final ArrayList<String> currentItem = currentLoc.get(0);  // One Item at a time
                currentLoc.remove(0);   // With Item loaded, remove Item from list of Items to cover
                // This pushes the next Item to the top of the list

                askItems(currentLoc, currentItem, nondescript);
            }
        });

        final Button mainIntent = (Button)findViewById(R.id.DoneButton);
        mainIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent MainIntent = new Intent(v.getContext(), MainActivity.class);
                MainIntent.putExtra("pt-log-file", CurrentPtLogFile);
                MainIntent.putExtra("pt-mhn-file", CurrentPtMHN);
                MainIntent.putExtra("pt-ehn-file", CurrentPtEHN);
                MainIntent.putExtra("status", status);
                MainIntent.putExtra("regNo", regNo);
                MainIntent.putExtra("name", name);
                MainIntent.putExtra("phone", phone);
                MainIntent.putExtra("age", age);
                MainIntent.putExtra("gender", gender);
                MainIntent.putExtra("sdw", sdw);
                MainIntent.putExtra("occup", occup);
                MainIntent.putExtra("address", address);
                MainIntent.putExtra("height", height);
                MainIntent.putExtra("weight", weight);
                MainIntent.putExtra("bmi", bmi);
                MainIntent.putExtra("pulse", pulse);
                MainIntent.putExtra("bpSys", bpSys);
                MainIntent.putExtra("bpDia", bpDia);
                MainIntent.putExtra("temp", temp);
                MainIntent.putExtra("spo2", spo2);
                MainIntent.putStringArrayListExtra("prev-med-history", prevMedHis);
                MainIntent.putStringArrayListExtra("prev-fam-history", prevFamHis);
                MainIntent.putStringArrayListExtra("prev-rx-lab", prevRxLab);
                MainIntent.putStringArrayListExtra("med-history-note", medHisNote);
                MainIntent.putStringArrayListExtra("exam-history-note", examHisNote);
                startActivity(MainIntent);
                finish();
            }
        });
    }

    // The following functions should not need to be modified -- they are called by the Button

    // Generate and show the question-asking Dialog given a current Item to ask about
    // This function handles multiple Items by re-clicking the original calling Location
    // until the list of Items for a particular Location has been exhausted.
    private void askItems(final ArrayList<ArrayList<String>> currentLoc,
                          final ArrayList<String> ci, final Button button) {
        if (ci.size() > 6) {        // Anything passed in should be greater than 6 elements long
            AlertDialog.Builder itemAlert = new AlertDialog.Builder(PhysExamActivity.this);
            itemAlert.setTitle(ci.get(1));

            // How to display: If there is a job aid picture, we need to display it
            if (!ci.get(5).isEmpty()) {         // 6th element is where the job aid is kept, if any
                FrameLayout imageLay = new FrameLayout(PhysExamActivity.this);
                final ImageView image = new ImageView(PhysExamActivity.this);

                String jobAid = ci.get(5);
                int resID = getResources().getIdentifier(jobAid, "drawable", getPackageName());

                image.setImageResource(resID);
                imageLay.addView(image);
                itemAlert.setView(imageLay);
            }

            // How to answer: Depending on answers' configuration (Ok, Yes/No or list of ans)
            // We need to handle UI generation differently
            String firstAns = ci.get(6);        // 7th element is where the answers start
            switch (firstAns) {
                case "OK":
                    itemAlert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Depending on P/V/A/S/E, behavior here changes
                            String addMedia = ci.get(3);
                            switch (addMedia) {
                                case "P":       // Asked to take a photo

                                    break;
                                case "V":       // Asked to take a video recording

                                    break;
                                case "A":       // Asked to take an audio recording

                                    break;
                                case "S":       // Asked to take a stetho sound

                                    break;
                                case "E":       // Asked to do an ECG
                                    Intent sendIntent = new Intent("org.centum.android.ECG");
                                    sendIntent.putExtra("ECG-name", PtECGFile);

                                    startActivity(sendIntent);

                                    break;
                                default:

                                    break;
                            }
                        }
                    });
                    break;
                case "Yes":
                    itemAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Depending on P/V/A/S/E, behavior here changes
                            String addMedia = ci.get(3);
                            switch (addMedia) {
                                case "P":       // Asked to take a photo

                                    break;
                                case "V":       // Asked to take a video recording

                                    break;
                                case "A":       // Asked to take an audio recording

                                    break;
                                case "S":       // Asked to take a stetho sound

                                    break;
                                case "E":       // Asked to do an ECG

                                    break;
                                default:

                                    break;
                            }

                            examHisNote.add(ci.get(4).replace("_", "Yes"));

                            printToEHN(examHisNote);    // To be removed/relocated later
                        }
                    });
                    itemAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            examHisNote.add(ci.get(4).replace("_", "No"));
                        }
                    });
                    break;
                default:
                    final ArrayList<String> list = new ArrayList<String>();
                    for (int i = 6; i < ci.size(); i++)
                        list.add(ci.get(i));

                    itemAlert.setItems(list.toArray(new String[list.size()]),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    examHisNote.add(ci.get(4).replace("_", list.get(which)));
                                }
                            });
                    break;
            }
            itemAlert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (currentLoc.isEmpty())   // If last Item has been asked, remove Button from sight
                        button.setVisibility(View.GONE);
                    else                        // Otherwise, keep asking Items
                        button.performClick();
                }
            });
            itemAlert.show();
        }
        else {
            // Error, why is the ArrayList too small?
            CharSequence text = "Error! ArrayList is less than 7 objects long";
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getApplicationContext(), text, duration).show();
        }
    }

    // When a Button is clicked, it will send its Location to this function, which searches through
    // the CSV file for a match. When it finds the lines relevant to the assigned Location, it will
    // generate a list of questions + possible answers for us to display in the dialogs.
    private void genPhysExamTask (ArrayList<ArrayList<String>> peq, String location) {

        String cvsSplitBy = ",";    // Setup what character splits CSV lines (here, commas)
        String ansSplitBy = ";";    // Setup what character splits the ans apart (here, semicolon)

        String currentLoc = "";     // Temp container for keeping the "current Location" last read

        // Clear out anything from peq in case there are remnants
        peq.clear();

        // Set up file reading functionality
        InputStream is = getResources().openRawResource(R.raw.physexamitems);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        try {
            line = br.readLine();   // Skip first line of the CSV -- it contains headers, no data

            line = br.readLine();   // Read first line of real data (then move cursor to next line)

            // Enter loop: Read line, move to next line, and whatever has been read, we will process
            while (line != null) {
                String[] field = line.split(cvsSplitBy);    // Being a CSV line, we remove commas
                // and separate the items of each line

                // As we iterate line by line, keep the last seen Location in memory to excuse
                // subsequent lines which will be blank in Cell 1 but be of the same Location
                if (!field[0].isEmpty()){
                    currentLoc = field[0];
                }

                // Now we compare the last seen Location to the assigned Location
                if (currentLoc.equals(location)) {   // If they match
                    ArrayList<String> temp = new ArrayList<String>();   // Storage container

                    temp.add(field[1]); // Item goes into 1st slot of new ArrayList
                    temp.add(field[2]); // Question goes into 2nd slot
                    temp.add(field[3]); // Layer goes into the 3rd slot
                    temp.add(field[5]); // P/V/A/S/E goes into the 4th slot
                    // If nothing was here in the CSV, this adds an empty string
                    temp.add(field[6]); // History Note goes into 5th slot

                    if (field.length > 7) // If there is an example picture, put in 6th slot
                        temp.add(field[7]);
                    else                    // Otherwise add empty string
                        temp.add("");

                    String[] answers = field[4].split(ansSplitBy); // Finally, insert possible ans
                    for (String ans : answers)
                        temp.add(ans);

                    peq.add(temp);          // Add temp container into the main ArrayList
                }

                line = br.readLine();       // Read next line
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Currently a debug print, eventually will piece together an Examination History Note
    // to send to Meteor
    public void printToEHN(ArrayList toPrint) {
        String output = "";
        for (int i = 0; i < toPrint.size(); i++) {
            output += toPrint.get(i) + "; ";
        }
        Log.i("EHN Result: ", output);


//        File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS/Patients");
//        if (!folder.exists()) folder.mkdir();
//
//        file = new File(folder, EHNFile);
//
//        try
//        {
//            BufferedWriter writer= new BufferedWriter(new FileWriter(file, true));
//
//            for (int i = 0; i < toPrint.size(); i++) {
//                writer.append(toPrint.get(i).toString()).append("; ");
//            }
//            writer.append('\n');
//
//            // flush and close buffer when done?
//            writer.close();
//        }
//        catch(IOException e)
//        {
//            e.printStackTrace();
//        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
}
