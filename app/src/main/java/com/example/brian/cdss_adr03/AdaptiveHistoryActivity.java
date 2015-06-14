package com.example.brian.cdss_adr03;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;


public class AdaptiveHistoryActivity extends Activity {

    private static final String TAG = AdaptiveHistoryActivity.class.getSimpleName();
    final Context context = this;

    // File Names
    static String cvsSplitBy = ",";
    static int CondDBFile = R.raw.lindadb20015;     // Adaptive Condition Database
    // Not used, but you should know where we're
    // pulling the questions from
    static int SymDBFile = R.raw.sahadb2;           // Non-adaptive Condition Database
    // Not used, but you should know where we're
    // pulling the questions from
    static String CurrentPtLogFile = "";            // patient file for current patient. ie
    // for the prescription form.
    static String AndroidLogFile = "Android File.txt";
    static String CurrentPtMHN = "";                // medical history note for the current patient
    static String CurrentPtEHN = "";                // examination history note for current patient
    static File file;                               // Actual file we're writing to on device. Will
                                                    // be passed through all Activities that write
    static boolean startedMHN;

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

    // Information collected from patient in PhysExamActivity
    static ArrayList<String> examHisNote = new ArrayList<String>();

    // Information to be collected from patient in this Activity
    static ArrayList<String> etcSymObservations = new ArrayList<String>();

    // Major Variables - Heavy Lifting Time

    // List of Symptoms to choose from (100% populated from database onCreate())
    //      Also used as a UI adapter, so it can contain less than the total list (be warned)
    static ArrayList<String> totalSym = new ArrayList<String>();

    // List of the Symptoms that Patient chooses (out of totalSym)
    //      Also copied back into totalSym (replacing the original) in order to pare down
    //      the number of relevant Symptoms we show the patient
    static ArrayList<String> presentingSym = new ArrayList<String>();

    // Containers for Conditions, Symptom and Attribute lists
    static ArrayList<Condition> Condlist = new ArrayList<Condition>();
    static ArrayList<Symptom> Symlist = new ArrayList<Symptom>();
    static ArrayList<Attribute> Attrilist = new ArrayList<Attribute>();

    // Display Attributes (all questions and possible answers for one Symptom)
    static ArrayList<ArrayList<String>> displayAttriList = new ArrayList<ArrayList<String>>();
    // Split up displayAttriList into the main Attributes list and its children values
    static ArrayList<String> displayAttriNames = new ArrayList<String>();
    static ArrayList<String> currentValues = new ArrayList<String>();

    // Store returned attribute Values (as returned from patient)
    static ArrayList<ArrayList<String>> chosenAttriList = new ArrayList<ArrayList<String>>();

    // For differential diagnosis
    static ArrayList<String> otherSym = new ArrayList<String>(); // additional symptoms to ask
    static ArrayList<String> nopresentingSym = new ArrayList<String>();// symptoms the Pt does NOT have

    // For tracking risk factors
    static ArrayList<String> yesRiskFactor = new ArrayList<String>();

    // examinations and tests for after initial diagnosis (ie ECG or physical examinations)
    static ArrayList<String> ExamsTestsToPerform = new ArrayList<String>();

    // Minor Variables - used for iterating, temporary storage, etc
    //ArrayList<String> tempChosenAttriListrow = new ArrayList<String>();
    //int attrivalueAdded = 0;
    //int attriAdded = 0;
    String prevSymConsidered = "";
    // Temporarily logs the patient's selections of answers to Attribute questions
    final ArrayList<String> checkedAns = new ArrayList<String>();
    // For navigating ArrayLists and building chosenAttriList
    private int qPos;
    private int qPosTemp;
    private String thisSym = "";

    // These are for distinguishing the different modes of history-taking operation:
    // 1st: Symptoms not chosen yet (symChosen == 0)
    // 2nd: Symptoms chosen, but asking a rigid/non-adaptive set of questions (adaptiveSym == 0)
    // 3rd: Symptoms chosen, and asking an adaptive set of questions (adaptiveSym == 1)
    private int symChosen = 0;
    private int adaptiveSym = 0;
    private int alreadyRecorded = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adaptive_history);

        // Collect information from MainActivity (Patient entry screen)
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
        examHisNote = getIntent().getStringArrayListExtra("exam-history-note");

        // Clear out containers -- can sometimes contain old information between executions
        totalSym.clear();
        presentingSym.clear();
        etcSymObservations.clear();
        displayAttriList.clear();
        displayAttriNames.clear();
        currentValues.clear();
        Condlist.clear();
        Symlist.clear();
        Attrilist.clear();
        chosenAttriList.clear();
        otherSym.clear();
        nopresentingSym.clear();
        ExamsTestsToPerform.clear();

        // Reference clinical database to generate complete list of symptoms
        completeSymList(cvsSplitBy,totalSym);
        Collections.sort(totalSym);

        // Tell the file-writing functions we are starting a new activity, so overwrite the old
        // buffer file by passing False to the "append" argument of FileWriter
        startedMHN = false;

        // Initialize UI elements: attach Listview class to UI lists
        final ListView symDisplay = (ListView) findViewById(R.id.ptPresentingSym);
        final ListView attriDisplay = (ListView) findViewById(R.id.ptAttri);
        final ListView valueDisplay = (ListView) findViewById(R.id.ptValue);
        final EditText etcSymObsEntry = (EditText) findViewById(R.id.etcSymObservationsField);
        final Button nextToMoreSymptoms = (Button) findViewById(R.id.buttonNext);

        // Initialize adapter between ArrayList totalSym and Listview symDisplay
        final ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, totalSym);
        symDisplay.setAdapter(adapter1);
        symDisplay.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Initialize adapter between ArrayList displayAttriNames and Listview attriDisplay
        final ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, displayAttriNames);
        attriDisplay.setAdapter(adapter2);
        attriDisplay.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Initialize adapter between ArrayList currentValues and Listview valueDisplay
        final ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, currentValues);
        valueDisplay.setAdapter(adapter3);
        valueDisplay.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);


        // Start audio recording for quality-control purposes
        File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS/Recordings");
        if (!folder.exists()) folder.mkdir();
        String ptAudioFile = CurrentPtLogFile.substring(0, CurrentPtLogFile.lastIndexOf(".")) + ".mp3";
        final MediaRecorder recorder = new MediaRecorder();
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, ptAudioFile);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(folder.getPath() + "/" + ptAudioFile);
        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e){
            e.printStackTrace();
        }


        // Behavior when you click on an item in 1st Listview
        symDisplay.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            SparseBooleanArray sp;

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

                if (!thisSym.isEmpty()) {
                    if (!currentValues.isEmpty()&& alreadyRecorded==0) {
                        ArrayList<String> tempList = new ArrayList<String>();   // temporary container

                        tempList.add(displayAttriNames.get(qPos));              // add question
                        tempList.add(displayAttriList.get(qPos).get(1));        // add history note
                        for (int i = 0; i < checkedAns.size(); i++)             // add answers item-wise
                            tempList.add(checkedAns.get(i));

                        chosenAttriList.add(tempList);                  // Add question and answer pair
                        Log.i(TAG, "chosen attribute list" + chosenAttriList.toString());
                        alreadyRecorded=1;
                        adapter3.clear();
                        adapter3.notifyDataSetChanged();
                        // Log the chosen Attribute list and send it to calcMatchedAttriScore
                        selectedAttriList(thisSym, chosenAttriList, CurrentPtLogFile);
                        // Calculate the Symptom score (of the last selected Symptom)
                        if (totalSym.contains("Abdominal Pain"))
                            calcMatchedAttriScore(thisSym, Symlist, Attrilist, chosenAttriList);

                    }

                        }

                // New items will be generated for these existing attribute lists;
                // empty them now to avoid overloading ListView attriDisplay
                displayAttriList.clear();
                chosenAttriList.clear();
                displayAttriNames.clear();

                // Clear checkmarks on 2nd Listview before re-populating with new items
                attriDisplay.getCheckedItemPositions().clear();

                // New items are about to be generated for the existing answers list, empty now
                currentValues.clear();

                // Clear checkmarks on 3rd Listview before re-populating with new items
                valueDisplay.getCheckedItemPositions().clear();


                // Handle check/uncheck behavior, and remember the selected symptom
                // if it is being freshly checked (ON) by the user
                sp=symDisplay.getCheckedItemPositions();
                if (!sp.get(position))                  // Check item for unchecked state
                    sp.delete(position);                // Remove item if supposed to be unchecked
                else {
                    // Remember which symptom has been selected for other Listviews' use
                    prevSymConsidered = totalSym.get(position);
                    Log.i(TAG, "This Symptom: " + prevSymConsidered);   // debug use only
                }

                // Handle 3 different cases within one click listener:
                // 1st: During symptom selection mode, get presentingSym from totalSym
                // 2nd: During non-adaptive mode, straightforward to display Attri via Symptom
                // 3rd: During adaptive mode, add selected items from otherSym for all new questions

                // 1st Mode
                if (symChosen == 0) {
                    // rebuild presentingSym each time we tap -- keeps items in alphabetical order
                    presentingSym.clear();
                    // After sp is reconciled, rebuild presentingSym
                    for (int i = 0; i < sp.size(); i++)
                        presentingSym.add(totalSym.get(sp.keyAt(i)));
                }
                else {
                    // 2nd Mode
                    if (adaptiveSym == 0) {
                        // presentingSym should basically be the only items displayed in the 1st
                        // Listview now (as "totalSym"), so take each selected item directly to
                        // Generate non-adaptive list of attribute questions & answers
                        attriofPresentingSymptomNonAdaptive((String) (totalSym.get(position)),
                                cvsSplitBy, displayAttriList);
                    }
                    // 3rd Mode
                    else {
                        // presentingSym and otherSym should be the only items displayed in the 1st
                        // Listview now (as "totalSym"), so take each selected item directly to
                        // generate adaptive list of attribute questions and answers
                        attriofPresentingSymptom(
                                (String) (totalSym.get(position)), 1, Condlist, Symlist, Attrilist,
                                displayAttriList, ExamsTestsToPerform);
                    }

                    // displayAttriList is the resulting 2D ArrayList. Copy the first item of each
                    // ArrayList to make displayAttriNames, which fills in the 2nd Listview
                    for (int i = 0; i < displayAttriList.size(); i++)
                        displayAttriNames.add(displayAttriList.get(i).get(0));
                    // Following ArrayList creation, update Listview and tell it display new stuff
                    adapter2.notifyDataSetChanged();


                    // TODO Log in the medical history note which current Symptom we are on
                }

            }

        });

        // Behavior when you click on an item in 2nd Listview
        attriDisplay.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            SparseBooleanArray sp;

            // When symptom in ListView symEntry is clicked...
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                if (!currentValues.isEmpty()&&alreadyRecorded==0) {
                    ArrayList<String> tempList = new ArrayList<String>();   // temporary container

                    tempList.add(displayAttriNames.get(qPos));              // add question
                    tempList.add(displayAttriList.get(qPos).get(1));        // add history note
                    for (int i = 0; i < checkedAns.size(); i++)             // add answers item-wise
                        tempList.add(checkedAns.get(i));

                    chosenAttriList.add(tempList);                  // Add question and answer pair
                    Log.i(TAG, "chosen attribute list" + chosenAttriList.toString());

                    adapter3.clear();
                    adapter3.notifyDataSetChanged();
                    alreadyRecorded= 1;
                }
                // New items are about to be generated for the existing answers list, empty now
                currentValues.clear();

                // Clear checkmarks on 3rd Listview before re-populating with new items
                valueDisplay.getCheckedItemPositions().clear();

                // Handle racheck/uncheck behavior, and remember the selected attribute
                // if it is being freshly checked (ON) by the user
                sp=attriDisplay.getCheckedItemPositions(); // Debug with Log.i(TAG, sp.toString());
                if (!sp.get(position))                  // Check item for unchecked state
                    sp.delete(position);                // Remove item if supposed to be unchecked
                else {
                    // Remember which Attribute has been asked so we know who the answer belongs to
                    qPosTemp = position;
                    Log.i(TAG, "This Attribute: " + qPosTemp);   // debug use only
                }

                // Look inside the correct ArrayList within the big 2D ArrayList to retrieve the
                // possible answers corresponding to the Q we ask, and fill in the 3rd Listview
                for (int i = 2; i < displayAttriList.get(position).size(); i++)
                    currentValues.add(displayAttriList.get(position).get(i)); // skip 1st item (attri)
                // Following ArrayList creation, update Listview and tell it to display new stuff
                adapter3.notifyDataSetChanged();

                // TODO Log in the medical history note which current Attribute question we are on
            }

        });

        // Behavior when you click on an item in 3rd Listview
        valueDisplay.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            SparseBooleanArray sp;

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

                // New items are about to be generated for the temp answers list, empty now
                checkedAns.clear();

                String thisAns = currentValues.get(position);
alreadyRecorded=0;
                switch (thisAns) {
                    case "[Touch to Enter]":
                        AlertDialog.Builder medAlert = new AlertDialog.Builder(AdaptiveHistoryActivity.this);
                        // Set an EditText view to get user input
                        final EditText input = new EditText(AdaptiveHistoryActivity.this);
                        input.setHeight(90);
                        medAlert.setTitle("Please enter medications").setView(input); // Set fields

                        // Set buttons: log answers to file upon "OK"
                        medAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                checkedAns.add(input.getText().toString());
                            }
                        });
                        medAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                        medAlert.show();
                        break;

                    // Janky: Only using "Touch to Answer" for Diabetic blood sugar/HbA1C for now
                    case "[Touch to Answer]":
                        AlertDialog.Builder sugHbAlert = new AlertDialog.Builder(AdaptiveHistoryActivity.this);
                        // Set up two EditTexts to get user input
                        LinearLayout sugHbLay = new LinearLayout(AdaptiveHistoryActivity.this);
                        sugHbLay.setOrientation(LinearLayout.VERTICAL);
                        final TextView title1 = new TextView(AdaptiveHistoryActivity.this);
                        title1.setText("Blood Sugar");
                        sugHbLay.addView(title1);
                        final EditText input1 = new EditText(AdaptiveHistoryActivity.this);
                        input1.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        sugHbLay.addView(input1);
                        final TextView title2 = new TextView(AdaptiveHistoryActivity.this);
                        title2.setText("HbA1C");
                        sugHbLay.addView(title2);
                        final EditText input2 = new EditText(AdaptiveHistoryActivity.this);
                        input2.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        sugHbLay.addView(input2);

                        // Set fields
                        sugHbAlert.setTitle("Please enter last Blood Sugar and HbA1C readings");
                        sugHbAlert.setView(sugHbLay);

                        // Set buttons: log answers to file upon "OK"
                        sugHbAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String sugHbSumm = "Blood sugar: " + input1.getText().toString()
                                        + "; HbA1C: " + input2.getText().toString();
                                checkedAns.add(sugHbSumm);
                            }
                        });
                        sugHbAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                        sugHbAlert.show();
                        break;

                    case "[please enter duration]":
                        AlertDialog.Builder durAlert = new AlertDialog.Builder(AdaptiveHistoryActivity.this);

                        // Set up two Numberpickers to let us get qty and unit
                        LinearLayout durLay = new LinearLayout(AdaptiveHistoryActivity.this);
                        durLay.setOrientation(LinearLayout.HORIZONTAL);
                        durLay.setGravity(Gravity.CENTER);
                        final NumberPicker qty = new NumberPicker(AdaptiveHistoryActivity.this);
                        qty.setMinValue(1);
                        qty.setMaxValue(24);
                        durLay.addView(qty);
                        final NumberPicker units = new NumberPicker(AdaptiveHistoryActivity.this);
                        units.setMinValue(0);
                        units.setMaxValue(4);
                        units.setDisplayedValues( new String[] { "Hours", "Days", "Weeks",
                                "Months", "Years" } );
                        durLay.addView(units);

                        // Set fields
                        durAlert.setTitle("Please enter duration").setView(durLay);

                        // Set buttons: log answers to file upon "OK"
                        durAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                double days;
                                if (units.getValue() == 0) {
                                    days = qty.getValue() / 24.0;
                                } else if (units.getValue() == 2) {
                                    days = qty.getValue() * 7;
                                } else if (units.getValue() == 3) {
                                    days = qty.getValue() * 30;
                                } else if (units.getValue() == 4) {
                                    days = qty.getValue() *365;
                                } else {        // "Days" is the unit
                                    days = qty.getValue();
                                }
                                checkedAns.add(Double.toString(days));
                            }
                        });
                        durAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                        durAlert.show();
                        break;

                    case "[please enter date]":
                        // Setup date picker to figure out time passed since event
                        final Calendar oldDate = Calendar.getInstance();
                        final Calendar today = Calendar.getInstance();

                        DatePickerDialog.OnDateSetListener dob = new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                                  int dayOfMonth) {
                                oldDate.set(Calendar.YEAR, year);
                                oldDate.set(Calendar.MONTH, monthOfYear);
                                oldDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                long diff = (today.getTimeInMillis() - oldDate.getTimeInMillis());

                                // handle error case
                                if (diff < 0) {
                                    CharSequence text = "Invalid Date selected! Please enter again";
                                    int duration = Toast.LENGTH_LONG;
                                    Toast.makeText(getApplicationContext(), text, duration).show();
                                }

                                // handle unit conversions (days, weeks, months, years)
                                int dayDiff = (int) diff / (24 * 60 * 60 * 1000);
                                checkedAns.add("Duration: " + dayDiff + " Days");
                            }
                        };

                        new DatePickerDialog(AdaptiveHistoryActivity.this, dob, oldDate
                                .get(Calendar.YEAR), oldDate.get(Calendar.MONTH),
                                oldDate.get(Calendar.DAY_OF_MONTH)).show();
                        break;

                    case "[please enter how many times/day]":
                        AlertDialog.Builder freqAlert = new AlertDialog.Builder(AdaptiveHistoryActivity.this);

                        // Set up two Numberpickers to let us get qty and unit
                        LinearLayout freqLay = new LinearLayout(AdaptiveHistoryActivity.this);
                        freqLay.setOrientation(LinearLayout.HORIZONTAL);
                        freqLay.setGravity(Gravity.CENTER);
                        final NumberPicker num = new NumberPicker(AdaptiveHistoryActivity.this);
                        num.setMinValue(1);
                        num.setMaxValue(10);
                        freqLay.addView(num);
                        final NumberPicker perUnit = new NumberPicker(AdaptiveHistoryActivity.this);
                        perUnit.setMinValue(0);
                        perUnit.setMaxValue(2);
                        perUnit.setDisplayedValues(new String[] { "per Day", "per Week", "per Month" });
                        freqLay.addView(perUnit);

                        // Set fields
                        freqAlert.setTitle("Please enter frequency").setView(freqLay);

                        // Set buttons: log answers to file upon "OK"
                        freqAlert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                double times;
                                if (perUnit.getValue() == 1) {
                                    times = num.getValue() / 7.0;
                                } else if (perUnit.getValue() == 2) {
                                    times = num.getValue() / 30.0;
                                } else {        // "Days" is the unit
                                    times = num.getValue();
                                }
                                checkedAns.add(Double.toString(times));
                            }
                        });
                        freqAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                        freqAlert.show();
                        break;

                    default:
                        sp = valueDisplay.getCheckedItemPositions();
                        if (!sp.get(position))                  // Check item for unchecked state
                            sp.delete(position);                // Remove item if supposed to be unchecked
                        for (int i = 0; i < sp.size(); i++)
                            checkedAns.add(currentValues.get(sp.keyAt(i)));
                        break;
                }

                // Lock the Symptom and Attribute question being considered so that when we click
                // somewhere else and shift focus, we remember the old values, allowing us to
                // calculate scores while the UI handles the next user input
                qPos = qPosTemp;
                thisSym = prevSymConsidered;
            }
        });

        // Behavior when you click elsewhere (away) from 3rd Listview -- log results
        valueDisplay.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

            }
        });

        nextToMoreSymptoms.setOnClickListener(new View.OnClickListener() {

            // Handle 4 different cases within one click listener:
            // 1st: During symptom selection mode, get RAMDB setup using presentingSym
            // 2nd/3rd: At the end of asking Attributes/Values, calculate and display otherSym,
            //     the list of other possible Symptoms we should ask the patient
            // 4th: if otherSym is empty (CDSS has hit the extent of its knowledge), move on!
            @Override
            public void onClick(View view) {
                // You shouldn't be at the CDSS for New Symptoms if you have no symptoms...
                if (presentingSym.isEmpty() && symChosen == 0) {
                    // Stop audio recording -- we're done for now
                    recorder.stop();

                    // Now that we're done, let's copy the other/etc entries into memory
                    etcSymObservations.add(etcSymObsEntry.getText().toString());

                    Intent MainIntent = new Intent(view.getContext(), MainActivity.class);
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
                // 1st Mode
                else if (symChosen == 0) {
                    // This is the 1st Mode, so we're choosing the symptoms now and won't be able to
                    // go back. Once symptoms are chosen, back-search for all possible conditions
                    // and load them into our diagnostic possibilities.

                    // Reset 1st Listview
                    symDisplay.getCheckedItemPositions().clear();
                    // Reset totalSym to display only items from presentingSym
                    totalSym.clear();
                    for (int i = 0; i < presentingSym.size(); i++)
                        totalSym.add(presentingSym.get(i));
                    // Following ArrayList update, update Listview and tell it to display new stuff
                    adapter1.notifyDataSetChanged();
                    if (totalSym.contains("Abdominal Pain")) {
                        matchSymandgenRAMDB(presentingSym, cvsSplitBy, Condlist, Symlist, Attrilist);

                        // Assess vitals' information to start consideration of conditions
                        if (age != 0)
                            calcVitalrange("Age", age, Symlist, Attrilist);
                        if (!gender.isEmpty())
                            calcVitalmatch("Gender", gender, Symlist, Attrilist);
                        if (pulse != 0)
                            calcVitalrange("Heart Rate", pulse, Symlist, Attrilist);
                        if (temp != 0)
                            calcVitalrange("Temperature", temp, Symlist, Attrilist);
                        if (spo2 != 0)
                            calcVitalrange("SpO2", spo2, Symlist, Attrilist);
                        for (int i = 0; i < prevMedHis.size(); i++) {
                            calcVitalmatch(prevMedHis.get(i), "Yes", Symlist, Attrilist);
                        }
                    }

                    // Exit 1st Mode. Now we go to 2nd Mode, the non-adaptive questioning
                    symChosen = 1;

                    printToLog(CurrentPtLogFile,
                            "HA chose following Symptoms:", presentingSym);

                    // Display a quick Dialog to instruct the HA to go back and now ask questions
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Now, please select Symptoms to see questions to ask Patient")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    // create alert dialog
                    AlertDialog AlertAskQsDialog = builder.create();

                    // show it
                    AlertAskQsDialog.show();
                }
                // 2nd, 3rd and 4th Mode
                else {
                    // Shared between 2nd and 3rd modes:

                    // Just in case there is a race condition between pressing Next (gain focus)
                    // and losing focus from the Values list, we should calculate one more time
                    // the Attribute scores of the last selected Symptom.. pretty sure not this one..
                    if (!currentValues.isEmpty()&&alreadyRecorded==0) {
                        ArrayList<String> tempList = new ArrayList<String>();   // temporary container

                        tempList.add(displayAttriNames.get(qPos));              // add question
                        tempList.add(displayAttriList.get(qPos).get(1));        // add history note
                        for (int i = 0; i < checkedAns.size(); i++)             // add answers item-wise
                            tempList.add(checkedAns.get(i));

                        chosenAttriList.add(tempList);                  // Add question and answer pair
                        Log.i(TAG, "chosen attribute list added last item when pressed next" + chosenAttriList.toString());
                        alreadyRecorded=1;

                        selectedAttriList(thisSym, chosenAttriList, CurrentPtLogFile);

                        if (totalSym.contains("Abdominal Pain"))
                        calcMatchedAttriScore(thisSym, Symlist, Attrilist, chosenAttriList);
                    }

                  //  selectedAttriList(thisSym, chosenAttriList, CurrentPtLogFile);
                     // Calculate the Symptom score (of the last selected Symptom)
                    Log.i(TAG, "second round of sorting");          // Just a friendly debug msg

                    if(totalSym.contains("Abdominal Pain")) {
                        Log.i(TAG, "weeeeeeeeeeeeeeeeeeeeeeeeee went into abdo pain");          // Just a friendly debug msg

                     //   calcMatchedAttriScore(thisSym, Symlist, Attrilist, chosenAttriList);

                        // Sum up the Symptoms' scores across all Conditions
                        sumUpSymScore(Condlist, Symlist);
                        // Calculate the score of each Condition
                        calcCondScore(Condlist);
                        // Sort the Conditions within the list in order of their score
                        Collections.sort(Condlist, new ConditionComparator());


                        // Just a bunch of friendly debug messages TODO fix all these to Android Log.i
                        ramDBprint(Condlist, Symlist, Attrilist, CurrentPtLogFile);

                        // Prepare to get otherSym (other presenting symptoms) and nopresentingSym
                        // (other symptoms that we asked, but patient isn't reporting) from Dialog
                        otherSym.clear();
                        nopresentingSym.clear();

                        // Generate list of other symptoms to ask
                        otherSymList(Condlist, Symlist, otherSym, presentingSym, yesRiskFactor);
                        if (!yesRiskFactor.isEmpty()) {
                            totalSym.add("Risk Factor");
                            yesRiskFactor.clear();
                        }
                        addSpecialCasesSym(Condlist, Symlist, otherSym);

                        // Reset 1st Listview
                        symDisplay.getCheckedItemPositions().clear();

                        // 4th Mode: at the end of generating otherSym, if it is empty then we are done
                        // asking questions, and will move on to calling doctor/physical examination
                        if (otherSym.isEmpty()) {
                            // Stop audio recording -- we're done for now
                            recorder.stop();

                            // Now that we're done, let's copy the other/etc entries into memory
                            etcSymObservations.add(etcSymObsEntry.getText().toString());

                            // Gather all the patient's presenting symptoms from the totalSym list
                            // and send to the Rx Form
                            presentingSym.clear();
                            for (int i = 0; i < totalSym.size(); i++) {
                                presentingSym.add(totalSym.get(i));
                            }

                            String tempPresSym = "C/O:,";
                            for (int i = 0; i < presentingSym.size(); i++) {
                                tempPresSym += presentingSym.get(i) + "; ";
                            }
                            tempPresSym += etcSymObservations.get(0).replaceAll(",", ";");
                            //    printtoptMHN(AndroidLogFile,tempPresSym);
                            // Print all medical history information collected into a single medical
                            // history note for the doctor to view
                            printtoptMHNCDSS(CurrentPtMHN, Condlist, Symlist, Attrilist);

                            Intent MainIntent = new Intent(view.getContext(), MainActivity.class);
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
                        // 2nd/3rd mode: in adaptive questioning mode, if otherSym still has symptoms
                        // we must ask, then we will ask them and return data to the 1st Listview upon
                        // selection of options in the Dialog
                        else {
                            // erase presentingSym in preparation for "new" symptoms
                            presentingSym.clear();

                            // Display "other symptoms" in a Dialog for the user to select:
                            // setup array to populate dialog's list
                            List<String> list = new ArrayList<String>(otherSym);
                            final String[] choices = list.toArray(new String[list.size()]);

                            // initialize an empty list for the Other Symptoms that the patient selects
                            final ArrayList<String> selectedItems = new ArrayList<String>();

                            // build AlertDialog to ask about otherSym
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            // Set the dialog title
                            builder.setTitle("Does the patient have any of the following symptoms?")
                                    // Specify the list array, the items to be selected by default (null for none),
                                    // and the listener through which to receive callbacks when items are selected
                                    .setMultiChoiceItems(choices, null, new DialogInterface.OnMultiChoiceClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                            // If the user checked the item, add it to the selected items
                                            if (isChecked) {
                                                selectedItems.add(choices[which]);
                                            } else {
                                                // This is a special case -- selectedItems doesn't like to
                                                // be empty for some reason
                                                if (selectedItems.size() == 1) {
                                                    selectedItems.clear();
                                                    // Else, if the item is already in the array, remove it
                                                } else {
                                                    selectedItems.remove(which);
                                                }
                                            }

                                        }
                                    })
                                            // Set the action buttons
                                    .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            // totalSym will contain a record of all presented symptoms;
                                            // presentingSym will just be the most current set
                                            for (int i = 0; i < selectedItems.size(); i++) {
                                                totalSym.add(selectedItems.get(i));
                                                presentingSym.add(selectedItems.get(i));
                                            }

                                            printToLog(CurrentPtLogFile,
                                                    "HA chose following Symptoms:", presentingSym);

                                            // when otherSym returns stuff, we incorporate the
                                            // data into the program logic
                                            setSymScore(otherSym, presentingSym, nopresentingSym, Symlist, Attrilist, yesRiskFactor);

                                            if (!yesRiskFactor.isEmpty() && !presentingSym.contains("Risk Factor")) {
                                                presentingSym.add("Risk Factor");
                                                yesRiskFactor.clear();
                                            } else if (!yesRiskFactor.isEmpty() && presentingSym.contains("Risk Factor")) {
                                                yesRiskFactor.clear();
                                            }

                                            printToLog(CurrentPtLogFile,
                                                    "HA did not choose following Symptoms:",
                                                    nopresentingSym);

                                            adapter1.notifyDataSetChanged();

                                            // What if none of the otherSym are selected?
                                            if (presentingSym.isEmpty()) {

                                                nextToMoreSymptoms.performClick();

                                            }


                                        }
                                    });

                            // create alert dialog
                            AlertDialog otherSymDialog = builder.create();
                            // show it
                            otherSymDialog.show();
                        }

                        // If we were in 2nd Mode, exit; now to go to 3rd mode, adaptive questioning
                        if (adaptiveSym == 0) {
                            adaptiveSym = 1;
                            Log.i("What is adaptiveSym?", Integer.toString(adaptiveSym));
                        }
                    }
                    else {
                        //add in all those typed stuff??
                        Intent MainIntent = new Intent(view.getContext(), MainActivity.class);
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


                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_adaptive_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void printToLog(String CurrentPtLogFile, String category,
                                  ArrayList toPrint) {

        File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS");
        if (!folder.exists()) folder.mkdir();

        file = new File(folder, CurrentPtLogFile);
        Log.i(TAG, file.toString());

        try
        {
            BufferedWriter writer= new BufferedWriter(new FileWriter(file, true));

            writer.append(category);
            writer.append(":,");
            for (int i = 0; i < toPrint.size(); i++) {
                writer.append(toPrint.get(i).toString());
                writer.append(",");
            }
            writer.append('\n');

            // flush and close buffer when done?
            writer.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }


    public static void printtoptMHN(String CurrentPtMHN, String toPrint) {
        File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS");
        if (!folder.exists()) folder.mkdir();

        File file = new File(folder, CurrentPtMHN);

        try
        {
            BufferedWriter writer= new BufferedWriter(new FileWriter(file, startedMHN));


            writer.append(toPrint).append('\n');
            writer.close();

            startedMHN = true;
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void printtoptMHNCDSS(String CurrentPtMHN,
                                        ArrayList<Condition> Condlist, ArrayList<Symptom> Symlist,
                                        ArrayList<Attribute> Attrilist) {
        File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS");
        if (!folder.exists()) folder.mkdir();

        file = new File(folder, CurrentPtLogFile);
        Log.i(TAG, file.toString());

        String linetoprint = "";
        String linetoprintsymnot = "Patient does not have: ";
        String linetoprintsymdidnotask = "These symptoms were not probed: ";

        String linetoprintattrididnotask = "--->Did not ask these questions: ";

        Symptom tempSym;
        Attribute tempAttri;
        try {
            BufferedWriter writer= new BufferedWriter(new FileWriter(file, true));

            writer.append('\n').append("-----").append('\n');
            writer.append("CDSS thinks it's most likely this condition: ");
            writer.append(Condlist.get(0).getname()).append('\n');
            writer.append("CDSS has considered the following conditions, in order of ranking:").append('\n');
            for (int i = 1; i < Condlist.size(); i++) {
                writer.append(Condlist.get(i).getname() + ",");
            }

            writer.append('\n').append('\n');
            writer.append("-------------------------------").append('\n');
            writer.append("CDSS's reasoning for each of the conditions it considered: ").append('\n');

            for (int i = 0; i < Condlist.size(); i++) {
                writer.append('\n');
                writer.append(Condlist.get(i).getname() + ":").append('\n');
                for (int j = Condlist.get(i).getsymbegin(); j <= Condlist
                        .get(i).getsymend(); j++) {
                    tempSym = Symlist.get(j);

                    if (tempSym.getrelevance() % 1 < 0.0002
                            || Math.abs(Math.abs(tempSym.getrelevance()) - 0.5) < 0.0001) {
                        if (tempSym.getpresent() > 0) {
                            linetoprint += "Patient has " + tempSym.getname()
                                    + " which matches this condition: ";
                            for (int k = tempSym.getattribegin(); k <= tempSym
                                    .getattriend(); k++) {

                                if (Attrilist.get(k).getrelevance() % 1 < 0.01) {
                                    tempAttri = Attrilist.get(k);
                                    if (tempAttri.getpresent() > 0) {
                                        linetoprint += (tempAttri
                                                .getvalueEntered().replaceAll(
                                                        ";", ",")).replaceAll("_",
                                                " to ")
                                                + tempAttri.gethistorynote()
                                                .replaceAll("_", " ")
                                                + " (matching possible answers for this condition: "
                                                + tempAttri.getvalue()
                                                .replaceAll(";", "/")
                                                + "), ";
                                    } else if (tempAttri.getpresent() == 0
                                            && tempAttri.getalreadyAsked() != 0) {
                                        linetoprint += (tempAttri
                                                .getvalueEntered().replaceAll(
                                                        ";", ",")).replaceAll("_",
                                                " to ")
                                                + tempAttri.gethistorynote()
                                                .replaceAll("_", " ")
                                                + " (matching possible answers for this condition: "
                                                + tempAttri.getvalue()
                                                .replaceAll(";", "/")
                                                + "), ";
                                    } else if (tempAttri.getpresent() < 0
                                            && tempAttri.getalreadyAsked() != 0) {
                                        linetoprint += (tempAttri
                                                .getvalueEntered().replaceAll(
                                                        ";", ",")).replaceAll("_",
                                                " to ")
                                                + tempAttri.gethistorynote()
                                                .replaceAll("_", " ")
                                                + " (does not match possible answers: "
                                                + tempAttri.getvalue()
                                                .replaceAll(";", "/")
                                                + "), ";
                                    } else {
                                        linetoprintattrididnotask += tempAttri
                                                .getname() + ", ";
                                    }
                                }
                            }
                            writer.append(linetoprint).append('\n');
                            if (!linetoprintattrididnotask
                                    .equals("--->Did not ask these questions: "))
                                writer.append(linetoprintattrididnotask).append('\n');
                            linetoprint = "";
                            linetoprintattrididnotask = "--->Did not ask these questions: ";
                        } else if (tempSym.getpresent() <= 0
                                && tempSym.getalreadyAsked() != 0) {
                            if (tempSym.getalreadyAsked() > 0) {
                                linetoprint += "Patient might or might not have "
                                        + tempSym.getname()
                                        + " that matches this condition: ";
                                for (int k = tempSym.getattribegin(); k <= tempSym
                                        .getattriend(); k++) {

                                    if (Attrilist.get(k).getrelevance() % 1 < 0.01) {
                                        tempAttri = Attrilist.get(k);
                                        if (tempAttri.getpresent() > 0) {
                                            linetoprint += tempAttri
                                                    .getvalueEntered()
                                                    .replaceAll(";", ",")
                                                    .replaceAll("_", "-")
                                                    + tempAttri
                                                    .gethistorynote()
                                                    .replaceAll("_",
                                                            " ")
                                                    + " (matching possible answers for this condition: "
                                                    + tempAttri.getvalue()
                                                    .replaceAll(";",
                                                            "/")
                                                    + "), ";
                                        } else if (tempAttri.getpresent() == 0
                                                && tempAttri.getalreadyAsked() != 0) {
                                            linetoprint += tempAttri
                                                    .getvalueEntered()
                                                    .replaceAll(";", ",")
                                                    .replaceAll("_", "-")
                                                    + tempAttri
                                                    .gethistorynote()
                                                    .replaceAll("_",
                                                            " ")
                                                    + " (matching possible answers for this condition: "
                                                    + tempAttri.getvalue()
                                                    .replaceAll(";",
                                                            "/")
                                                    + "), ";
                                        } else if (tempAttri.getpresent() < 0
                                                && tempAttri.getalreadyAsked() != 0) {
                                            linetoprint += tempAttri
                                                    .getvalueEntered()
                                                    .replaceAll(";", ",")
                                                    .replaceAll("_", "-")
                                                    + tempAttri
                                                    .gethistorynote()
                                                    .replaceAll("_",
                                                            " ")
                                                    + " (does not match possible answers: "
                                                    + tempAttri.getvalue()
                                                    .replaceAll(";",
                                                            "/")
                                                    + "), ";
                                        } else {
                                            linetoprintattrididnotask += tempAttri
                                                    .getname() + ", ";
                                        }
                                    }
                                }
                                writer.append(linetoprint).append('\n');
                                if (!linetoprintattrididnotask
                                        .equals("--->Did not ask these questions: "))
                                    writer.append(linetoprintattrididnotask).append('\n');
                                linetoprint = "";
                                linetoprintattrididnotask = "--->Did not ask these questions: ";

                            } else
                                linetoprintsymnot += tempSym.getname() + ", ";
                        } else {
                            linetoprintsymdidnotask += tempSym.getname() + ", ";
                        }
                    } else if (tempSym.getrelevance() % 1 > 0.05
                            && tempSym.getalreadyAsked() != 0) {
                        tempAttri = Attrilist.get(tempSym.getattribegin());
                        linetoprint = "Patient's " + tempSym.getname() + " is "
                                + tempAttri.getvalueEntered();

                        if (tempSym.getname().equals("Age")
                                || tempSym.getname().equals("Gender")) {
                            if (tempAttri.getrelevance() <= -0.00001) {
                                linetoprint += ", which is highly unlikely at risk for this condition";
                            } else if (tempAttri.getrelevance() >= -0.00001
                                    && tempAttri.getrelevance() <= 0.00001) {
                                linetoprint += ", which is not relevant for his/her risk for this condition";
                            } else if (tempAttri.getrelevance() > 0.00001
                                    && tempAttri.getrelevance() < 1.5) {
                                linetoprint += ", which is highly likely at risk for this condition";
                            } else if (tempAttri.getrelevance() >= 1.5) {
                                linetoprint += ", which is likely at risk for this condition";
                            }
                        } else if (tempSym.getname().equals("Pulse")) {
                            if (tempAttri.getrelevance() <= -0.00001) {
                                linetoprint += " and is highly unlikely at risk for this condition";
                            } else if (tempAttri.getrelevance() >= -0.00001
                                    && tempAttri.getrelevance() <= 0.00001) {
                                linetoprint += " and does not affect his/her risk for this condition";
                            } else if (tempAttri.getrelevance() > 0.00001
                                    && tempAttri.getrelevance() < 1.5) {
                                linetoprint += " and is highly likely at risk for this condition";
                            } else if (tempAttri.getrelevance() >= 1.5) {
                                linetoprint += " and is likely at risk for this condition";
                            }
                        } else if (tempSym.getname().equals("Temperature")) {
                            if (tempAttri.getrelevance() <= -0.00001) {
                                linetoprint += " and is highly unlikely for this condition";
                            } else if (tempAttri.getrelevance() >= -0.00001
                                    && tempAttri.getrelevance() <= 0.00001) {
                                linetoprint += " and does not matter for this condition";
                            } else if (tempAttri.getrelevance() > 0.00001
                                    && tempAttri.getrelevance() < 1.5) {
                                linetoprint += " and is highly likely for this condition";
                            } else if (tempAttri.getrelevance() >= 1.5) {
                                linetoprint += " and is likely for this condition";
                            }
                        }
                        writer.append(linetoprint).append('\n');
                        linetoprint = "";
                    }

                }
                if (!linetoprintsymnot.equals("Patient does not have: "))
                    writer.append(linetoprintsymnot).append('\n');
                if (!linetoprintsymdidnotask
                        .equals("These symptoms were not probed: "))
                    writer.append(linetoprintsymdidnotask).append('\n');
                linetoprintsymnot = "Patient does not have: ";
                linetoprintsymdidnotask = "These symptoms were not probed: ";
            }

        } catch (IOException e) {
            // exception handling left as an exercise for the reader
        }
    }

    public void completeSymList(String cvsSplitBy, ArrayList<String> totalSym) {
        // System.out.println("Possible Symptoms from Database: ");
        InputStream is = getResources().openRawResource(SymDBFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            line = br.readLine();
            line = br.readLine();
            String[] field = line.split(cvsSplitBy);
            totalSym.add(field[0]);
            // System.out.println(field[2]);
            int symrep = 0;

            while ((line = br.readLine()) != null) {
                field = line.split(cvsSplitBy);
                if (!field[0].equals("")) {
                    for (String str : totalSym) {
                        if (field[0].equals(str)) {
                            symrep = 1;
                            break;
                        }
                    }
                    if (symrep == 0) {
                        // System.out.println(field[2]);
                        totalSym.add(field[0]);
                    } else
                        symrep = 0;
                }
            }
            br.close();
        }

        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void matchSymandgenRAMDB(ArrayList tempSym, String cvsSplitBy,
                                    ArrayList<Condition> Condlist, ArrayList<Symptom> Symlist,
                                    ArrayList<Attribute> Attrilist) {

        InputStream is = getResources().openRawResource(CondDBFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        String tempCondline = "";
        String tempCondname;
        int tempCondrank;
        String tempSymname;
        float tempSymrelevance;

        int temptotalprimaryattri = 0;
        int temptotalprimarysym = 0;

        int attriCntLow = 0;
        int tempSympresent = 0;
        int symCntLow = 0;

        try {
            line = br.readLine();
            // System.out.println(line);
            line = br.readLine();
            // System.out.println(line);

            while (line != null) {
                String[] field = line.split(cvsSplitBy);

                if (!field[0].equals("")) {
                    br.mark(0);
                    tempCondline = line;
                }
                // Marks the current position in this input stream. A subsequent
                // call to the reset method repositions this stream at the last
                // marked position so that subsequent reads re-read the same
                // bytes.
                // The readlimit argument tells this input stream to allow that
                // many bytes to be read before the mark position gets
                // invalidated.

                if (tempSym.contains(field[2])) {
                    br.reset();
                    field = tempCondline.split(cvsSplitBy);
                    tempCondname = field[0];
                    tempCondrank = Integer.parseInt(field[1]);
                    tempSymname = field[2];
                    tempSymrelevance = Float.parseFloat(field[3]);

                    Attrilist.add(new Attribute(field[4], field[5], Float
                            .parseFloat(field[6]), field[7], field[8], Symlist
                            .size()));
                    if (Math.abs(Float.parseFloat(field[6]) - 1) < 0.05)
                        temptotalprimaryattri++;
                    line = br.readLine();
                    if (line != null)
                        field = line.split(cvsSplitBy);

                    while (field[0].equals("") && line != null) {
                        if (!field[2].equals("")) {
                            if (tempSym.contains(tempSymname)) {
                                // System.out.println("found a match!: " +
                                // tempSymname);
                                tempSympresent = 1;
                                if (Attrilist.get(attriCntLow).getrelevance() % 1 > 0.05) {
                                    Attrilist.get(attriCntLow)
                                            .changealreadyAsked(1);
                                    Attrilist.get(attriCntLow)
                                            .changevalueEntered("Yes");
                                    Attrilist.get(attriCntLow).changepresent(1);

                                    if (Attrilist.get(attriCntLow).getvalue()
                                            .contains(";")) {
                                        String[] evalsymyesno = (Attrilist.get(
                                                attriCntLow).getvalue()
                                                .split(";"));
                                        for (int o = 0; o < evalsymyesno.length; o++) {
                                            if (evalsymyesno.equals("Yes")) {
                                                tempSymrelevance = Float
                                                        .parseFloat(Attrilist
                                                                .get(attriCntLow)
                                                                .getvaluespecificrelevance()
                                                                .split(";")[o]);
                                            }
                                        }
                                    } else if (Attrilist.get(attriCntLow)
                                            .getvalue().equals("No")) {
                                        tempSymrelevance = -(Float
                                                .parseFloat(Attrilist
                                                        .get(attriCntLow)
                                                        .getvaluespecificrelevance()));
                                    }
                                }

                            } else
                                tempSympresent = 0;
                            Symlist.add(new Symptom(tempSymname,
                                    tempSymrelevance, tempCondname,
                                    attriCntLow, Attrilist.size() - 1,
                                    temptotalprimaryattri, tempSympresent));
                            if (Math.abs(Math.abs(tempSymrelevance) - 1) < 0.76
                                    || Math.abs(Math.abs(tempSymrelevance) - 1.1) < 0.76)
                                temptotalprimarysym++;
                            tempSymname = field[2];
                            // System.out.println(tempSymname + " _ "
                            // + tempCondname);
                            tempSymrelevance = Float.parseFloat(field[3]);
                            attriCntLow = Attrilist.size();
                            temptotalprimaryattri = 0;
                        }
                        Attrilist.add(new Attribute(field[4], field[5], Float
                                .parseFloat(field[6]), field[7], field[8],
                                Symlist.size()));
                        if (Math.abs(Float.parseFloat(field[6]) - 1) < 0.05)
                            temptotalprimaryattri++;
                        line = br.readLine();
                        if (line != null)
                            field = line.split(cvsSplitBy);
                    }
                    if (tempSym.contains(tempSymname)) {
                        // System.out.println("found a match!: " + tempSymname);
                        tempSympresent = 1;

                        if (Attrilist.get(attriCntLow).getrelevance() % 1 > 0.05) {
                            Attrilist.get(attriCntLow).changealreadyAsked(1);
                            Attrilist.get(attriCntLow)
                                    .changevalueEntered("Yes");
                            Attrilist.get(attriCntLow).changepresent(1);

                            if (Attrilist.get(attriCntLow).getvalue()
                                    .contains(";")) {
                                String[] evalsymyesno = (Attrilist.get(
                                        attriCntLow).getvalue().split(";"));
                                for (int o = 0; o < evalsymyesno.length; o++) {
                                    if (evalsymyesno.equals("Yes")) {
                                        tempSymrelevance = Float
                                                .parseFloat(Attrilist
                                                        .get(attriCntLow)
                                                        .getvaluespecificrelevance()
                                                        .split(";")[o]);
                                    }
                                }
                            } else if (Attrilist.get(attriCntLow).getvalue()
                                    .equals("No")) {
                                tempSymrelevance = -(Float.parseFloat(Attrilist
                                        .get(attriCntLow)
                                        .getvaluespecificrelevance()));
                            }
                        }
                    } else
                        tempSympresent = 0;
                    Symlist.add(new Symptom(tempSymname, tempSymrelevance,
                            tempCondname, attriCntLow, Attrilist.size() - 1,
                            temptotalprimaryattri, tempSympresent));
                    if (Math.abs(Math.abs(tempSymrelevance) - 1) < 0.76
                            || Math.abs(Math.abs(tempSymrelevance) - 1.1) < 0.76)
                        temptotalprimarysym++;
                    attriCntLow = Attrilist.size();
                    temptotalprimaryattri = 0;

                    Condlist.add(new Condition(tempCondname, tempCondrank,
                            symCntLow, Symlist.size() - 1, temptotalprimarysym));
                    symCntLow = Symlist.size();
                    temptotalprimarysym = 0;
                    // br.mark(0);
                    // tempCondline = line;
                    continue;
                }
                line = br.readLine();
            }
            br.close();
        }

        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void calcVitalrange(String vitalName, int vitalValue,
                                      ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist) {

        // TODO NEEDs to be changed!!
        Symptom tempsym;
        String tempSymname;
        float tempSymAttriSum;
        float tempsympresent;

        Attribute tempattri;
        // String tempAttriname;
        String tempattrivalue;
        int tempattripresent;
        float tempattrirelevance;
        float tempattrisum;
        int temptotalprimaryattri;

        int vitalrep = 0;
        String vitalvaluestr = vitalValue + "";
        // ArrayList tempchosenAttriRow;

        for (int j = 0; j < Symlist.size(); j++) {
            tempsym = Symlist.get(j);
            tempSymname = tempsym.getname();
            if (tempSymname.equals(vitalName)&&tempsym.getalreadyAsked()==0) {
                tempattri = Attrilist.get(tempsym.getattribegin());
                tempattrivalue = tempattri.getvalue();
                tempattri.changevalueEntered(vitalvaluestr);

				/*
				 * System.out.println("looking at _" + vitalName + "_ of " +
				 * tempSymname + "_ which has value of _" + tempattrivalue +
				 * "_ of this cond: _" + tempsym.getparent() +
				 * "_ and get following to fit in the range _" + vitalValue +
				 * "_");
				 */
                String[] valuesRangeForVital = tempattrivalue.split("_");
                int vitalValueRangeLow;
                int vitalValueRangeHigh;
                for (int o = 0; o < valuesRangeForVital.length - 1; o++) {

                    vitalValueRangeLow = Integer
                            .parseInt(valuesRangeForVital[o]);
                    vitalValueRangeHigh = Integer
                            .parseInt(valuesRangeForVital[o + 1]);

                    if (vitalValueRangeLow <= vitalValue
                            && vitalValueRangeHigh >= vitalValue) {
                        // System.out
                        // .println("match found for "
                        // + tempattrivalue);
                        tempattri.changerelevance(Float.parseFloat(tempattri
                                .getvaluespecificrelevance().split("_")[o]));
                        // System.out.println("changed this cond to positive");
                        tempattri.changepresent(1);
                        tempattri.changealreadyAsked(1);
                        vitalrep = 1;
                        break;
                    }
                }
                if (vitalrep == 0) {
                    // System.out.println("changed this cond to negative");
                    tempattri.changepresent(-1);// / -1 or 0??
                    tempattri.changealreadyAsked(-1);
                } else
                    vitalrep = 0;

                tempSymAttriSum = tempsym.getsumattri();
                tempattripresent = tempattri.getpresent();
                tempattrirelevance = tempattri.getrelevance();

                if (Math.abs(tempattrirelevance) > 0.00001)
                    tempSymAttriSum += tempattripresent
                            * (1 / (Math.floor(tempattrirelevance * 4) / 4));
                tempsym.changesumattri(tempSymAttriSum);

                tempattrisum = tempsym.getsumattri();
                if (tempsym.gettotalprimaryattri() != 0) {
                    tempsympresent = tempsym.getsumattri()
                            / tempsym.gettotalprimaryattri();
                } else {
                    // //TODO NEED TO BE CHANGED LATER!!
                    System.out
                            .println("this sym did not have any primary attri: "
                                    + tempsym.getname());
                    tempsympresent = tempsym.getsumattri();
                }
                tempsym.changepresent(tempsympresent);
                tempsym.changealreadyAsked(1);
                // System.out.println();
            }
        }

    }

    public static void calcVitalrangelookup(String Lookuptable,
                                            String vitalName, int vitalValue, int othervitalvalue,
                                            ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist) {

        // TODO NEEDs to be changed!!
        Symptom tempsym;
        String tempSymname;
        float tempSymAttriSum;
        float tempsympresent;

        Attribute tempattri;
        // String tempAttriname;
        String tempattrivalue;
        int tempattripresent;
        float tempattrirelevance;
        float tempattrisum;
        int temptotalprimaryattri;

        int vitalrep = 0;
        int vitallow = 0;
        int vitalhigh = 0;
        String vitalstr;
        // ArrayList tempchosenAttriRow;

        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(Lookuptable));
            line = br.readLine();
            // System.out.println(line);
            line = br.readLine();
            while (line != null) {
                String[] field = line.split(",");
                String[] fieldrange = field[0].split("_");
                if (othervitalvalue >= Integer.parseInt(fieldrange[0])
                        && othervitalvalue <= Integer.parseInt(fieldrange[1])) {
                    vitallow = Integer.parseInt(field[1]);
                    vitalhigh = Integer.parseInt(field[2]);
                    break;
                }
                line = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (vitalValue < vitallow)
            vitalstr = "Low";
        else if (vitalValue > vitalhigh)
            vitalstr = "High";
        else
            vitalstr = "Normal";

        String vitalentered = vitalValue + ", which is " + vitalstr;
        for (int j = 0; j < Symlist.size(); j++) {
            tempsym = Symlist.get(j);
            tempSymname = tempsym.getname();
            if (tempSymname.equals(vitalName)&&tempsym.getalreadyAsked()==0) {
                tempattri = Attrilist.get(tempsym.getattribegin());
                tempattrivalue = tempattri.getvalue();
                tempattri.changevalueEntered(vitalentered);
				/*
				 * System.out.println("looking at _" + vitalName + "_ of " +
				 * tempSymname + "_ which has value of _" + tempattrivalue +
				 * "_ of this cond: _" + tempsym.getparent() +
				 * "_ and get following to fit in the range _" + vitalValue +
				 * "_");
				 */

                if (tempattrivalue.contains(vitalstr)) {
                    // System.out.println("changed this cond to positive");
                    tempattri.changepresent(1);
                    tempattri.changealreadyAsked(1);
                    if (tempattrivalue.contains(";")) {
                        String[] splittempattrivalue = tempattrivalue
                                .split(";");
                        for (int o = 0; o < splittempattrivalue.length; o++) {
                            tempattrivalue = splittempattrivalue[o];
                            if (tempattrivalue.equals(vitalstr)) {
                                // System.out
                                // .println("match found for "
                                // + tempattrivalue);
                                tempattri.changerelevance(Float
                                        .parseFloat(tempattri
                                                .getvaluespecificrelevance()
                                                .split(";")[o]));
                                break;
                            }
                        }
                    }
                } else {
                    // System.out.println("changed this cond to negative");
                    tempattri.changepresent(-1);// / -1 or 0??
                    tempattri.changealreadyAsked(-1);

                }

                tempSymAttriSum = tempsym.getsumattri();
                tempattripresent = tempattri.getpresent();
                tempattrirelevance = tempattri.getrelevance();

                if (Math.abs(tempattrirelevance) > 0.00001)
                    tempSymAttriSum += tempattripresent
                            * (1 / (Math.floor(tempattrirelevance * 4) / 4));
                tempsym.changesumattri(tempSymAttriSum);

                tempattrisum = tempsym.getsumattri();
                if (tempsym.gettotalprimaryattri() != 0) {
                    tempsympresent = tempsym.getsumattri()
                            / tempsym.gettotalprimaryattri();
                } else {
                    // //TODO NEED TO BE CHANGED LATER!!
                    System.out
                            .println("this sym did not have any primary attri: "
                                    + tempsym.getname());
                    tempsympresent = tempsym.getsumattri();
                }
                tempsym.changepresent(tempsympresent);
                tempsym.changealreadyAsked(1);

                // System.out.println();
            }
        }
    }

    public static void calcVitalmatch(String vitalName, String vitalValue,
                                      ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist) {
        Symptom tempsym;
        String tempSymname;
        float tempSymAttriSum;
        float tempsympresent;

        Attribute tempattri;
        // String tempAttriname;
        String tempattrivalue;
        int tempattripresent;
        float tempattrirelevance;
        float tempattrisum;
        int temptotalprimaryattri;

        // ArrayList tempchosenAttriRow;

        for (int j = 0; j < Symlist.size(); j++) {
            tempsym = Symlist.get(j);
            tempSymname = tempsym.getname();
            if (tempSymname.equals(vitalName)&&tempsym.getalreadyAsked()==0) {
                tempattri = Attrilist.get(tempsym.getattribegin());
                tempattrivalue = tempattri.getvalue();
                tempattri.changevalueEntered(vitalValue);
				/*
				 * System.out.println("looking at _" + vitalName + "_ of " +
				 * tempSymname + "_ which has value of _" + tempattrivalue +
				 * "_ of this cond: _" + tempsym.getparent() +
				 * "_ and trying to match _" + vitalValue + "_");
				 */
                if (tempattrivalue.contains(vitalValue)) {
                    // System.out.println("changed this cond to positive");
                    tempattri.changepresent(1);
                    tempattri.changealreadyAsked(1);
                    if (tempattrivalue.contains(";")) {
                        String[] splittempattrivalue = tempattrivalue
                                .split(";");
                        for (int o = 0; o < splittempattrivalue.length; o++) {
                            if (splittempattrivalue[o].equals(vitalValue)) {
                                // System.out
                                // .println("match found for "
                                // + tempattrivalue);
                                tempattri.changepresent(1);
                                tempattri.changerelevance(Float
                                        .parseFloat(tempattri
                                                .getvaluespecificrelevance()
                                                .split(";")[o]));
                                break;
                            }
                        }
                    }
                } else {
                    // System.out.println("changed this cond to negative");
                    tempattri.changepresent(-1);// / -1 or 0??
                    tempattri.changealreadyAsked(-1);

                }

                tempSymAttriSum = tempsym.getsumattri();
                tempattripresent = tempattri.getpresent();
                tempattrirelevance = tempattri.getrelevance();
                if (Math.abs(tempattrirelevance) > 0.00001)
                    tempSymAttriSum += tempattripresent
                            * (1 / (Math.floor(tempattrirelevance * 4) / 4));
                tempsym.changesumattri(tempSymAttriSum);

                tempattrisum = tempsym.getsumattri();
                if (tempsym.gettotalprimaryattri() != 0) {
                    tempsympresent = tempsym.getsumattri()
                            / tempsym.gettotalprimaryattri();
                } else {
                    // //TODO NEED TO BE CHANGED LATER!!
                    System.out
                            .println("this sym did not have any primary attri: "
                                    + tempsym.getname());
                    tempsympresent = tempsym.getsumattri();
                }
                tempsym.changepresent(tempsympresent);
                tempsym.changealreadyAsked(1);

                // System.out.println();
            }
        }
    }

    public static void calcVitalrange(String vitalName, float vitalValue,
                                      ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist) {
        Symptom tempsym;
        String tempSymname;
        float tempSymAttriSum;
        float tempsympresent;

        Attribute tempattri;
        // String tempAttriname;
        String tempattrivalue;
        int tempattripresent;
        float tempattrirelevance;
        float tempattrisum;
        int temptotalprimaryattri;
        int vitalrep = 0;
        String vitalvaluestr = vitalValue + "";

        // ArrayList tempchosenAttriRow;

        for (int j = 0; j < Symlist.size(); j++) {
            tempsym = Symlist.get(j);
            tempSymname = tempsym.getname();
            if (tempSymname.equals(vitalName)&&tempsym.getalreadyAsked()==0) {
                tempattri = Attrilist.get(tempsym.getattribegin());
                tempattrivalue = tempattri.getvalue();
                tempattri.changevalueEntered(vitalvaluestr);

				/*
				 * System.out.println("looking at _" + vitalName + "_ of " +
				 * tempSymname + "_ which has value of _" + tempattrivalue +
				 * "_ of this cond: _" + tempsym.getparent() +
				 * "_ and get following to fit in the range _" + vitalValue +
				 * "_");
				 */
                String[] valuesRangeForVital = tempattrivalue.split("_");
                float vitalValueRangeLow;
                float vitalValueRangeHigh;
                for (int o = 0; o < valuesRangeForVital.length - 1; o++) {
                    vitalValueRangeLow = Integer
                            .parseInt(valuesRangeForVital[o]);
                    vitalValueRangeHigh = Integer
                            .parseInt(valuesRangeForVital[o + 1]);

                    if (vitalValueRangeLow <= vitalValue
                            && vitalValueRangeHigh >= vitalValue) {
                        // System.out
                        // .println("match found for "
                        // + tempattrivalue);
                        tempattri.changerelevance(Float.parseFloat(tempattri
                                .getvaluespecificrelevance().split("_")[o]));
                        // System.out.println("changed this cond to positive");
                        tempattri.changepresent(1);
                        tempattri.changealreadyAsked(1);
                        vitalrep = 1;
                        break;
                    }
                }
                if (vitalrep == 0) {
                    // System.out.println("changed this cond to negative");
                    tempattri.changepresent(-1);// / -1 or 0??
                    tempattri.changealreadyAsked(-1);

                } else
                    vitalrep = 0;

                tempSymAttriSum = tempsym.getsumattri();
                tempattripresent = tempattri.getpresent();
                tempattrirelevance = tempattri.getrelevance();

                if (Math.abs(tempattrirelevance) > 0.00001)
                    tempSymAttriSum += tempattripresent
                            * (1 / (Math.floor(tempattrirelevance * 4) / 4));
                tempsym.changesumattri(tempSymAttriSum);

                tempattrisum = tempsym.getsumattri();
                if (tempsym.gettotalprimaryattri() != 0) {
                    tempsympresent = tempsym.getsumattri()
                            / tempsym.gettotalprimaryattri();
                } else {
                    // //TODO NEED TO BE CHANGED LATER!!
                    System.out
                            .println("this sym did not have any primary attri: "
                                    + tempsym.getname());
                    tempsympresent = tempsym.getsumattri();
                }
                tempsym.changepresent(tempsympresent);
                tempsym.changealreadyAsked(1);

                // System.out.println();
            }
        }
    }

    public static void sumUpSymScore(ArrayList<Condition> Condlist,
                                     ArrayList<Symptom> Symlist) {
        Condition tempCond = Condlist.get(0);
        String tempCondname;
        Symptom tempsym;
        String tempSymname;
        float tempCondScore = 0;
        float tempCondSymSum = 0;
        float tempsympresent;
        float tempsymrelevance;
        float tempCondrisk = 0;

        for (int i = 0; i < Condlist.size(); i++) {
            Condlist.get(i).changesumsym(0);
        }

        for (int j = 0; j < Symlist.size(); j++) {
            tempsym = Symlist.get(j);
            tempSymname = tempsym.getname();
            for (int i = 0; i < Condlist.size(); i++) {
                if ((tempsym.getparent()).equals((Condlist.get(i)).getname())) {
                    tempCond = Condlist.get(i);
                    break;
                }
            }
            tempCondname = tempCond.getname();
            tempCondSymSum = tempCond.getsumsym();
            tempsympresent = tempsym.getpresent();
            tempsymrelevance = tempsym.getrelevance();
            if (tempsymrelevance != 0)
                tempCondSymSum += tempsympresent
                        * (1 / (Math.floor(tempsymrelevance * 4) / 4));
            tempCond.changesumsym(tempCondSymSum);

        }
    }

    public static void calcCondScore(ArrayList<Condition> Condlist) {

        Condition tempCond;
        float tempCondScore = 0;
        float tempCondSymSum = 0;
        float tempCondrisk = 0;

        for (int i = 0; i < Condlist.size(); i++) {
            tempCond = Condlist.get(i);
            tempCondrisk = tempCond.getrisk();
            tempCondSymSum = tempCond.getsumsym();
            tempCondScore = tempCondSymSum * (1 / tempCondrisk);
            tempCond.changescore(tempCondScore);
        }

    }

    public static void addSpecialCasesSym(ArrayList<Condition> Condlist,
                                          ArrayList<Symptom> Symlist, ArrayList<String> otherSym) {
    }

    public void attriofPresentingSymptomNonAdaptive(
            String currentPresentSym, String cvsSplitBy,
            ArrayList<ArrayList<String>> displayAttriList)
    // index of first symptom, from condition, then match it up.
    {

        InputStream is = getResources().openRawResource(SymDBFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        ArrayList<String> tempattrilist;

        try {
            line = br.readLine();
            // System.out.println(line);
            line = br.readLine();
            // System.out.println(line);
            while (line != null) {
                String[] field = line.split(cvsSplitBy);
                if (field[0].equals(currentPresentSym)) {
                    tempattrilist = new ArrayList<String>();
                    tempattrilist.add(field[1]);
                    tempattrilist.add(field[2]);
                    if (field[3].contains("_date")) {
                        tempattrilist.add("[please enter date]");
                        line = br.readLine();
                    } else if (field[3].contains("_days")) {
                        tempattrilist.add("[please enter duration]");
                        line = br.readLine();
                    } else if (field[3].contains("_freq")) {
                        tempattrilist.add("[please enter how many times/day]");
                        line = br.readLine();
                    } else if (field[3].contains("_describe")) {
                        tempattrilist.add("[Please describe]");
                        line = br.readLine();
                    }else if (field[3].contains(";")) {
                        String[] splittempattrivalue = (field[3]).split(";");
                        for (int o = 0; o < splittempattrivalue.length; o++) {
                            tempattrilist.add(splittempattrivalue[o]);
                        }
                        line = br.readLine();
                    } else {
                        tempattrilist.add(field[3]);
                        line = br.readLine();
                        field = line.split(cvsSplitBy);
                        while (field[1].equals("") && line != null) {
                            tempattrilist.add(field[3]);
                            line = br.readLine();
                            field = line.split(cvsSplitBy);
                        }
                    }
                    displayAttriList.add(tempattrilist);
                    while (line != null) {
                        field = line.split(cvsSplitBy);
                        if (field[0].equals("") && !field[1].equals("")
                                && !field[3].equals("")) {
                            tempattrilist = new ArrayList<String>();
                            tempattrilist.add(field[1]);
                            tempattrilist.add(field[2]);
                            if (field[3].contains("_date")) {
                                tempattrilist.add("[please enter date]");
                                line = br.readLine();
                            } else if (field[3].contains("_days")) {
                                tempattrilist
                                        .add("[please enter duration]");
                                line = br.readLine();
                            } else if (field[3].contains("_freq")) {
                                tempattrilist
                                        .add("[please enter how many times/day]");
                                line = br.readLine();
                            }else if (field[3].contains("_describe")) {
                                tempattrilist.add("[Please describe]");
                                line = br.readLine();
                            } else if (field[3].contains(";")) {
                                String[] splittempattrivalue = (field[3])
                                        .split(";");
                                for (int o = 0; o < splittempattrivalue.length; o++) {
                                    tempattrilist.add(splittempattrivalue[o]);
                                }
                                line = br.readLine();
                            } else {
                                tempattrilist.add(field[3]);
                                line = br.readLine();
                                field = line.split(cvsSplitBy);
                                while (field[1].equals("")) {
                                    tempattrilist.add(field[3]);
                                    line = br.readLine();
                                    field = line.split(cvsSplitBy);
                                }
                            }
                            displayAttriList.add(tempattrilist);
                        } else
                            break;
                    }
                    break;
                }
                line = br.readLine();
            }
            br.close();
        }

        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void attriofPresentingSymptom(String currentPresentSym,
                                                float tempwantattrirelevance, ArrayList<Condition> Condlist,
                                                ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist,
                                                ArrayList<ArrayList<String>> displayAttriList,
                                                ArrayList<String> ExamsTestsToPerform)
    // index of first symptom, from condition, then match it up.
    {
        Condition tempCond = Condlist.get(0);
        int tempsymbegin;
        int tempsymend;

        Symptom tempsym;
        String tempsymname;
        int tempattribegin;
        int tempattriend;

        Attribute tempattri;
        String tempattriname;
        String tempattrivalue;
        float tempattrirelevance;

        int testrep = 0;

        ArrayList<String> tempattrilist = new ArrayList<String>();
        String tempattrilistname;
        int samerankCondcnt = 0;
        do {
            tempsymbegin = tempCond.getsymbegin();
            tempsymend = tempCond.getsymend();

            for (int j = tempsymbegin; j <= tempsymend; j++) {
                // System.out.println("looking at Symptom " + tempsymbegin
                // + " of Conditing 1. Index: " + j);
                tempsym = Symlist.get(j);
                tempsymname = tempsym.getname();
                if (tempsymname.equals(currentPresentSym)) {
                    // System.out.println("found the presenting symptom match");
                    // tempsym.changepresent((float)1);
                    tempattribegin = tempsym.getattribegin();
                    tempattriend = tempsym.getattriend();
                    // System.out
                    // .println("the attributes that belong to this symptom are"
                    // + tempattribegin + " and " + tempattriend);
                    for (int k = tempattribegin; k <= tempattriend; k++) {
                        tempattri = Attrilist.get(k);
                        tempattrirelevance = tempattri.getrelevance();
                        // System.out
                        // .println("we're looking at this attribute with its relevance: "
                        // + tempattrirelevance);
                        // System.out.println("looking at this attribute: _"
                        // + tempattri.getname() +
                        // "_'s relevancy and seeing if it can be added to list"
                        // + tempattrirelevance);
                        if (tempattri.getalreadyAsked() == 0
                                && (tempattrirelevance % 1 < 0.05
                                || Math.abs(Math
                                .abs(tempattrirelevance) - 0.5) < 0.05 || Math
                                .abs(Math.abs(tempattrirelevance) - 0.25) < 0.05)) {
                            // System.out.println("this attri was added to the list"
                            // + tempattri.getname() + " with relevancy"
                            // + tempattrirelevance);
                            tempattriname = tempattri.getname();
                            // TODO this won't really work.... because 1.2, 1.3
                            // etc
                            // are already filtered it out previously...
							/*
							 * if (Math.abs(tempattrirelevance - 1.1) < 0.05 ||
							 * Math.abs(tempattrirelevance - 1.3) < 0.05) { for
							 * (String str : ExamsTestsToPerform) { if
							 * (tempattriname.equals(str)) { testrep = 1; break;
							 * } } if (testrep == 0) { //
							 * System.out.println(field[2]);
							 * ExamsTestsToPerform.add(tempattriname); } else
							 * testrep = 0; // System.out //
							 * .println("need to do this diagnostic later:" // +
							 * tempattriname); } else {
							 */
                            int alreadyInc = 0;
                            for (int m = 0; m < tempattrilist.size(); m++) {
                                if (tempattrilist.get(m).equals(tempattriname)) {
                                    alreadyInc = 1;
                                    break;
                                }
                            }
                            if (alreadyInc == 0) {
                                tempattrilist.add(tempattriname);
                                tempattrilist.add(tempattri.gethistorynote());
                            } else
                                alreadyInc = 0;
                            // System.out
                            // .println("this attribute name have been added to the list: "
                            // + tempattriname);
                            // }
                        }
                    }
                }
            }
            samerankCondcnt++;
            tempCond = Condlist.get(samerankCondcnt);
        } while (Math.abs(tempCond.getscore() - Condlist.get(0).getscore()) < 0.1);

        ArrayList<String>[] tempattrivaluelists = (ArrayList<String>[]) new ArrayList[tempattrilist
                .size() / 2];
        // System.out.println(tempattrilist.size());
        int historynotecounter = 0;
        for (int m = 0; m < tempattrilist.size() / 2; m++) {
            tempattrilistname = tempattrilist.get(historynotecounter);
            historynotecounter++;
            tempattrivaluelists[m] = new ArrayList<String>();
            tempattrivaluelists[m].add(tempattrilistname);
            tempattrivaluelists[m].add(tempattrilist.get(historynotecounter));
            historynotecounter++;
            // System.out.println("the index of " + tempattrilistname + "is" +
            // m);
        }

        int attrirep = 0;
        for (int l = 0; l < Attrilist.size(); l++) {
            tempattri = Attrilist.get(l);
            tempattriname = tempattri.getname();
            // System.out.println("looking at attri" + tempattriname);

            for (int m = 0; m < tempattrivaluelists.length; m++) {
                tempattrilistname = tempattrivaluelists[m].get(0);
                // System.out.println("trying to match: " + tempattrilistname);

                if (tempattriname.equals(tempattrilistname)
                        && ((Symlist.get(tempattri.getparent())).getname())
                        .equals(currentPresentSym)) {// /need to add if
                    // statement
                    // about the
                    // symptom is
                    // matching too
                    if (tempattrivaluelists[m].get(0).contains("Date")
                            && tempattri.getvalue().contains("~")) {
                        for (String str : tempattrivaluelists[m]) {
                            if (str.equals("[please enter date]")) {
                                attrirep = 1;
                                break;
                            }
                        }
                        if (attrirep == 0) {
                            tempattrivaluelists[m].add("[please enter date]");
                            tempattrivaluelists[m].add("Do not remember");
                        } else
                            attrirep = 0;
                    } else if (tempattrivaluelists[m].get(0).contains("long")
                            && tempattri.getvalue().contains("_")) {
                        for (String str : tempattrivaluelists[m]) {
                            if (str.equals("[please enter duration]")) {
                                attrirep = 1;
                                break;
                            }
                        }
                        if (attrirep == 0) {
                            tempattrivaluelists[m]
                                    .add("[please enter duration]");
                        } else
                            attrirep = 0;
                    } else if (tempattrivaluelists[m].get(1).contains("times per day")
                            && tempattri.getvalue().contains("_")) {
                        for (String str : tempattrivaluelists[m]) {
                            if (str.equals("[please enter how many times/day]")) {
                                attrirep = 1;
                                break;
                            }
                        }
                        if (attrirep == 0) {
                            tempattrivaluelists[m]
                                    .add("[please enter how many times/day]");
                        } else
                            attrirep = 0;
                    }

                    else if (tempattri.getvalue().contains(";")) {
                        String[] splittempattrivalue = (tempattri.getvalue())
                                .split(";");
                        for (int o = 0; o < splittempattrivalue.length; o++) {
                            tempattrivalue = splittempattrivalue[o];

                            for (String str : tempattrivaluelists[m]) {
                                if (tempattrivalue.equals(str)) {
                                    attrirep = 1;
                                    break;
                                }
                            }
                            if (attrirep == 0) {
                                tempattrivaluelists[m].add(tempattrivalue);
                            } else
                                attrirep = 0;
                        }
                    }
                    // if it contains _, check next door, and show []... 3
                    // cases? LMP, frequency,
                    else {
                        tempattrivalue = tempattri.getvalue();
                        for (String str : tempattrivaluelists[m]) {
                            if (tempattrivalue.equals(str)) {
                                attrirep = 1;
                                break;
                            }
                        }
                        if (attrirep == 0) {
                            tempattrivaluelists[m].add(tempattrivalue);
                        } else
                            attrirep = 0;
                    }

                }
                // System.out.println("matched! and it's stored at array" +
                // m);
            }

        }
        for (int m = 0; m < tempattrilist.size() / 2; m++) {
            displayAttriList.add(tempattrivaluelists[m]);
            // System.out.println("just added values of "
            // + (tempattrivaluelists[m].get(0)).toString()
            // + " to the array");
        }
    }

    public static void calcMatchedAttriScore(String tempsymtomatch,
                                             ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist,
                                             ArrayList<ArrayList<String>> chosenAttriList) {
        Symptom tempsym;
        String tempSymname;
        int tempattribegin;
        int tempattriend;
        float tempSymAttriSum;
        float tempsympresent;

        Attribute tempattri;
        String tempAttriname;
        String tempattrivalue;
        float templowerrangevalue;
        float tempupperrangevalue;
        int tempattripresent;
        float tempattrirelevance;
        float tempattrisum;
        int temptotalprimaryattri;

        ArrayList tempchosenAttriRow;
        int attrirep = 0;
        int attriandrep = 0;
        float tempattrirelev = 0;

        for (int j = 0; j < Symlist.size(); j++) {
            tempsym = Symlist.get(j);
            tempSymname = tempsym.getname();
            if (tempSymname.equals(tempsymtomatch)) {
                // System.out.println("found a symptom that matched in the list!");
                tempattribegin = tempsym.getattribegin();
                tempattriend = tempsym.getattriend();
                for (int i = tempattribegin; i <= tempattriend; i++) {
                    // System.out.println("going through the attribute array for this symptom");
                    tempattri = Attrilist.get(i);
                    tempAttriname = tempattri.getname();
                    for (int k = 0; k < chosenAttriList.size(); k++) {
                        tempchosenAttriRow = chosenAttriList.get(k);
                        // System.out.println("trying to match each attribute with chosen attribute");
                        if (tempAttriname.equals(tempchosenAttriRow.get(0))) {
                            tempattri.changealreadyAsked(1);
                            String valueEntered = "";
                            for (int l = 2; l < tempchosenAttriRow.size(); l++)// get
                            // 2+
                            // values
                            // entered
                            {
                                valueEntered += tempchosenAttriRow.get(l)
                                        .toString() + ", ";
                            }
                            tempattri.changevalueEntered(valueEntered);
                            // System.out.println("attri name matched"
                            // + tempAttriname + " for sym " + tempSymname
                            // + "at index " + i);
                            if (tempattri.getvalue().contains(";")) {
                                String[] splittempattrivalue = (tempattri
                                        .getvalue()).split(";");
                                for (int o = 0; o < splittempattrivalue.length; o++) {
                                    tempattrivalue = splittempattrivalue[o];
                                    for (int p = 2; p < tempchosenAttriRow
                                            .size(); p++) {
                                        // System.out.println("trying to match _"
                                        // + tempattrivalue
                                        // + "_ by comparing with_"
                                        // + tempchosenAttriRow.get(p));
                                        if (tempattrivalue
                                                .equals(tempchosenAttriRow
                                                        .get(p))) {
                                            // System.out
                                            // .println("match found for "
                                            // + tempattrivalue);
                                            tempattri.changepresent(1);

                                            if (tempattri
                                                    .getvaluespecificrelevance()
                                                    .contains("&")) {

                                                if (attriandrep == 0) {
                                                    tempattri
                                                            .changerelevance(Float
                                                                    .parseFloat(tempattri
                                                                            .getvaluespecificrelevance()
                                                                            .split(";")[o + 1]));
                                                    attriandrep = 1;
                                                    attrirep = 1;
                                                } else {
                                                    tempattrirelev = tempattri
                                                            .getrelevance();
                                                    float tempattrirelevtoadd = Float
                                                            .parseFloat(tempattri
                                                                    .getvaluespecificrelevance()
                                                                    .split(";")[o + 1]);
                                                    tempattrirelev = tempattrirelev
                                                            + tempattrirelevtoadd;
                                                    tempattri
                                                            .changerelevance(tempattrirelev);
                                                    attrirep = 1;
                                                }
                                                break;

                                            } else {
                                                tempattri
                                                        .changerelevance(Float
                                                                .parseFloat(tempattri
                                                                        .getvaluespecificrelevance()
                                                                        .split(";")[o]));

                                                tempattri.changepresent(1);
                                                attrirep = 1;
                                                break;
                                            }
                                        }
                                    }
                                    if (attrirep == 1 && attriandrep == 0) {
                                        break;
                                    }
                                }
                                if (attrirep != 1) {
                                    tempattri.changepresent(-1);
                                    // System.out.println("no match found for _"
                                    // + tempAttriname);

                                } else {
                                    attrirep = 0;
                                    attriandrep = 0;
                                }
                            } else if (tempattri.getvalue().contains("_")) {
                                // TODO
                                // is there a way to return how many _
                                // contained?
                                String[] valuesRange = tempattri.getvalue()
                                        .split("_");
                                // for each underscore, ... get beginning range
                                // and later range...
                                // or for string array size -1, get beginning
                                // range and later range
                                // try to fit in between the ranges
                                // if yes, look into value specific relevance
                                // change present to the value,
                                // if not within value, change present = -1

                                for (int o = 0; o < valuesRange.length - 1; o++) {
                                    templowerrangevalue = Float
                                            .parseFloat(valuesRange[o]);
                                    tempupperrangevalue = Float
                                            .parseFloat(valuesRange[o + 1]);
                                    for (int p = 2; p < tempchosenAttriRow
                                            .size(); p++) {
                                        // System.out.println("trying to match _"
                                        // + tempattrivalue
                                        // + "_ by comparing with_"
                                        // + tempchosenAttriRow.get(p));
                                        if (Float.parseFloat(tempchosenAttriRow
                                                .get(p).toString()) >= templowerrangevalue
                                                && Float.parseFloat(tempchosenAttriRow
                                                .get(p).toString()) <= tempupperrangevalue) {
                                            // System.out
                                            // .println("match found for "
                                            // + tempattrivalue);
                                            tempattri
                                                    .changerelevance(Float
                                                            .parseFloat(tempattri
                                                                    .getvaluespecificrelevance()
                                                                    .split("_")[o]));
                                            tempattri.changepresent(1);
                                            attrirep = 1;
                                            break;
                                        }
                                    }
                                    if (attrirep == 1) {
                                        break;
                                    }
                                }
                                if (attrirep != 1) {
                                    tempattri.changepresent(-1);
                                    // System.out.println("no match found for _"
                                    // + tempAttriname);

                                } else {
                                    attrirep = 0;
                                }
                            } else {
                                tempattrivalue = tempattri.getvalue();
                                for (int p = 2; p < tempchosenAttriRow.size(); p++) {

                                    // System.out.println("trying to match _"
                                    // + tempattrivalue
                                    // + "_ by comparing with_ "
                                    // + tempchosenAttriRow.get(p));
                                    if (tempattrivalue
                                            .equals(tempchosenAttriRow.get(p))) {
                                        // System.out.println("match found for "
                                        // + tempattrivalue);
                                        attrirep = 1;
                                        break;
                                    }
                                }
                                if (attrirep == 1) {
                                    tempattri.changepresent(1);
                                    attrirep = 0;
                                } else {
                                    if (tempchosenAttriRow.get(2).equals(
                                            "Do not remember"))
                                        tempattri.changepresent(0);
                                    else
                                        tempattri.changepresent(-1);// / -1 or
                                    // 0??
                                    // System.out
                                    // .println("none of the values matched for "
                                    // + tempattrivalue);
                                }
                            }
                            break;
                        }
                    }

                    // take into account current SymSum and add new Attri to it.
                    tempattrirelevance = tempattri.getrelevance();
                    if (Math.abs(tempattrirelevance) > 0.00001) {
                        tempSymAttriSum = tempsym.getsumattri();
                        tempattripresent = tempattri.getpresent();
                        // tempattrirelevance = tempattri.getrelevance();
                        tempSymAttriSum += tempattripresent
                                * (1 / Math.floor(tempattrirelevance));
                        tempsym.changesumattri(tempSymAttriSum);
                    }
                }
                tempattrisum = tempsym.getsumattri();
                if (tempsym.gettotalprimaryattri() != 0) {
                    tempsympresent = tempsym.getsumattri()
                            / tempsym.gettotalprimaryattri();
                } else {
                    // //TODO NEED TO BE CHANGED LATER!!
                    // System.out
                    // .println("this sym did not have any primary attri: "
                    // + tempsym.getname());
                    tempsympresent = tempsym.getsumattri();
                }
                tempsym.changepresent(tempsympresent);
                // System.out.println();
                // end of if for matching attribute name....
            }
        }
        // System.out.println();
    }

    public static void otherSymList(ArrayList<Condition> Condlist,
                                    ArrayList<Symptom> Symlist, ArrayList<String> otherSym,
                                    ArrayList<String> presentingSym, ArrayList<String> yesRiskFactor) {
        Condition tempCond = Condlist.get(0);
        int tempsymbegin = tempCond.getsymbegin();
        int tempsymend = tempCond.getsymend();

        Symptom tempsym;
        String tempsymname;

        otherSym.clear();

        System.out.println("asking about other symptoms");
        int samerankCondcnt = 0;
        int testrep = 0;

        do {
            tempsymbegin = tempCond.getsymbegin();
            tempsymend = tempCond.getsymend();

            for (int j = tempsymbegin; j <= tempsymend; j++) {
                // System.out.println("looking at Symptom " + tempsymbegin
                // + " of Conditing 1. Index: " + j);
                tempsym = Symlist.get(j);
                tempsymname = tempsym.getname();
                if ((tempsym.getrelevance() % 1 < 0.00005
                        || Math.abs(Math.abs(tempsym.getrelevance()) - 0.5) < 0.00005 || Math
                        .abs(Math.abs(tempsym.getrelevance()) - 0.25) < 0.00005)
                        && tempsym.getalreadyAsked() == 0) {
                    for (String str : otherSym) {
                        if (tempsymname.equals(str)) {
                            testrep = 1;
                            break;
                        }
                    }
                    if (testrep == 0) {
                        // System.out.println(field[2]);
                        otherSym.add(tempsymname);
                    } else
                        testrep = 0;
                }
                if (tempsymname.equals("Risk Factor")) {
                    yesRiskFactor.add("Yes");
                    tempsym.changepresent(1);
                    tempsym.changealreadyAsked(1);
                }
            }
            samerankCondcnt++;
            tempCond = Condlist.get(samerankCondcnt);
        } while (Math.abs(tempCond.getscore() - Condlist.get(0).getscore()) < 0.1);
    }

    public static void setSymScore(ArrayList<String> otherSym,
                                   ArrayList<String> presentingSym, ArrayList<String> nopresentingSym,
                                   ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist,
                                   ArrayList<String> yesRiskFactor) {

        int symcount = 0;
        String totalnopresenting = "Patient does not have:";
        Symptom tempsym;

        System.out.println("compiling nopresenting sym array");
        for (int i = 0; i < otherSym.size(); i++) {
            for (int j = 0; j < presentingSym.size(); j++) {
                System.out.println("presenting sym being evaluated is "
                        + presentingSym.get(j) + "and it's versus "
                        + otherSym.get(i));
                if (otherSym.get(i).equals(presentingSym.get(j))) {
                    symcount = 1;
                    break;
                }
            }
            if (symcount != 1) {
                nopresentingSym.add(otherSym.get(i));
                System.out.println("symptom added to no presenting sym"
                        + nopresentingSym);
                totalnopresenting += otherSym.get(i) + ", ";
            } else
                symcount = 0;
        }

        printtoptMHN(CurrentPtMHN, totalnopresenting);


        for (int i = 0; i < Symlist.size(); i++) {
            for (int j = 0; j < presentingSym.size(); j++) {
                if (Symlist.get(i).getname().equals(presentingSym.get(j))
                        && Symlist.get(i).getalreadyAsked() == 0) {
                    tempsym = Symlist.get(i);
                    Symlist.get(i).changepresent(1);
                    Symlist.get(i).changealreadyAsked(1);
                    if (Attrilist.get(Symlist.get(i).getattribegin())
                            .getrelevance() % 1 > 0.05) {
                        Attrilist.get(Symlist.get(i).getattribegin())
                                .changevalueEntered("Yes");
                        Attrilist.get(Symlist.get(i).getattribegin())
                                .changealreadyAsked(1);

                        if (Attrilist.get(Symlist.get(i).getattribegin())
                                .getvalue().contains(";")) {
                            String[] evalsymyesno = (Attrilist.get(
                                    Symlist.get(i).getattribegin()).getvalue()
                                    .split(";"));
                            for (int o = 0; o < evalsymyesno.length; o++) {
                                if (evalsymyesno[o].equals("Yes")) {
                                    Symlist.get(i)
                                            .changerelevance(
                                                    Float.parseFloat(Attrilist
                                                            .get(Symlist
                                                                    .get(i)
                                                                    .getattribegin())
                                                            .getvaluespecificrelevance()
                                                            .split(";")[o]));
                                }
                            }
                        } else if (Attrilist
                                .get(Symlist.get(i).getattribegin()).getvalue()
                                .equals("No")) {
                            Symlist.get(i).changerelevance(
                                    -(Float.parseFloat(Attrilist.get(
                                            Symlist.get(i).getattribegin())
                                            .getvaluespecificrelevance())));

                            // change relev to negative?? final is subtraction??
                        }
                    }
                }
            }
            for (int j = 0; j < nopresentingSym.size(); j++) {
                if (Symlist.get(i).getname().equals(nopresentingSym.get(j))
                        && Symlist.get(i).getalreadyAsked() == 0) {
                    tempsym = Symlist.get(i);
                    Symlist.get(i).changepresent(-1);
                    Symlist.get(i).changealreadyAsked(-1);

                    if (Attrilist.get(Symlist.get(i).getattribegin())
                            .getrelevance() % 1 > 0.05) {
                        Attrilist.get(Symlist.get(i).getattribegin())
                                .changevalueEntered("No");
                        Attrilist.get(Symlist.get(i).getattribegin())
                                .changealreadyAsked(-1);
                        if (Attrilist.get(Symlist.get(i).getattribegin())
                                .getvalue().contains(";")) {
                            String[] evalsymyesno = (Attrilist.get(
                                    Symlist.get(i).getattribegin()).getvalue()
                                    .split(";"));
                            for (int o = 0; o < evalsymyesno.length; o++) {
                                if (evalsymyesno.equals("No")) {
                                    Symlist.get(i)
                                            .changerelevance(
                                                    -(Float.parseFloat(Attrilist
                                                            .get(Symlist
                                                                    .get(i)
                                                                    .getattribegin())
                                                            .getvaluespecificrelevance()
                                                            .split(";")[o])));
                                    // change relev to negative?
                                }
                            }
                        } else if (Attrilist
                                .get(Symlist.get(i).getattribegin()).getvalue()
                                .equals("No")) {
                            Symlist.get(i).changerelevance(
                                    -(Float.parseFloat(Attrilist.get(
                                            Symlist.get(i).getattribegin())
                                            .getvaluespecificrelevance())));
                        }
                    }
                }
            }
        }
    }

    public static void ramDBprint(ArrayList<Condition> Condlist,
                                  ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist,
                                  String CurrentPtLogFile) {

        String filename = CurrentPtLogFile;
        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(filename, true)))) {
            out.println("--------------------printingggggggggggg--------------------");
            out.println("number of Condition nodes:" + Condlist.size());
            out.println("number of Symptom nodes:" + Symlist.size());
            out.println("number of Attribute nodes:" + Attrilist.size());
            for (int i = 0; i < Condlist.size(); i++) {
                out.println();
                out.println();
                out.println("Conditions rank: ");
                out.println(i);
                out.println();
                Condition temp = Condlist.get(i);
                temp.printCond(CurrentPtLogFile);

                for (int j = Condlist.get(i).getsymbegin(); j <= Condlist
                        .get(i).getsymend(); j++) {
                    out.println();
                    out.println();
                    out.println("Symptoms array: ");
                    out.println(j);
                    out.println();
                    Symptom tempsym = Symlist.get(j);
                    tempsym.printSym(CurrentPtLogFile);

                    for (int k = Symlist.get(j).getattribegin(); k <= Symlist
                            .get(j).getattriend(); k++) {
                        out.println();
                        out.println();
                        out.println("Attributes array: ");
                        out.println(k);
                        out.println();
                        Attribute tempattri = Attrilist.get(k);
                        tempattri.printAttri(CurrentPtLogFile);
                    }
                }
            }
        } catch (IOException e) {
            // exception handling left as an exercise for the reader
        }
    }

	/*
	 * public static void ramCondprint(ArrayList<Condition> Condlist,
	 * ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist) {
	 * System.out.println("number of Condition nodes:" + Condlist.size());
	 * System.out.println("number of Symptom nodes:" + Symlist.size());
	 * System.out.println("number of Attribute nodes:" + Attrilist.size()); for
	 * (int i = 1; i <= Condlist.size(); i++) { System.out.println();
	 * System.out.println("Conditions rank: " + i); System.out.println();
	 * Condition temp = Condlist.get(i - 1); temp.printCond(); } }
	 *
	 * public static void ramSymprint(ArrayList<Condition> Condlist,
	 * ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist) {
	 * System.out.println("number of Condition nodes:" + Condlist.size());
	 * System.out.println("number of Symptom nodes:" + Symlist.size());
	 * System.out.println("number of Attribute nodes:" + Attrilist.size());
	 *
	 * for (int j = 1; j <= Symlist.size(); j++) { System.out.println();
	 * System.out.println("Symptoms array: " + j); System.out.println(); Symptom
	 * tempsym = Symlist.get(j - 1); tempsym.printSym(); }
	 *
	 * }
	 *
	 * public static void ramAttriprint(ArrayList<Condition> Condlist,
	 * ArrayList<Symptom> Symlist, ArrayList<Attribute> Attrilist) {
	 * System.out.println("number of Condition nodes:" + Condlist.size());
	 * System.out.println("number of Symptom nodes:" + Symlist.size());
	 * System.out.println("number of Attribute nodes:" + Attrilist.size()); for
	 * (int k = 1; k <= Attrilist.size(); k++) { System.out.println();
	 * System.out.println("Attributes array: " + k); System.out.println();
	 * Attribute tempattri = Attrilist.get(k - 1); tempattri.printAttri(); }
	 *
	 * }
	 */

    public static void selectedAttriList(String prevSymConsidered,
                                         ArrayList<ArrayList<String>> chosenAttriList,
                                         String CurrentPtLogFile) {
        String AttriForaSym = prevSymConsidered + ": ";
        String tempFormatforanAttri = "";
        System.out.println("_______ user/HA chose: ");
        System.out.println(prevSymConsidered);
        for (ArrayList temp : chosenAttriList) {
            System.out.print((temp.get(0)).toString() + ": ");
            printToLog(CurrentPtLogFile, "_______ user/HA choose attribute: ",
                    temp);
            for (int i = 2; i < temp.size(); i++) {
                //System.out.print((temp.get(i)).toString() + ", ");

                if (temp.get(i).toString().equals("Yes")) {
                    tempFormatforanAttri = temp.get(1).toString()
                            .replaceAll("_", "");
                } else {
                    tempFormatforanAttri = temp.get(1).toString()
                            .replaceAll("_", (temp.get(i)).toString());
                }
                AttriForaSym += tempFormatforanAttri + "; ";
            }
        }
        Log.i("Checking for Function", "------------------print to file is:");
        Log.i("Checking for Function", AttriForaSym);
        AttriForaSym=AttriForaSym.replaceAll(";", ",");
        printtoptMHN(CurrentPtMHN, AttriForaSym);
        printtoptMHN(AndroidLogFile, AttriForaSym);

    }

    public static void printFinalToLog(String CurrentPtLogFile,
                                       ArrayList<Condition> Condlist, ArrayList<Symptom> Symlist,
                                       ArrayList<Attribute> Attrilist) {

        String filename = CurrentPtLogFile;
        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(filename, true)))) {
            out.println("number of Condition nodes:" + Condlist.size());
            for (int i = 1; i <= Condlist.size(); i++) {
                // out.println();
                // out.println("Conditions rank: " + i);
                // out.println();
                Condition temp = Condlist.get(i - 1);
                out.print(temp.getname() + ",");
            }
            out.println();
            out.println("number of Symptom nodes:" + Symlist.size());

            for (int j = 1; j <= Symlist.size(); j++) {
                // out.println();
                // out.println("Symptoms array: " + j);
                // out.println();
                Symptom tempsym = Symlist.get(j - 1);
                out.print(tempsym.getname() + ",");
            }
            out.println();
            out.println("number of Attribute nodes:" + Attrilist.size());

            for (int k = 1; k <= Attrilist.size(); k++) {
                // out.println();
                // out.println("Attributes array: " + k);
                // out.println();
                Attribute tempattri = Attrilist.get(k - 1);
                out.print(tempattri.getname() + ",");
            }
            out.println();

        } catch (IOException e) {
            // exception handling left as an exercise for the reader
        }

    }

    public static String translate(String MedicationDBFile, String English) {
        String Bengali = "wasn't found";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(MedicationDBFile));
            String line = br.readLine();
            line = br.readLine();
            while (line != null) {
                String[] field = line.split("\t");
                if (field[0].equals(English))
                    Bengali = field[1];
            }
            br.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        if (Bengali.equals("wasn't found")) {
            // TODO implement google translate...done later...
        }
        System.out.println(English + " to " + Bengali);
        return Bengali;
    }
}
