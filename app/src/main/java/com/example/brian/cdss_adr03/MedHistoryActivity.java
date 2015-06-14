package com.example.brian.cdss_adr03;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class MedHistoryActivity extends Activity {

    private static final String TAG = MedHistoryActivity.class.getSimpleName();

    // File Names
    static String cvsSplitBy = ",";
    static int medhisdbfile =  R.raw.medhistdb;     // Database of Medical History items
    // Not used, but you should know where we're
    // pulling the questions from
    static String CurrentPtLogFile;                 // patient file for current patient. ie
    // for the prescription form.
    static String AndroidLogFile = "Android File.txt";

    static String CurrentPtMHN = "";                // medical history note for the current patient
    static String CurrentPtEHN = "";                // examination history note for current patient

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

    // Information collected from patient in AdaptiveHistoryActivity
    static ArrayList<String> medHisNote = new ArrayList<String>();

    // Information collected from patient in PhysExamActivity
    static ArrayList<String> examHisNote = new ArrayList<String>();

    // Information to be collected from patient in MedHistoryActivity
    static ArrayList<String> prevMedHis = new ArrayList<String>(); // for PtLogFile
    static ArrayList<String> notCheckedMedHis;                // the ones that aren't selected
    static ArrayList<String> prevFamHis = new ArrayList<String>(); // for PtLogFile
    static ArrayList<String> prevRxLab = new ArrayList<String>(); // for PtLogFile

    // Reference information for UI display (to be pulled from databases)
    static ArrayList<String> totalMedHis = new ArrayList<String>();
    static ArrayList<String> totalRxLab = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_med_history);

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
        Log.i("Patient Gender = ", gender);
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
        medHisNote = getIntent().getStringArrayListExtra("med-history-note");
        examHisNote = getIntent().getStringArrayListExtra("exam-history-note");


        // Spoofing totalRxLab as static ArrayList until later!
        String[] values2 = new String[] { "Previous Prescription", "Previous X-Ray",
                "Previous Ultrasound", "Previous Lab Result"};
        totalRxLab.clear();
        for (String aValues2 : values2) { totalRxLab.add(aValues2); }

        // Collect list of possible Medical History selections from medical history database
        // (medhistdb)
        totalMedHis.clear();
        InputStream is = getResources().openRawResource(medhisdbfile);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        try {
            //Skip first line of headers
            line = br.readLine();
            // Read first line of real data
            line = br.readLine();

            while (line != null) {
                String[] field = line.split(cvsSplitBy);

                if (!field[0].isEmpty())
                    totalMedHis.add(field[0]);

                line = br.readLine();       // Read next line of real data
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

        // Initialize UI elements
        final ListView prevMedHisList = (ListView) findViewById(R.id.PtHistoryListView);
        final EditText famHisField = (EditText) findViewById(R.id.FamHistoryField);
        final ListView prevRxLabList = (ListView) findViewById(R.id.PrevRxLabListView);
        final Button nextToBasicVitals = (Button) findViewById(R.id.buttonNext);

        // Setup Medical History selection menu
        final ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, android.R.layout
                .simple_list_item_multiple_choice, totalMedHis);
        prevMedHisList.setAdapter(adapter1);
        prevMedHisList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        prevMedHisList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            SparseBooleanArray sp;
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                prevMedHis.clear();
                sp=prevMedHisList.getCheckedItemPositions(); // Debug with Log.i(TAG, sp.toString());
                if (!sp.get(position))                  // Check for previous TRUE state
                    sp.delete(position);                // Remove item if supposed to be unchecked
                for(int i=0;i<sp.size();i++)
                    prevMedHis.add(totalMedHis.get(sp.keyAt(i)));
            }
        });

        // Setup Previous Rx/Lab selection menu
        final ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout
                .simple_list_item_multiple_choice, totalRxLab);
        prevRxLabList.setAdapter(adapter2);
        prevRxLabList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        prevRxLabList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            SparseBooleanArray sp;
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                prevRxLab.clear();
                sp=prevRxLabList.getCheckedItemPositions(); // Debug with Log.i(TAG, sp.toString());
                if (!sp.get(position))                  // Check for previous TRUE state
                    sp.delete(position);                // Remove item if supposed to be unchecked
                for(int i=0;i<sp.size();i++)
                    prevRxLab.add(totalRxLab.get(sp.keyAt(i)));
            }
        });

        // Next button
        // Log all fields that aren't dynamically updated, and send all collected information
        // to next Activity
        nextToBasicVitals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Record whatever is in the Family History text field for patient file
                prevFamHis.add(famHisField.getText().toString());

                // Subtract prevMedHis from totalMedHis to get the list of
                notCheckedMedHis = new ArrayList<String>(totalMedHis);

                for (int i = 0; i < prevMedHis.size(); i++) {
                    notCheckedMedHis.remove(prevMedHis.get(i));
                }

                Log.i(TAG, prevMedHis.toString());
                Log.i(TAG, prevFamHis.toString());
                Log.i(TAG, prevRxLab.toString());

                printToLognoComma(CurrentPtLogFile, "Medical History",
                        prevMedHis);
                printToLognoComma(CurrentPtLogFile, "Family History",
                        prevFamHis);
                printToLognoComma(
                        CurrentPtLogFile,
                        "Patient brought in these prescriptions and test results",
                        prevRxLab);
                printToLognoComma(AndroidLogFile, "Medical History",
                        prevMedHis);
                printToLognoComma(AndroidLogFile, "Family History",
                        prevFamHis);
                printToLognoComma(
                        AndroidLogFile,
                        "Patient brought in these prescriptions and test results",
                        prevRxLab);



                Intent MainIntent = new Intent(view.getContext(),MainActivity.class);
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_med_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public static void calcMedHistoryMatch(ArrayList<String> checkedMedHis,
                                           ArrayList<String> uncheckedMedHis, ArrayList<Symptom> Symlist,
                                           ArrayList<Attribute> Attrilist) {

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
            if (tempSymname.equals("Risk Factor")&&tempsym.getalreadyAsked()==0) {
                for (int k = tempsym.getattribegin(); k <= tempsym
                        .getattriend(); k++) {
                    tempattri = Attrilist.get(k);

                    for (int l = 0; l < checkedMedHis.size(); l++) {
                        if (Attrilist.get(k).getname()
                                .equals(checkedMedHis.get(l))) {
                            // System.out.println("changed this cond to positive");
                            tempattri.changepresent(1);
                            tempattri.changealreadyAsked(1);
                            tempattri.changevalueEntered("Yes");

                            if (tempattri.getvalue().contains(";")) {
                                String[] evalyesno = tempattri.getvalue()
                                        .split(";");
                                for (int o = 0; o < evalyesno.length; o++) {
                                    if (evalyesno[o].equals("Yes")) {
                                        // System.out
                                        // .println("match found for "
                                        // + tempattrivalue);
                                        tempattri
                                                .changerelevance(Float
                                                        .parseFloat(tempattri
                                                                .getvaluespecificrelevance()
                                                                .split(";")[o]));
                                    }
                                }
                            } else if (tempattri.getvalue().equals("No")) {
                                tempattri.changerelevance(-(Float
                                        .parseFloat(tempattri
                                                .getvaluespecificrelevance())));
                            }
                        }
                    }
                    for (int l = 0; l < uncheckedMedHis.size(); l++) {
                        if (Attrilist.get(k).getname()
                                .equals(checkedMedHis.get(l))) {
                            tempattri = Attrilist.get(k);
                            // System.out.println("changed this cond to positive");
                            tempattri.changepresent(-1);
                            tempattri.changealreadyAsked(-1);
                            tempattri.changevalueEntered("No");

                            if (tempattri.getvalue().contains(";")) {
                                String[] evalyesno = tempattri.getvalue()
                                        .split(";");
                                for (int o = 0; o < evalyesno.length; o++) {
                                    if (evalyesno[o].equals("No")) {
                                        // System.out
                                        // .println("match found for "
                                        // + tempattrivalue);
                                        tempattri
                                                .changerelevance(-(Float
                                                        .parseFloat(tempattri
                                                                .getvaluespecificrelevance()
                                                                .split(";")[o])));
                                    }
                                }
                            } else if (tempattri.getvalue().equals("No")) {
                                tempattri.changerelevance(-(Float
                                        .parseFloat(tempattri
                                                .getvaluespecificrelevance())));
                            }
                        }
                    }

                    tempSymAttriSum = tempsym.getsumattri();
                    tempattripresent = tempattri.getpresent();
                    tempattrirelevance = tempattri.getrelevance();
                    if (Math.abs(tempattrirelevance) > 0.00001)
                        tempSymAttriSum += tempattripresent
                                * (1 / (Math.floor(tempattrirelevance * 4) / 4));
                    tempsym.changesumattri(tempSymAttriSum);
                }
                tempattrisum = tempsym.getsumattri();
                if (tempsym.gettotalprimaryattri() != 0) {
                    tempsympresent = tempsym.getsumattri()
                            / tempsym.gettotalprimaryattri();
                } else {
                    // //TODO NEED TO BE CHANGED LATER!!
                    Log.i(TAG, "this sym did not have any primary attri: " + tempsym.getname());
                    tempsympresent = tempsym.getsumattri();
                }
                tempsym.changepresent(tempsympresent);
                tempsym.changealreadyAsked(1);

                // System.out.println();

            }
        }
    }

    public void printToLognoComma(String CurrentPtLogFile, String category, ArrayList toPrint) {

        File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS");
        if (!folder.exists()) folder.mkdir();

        File file = new File(folder, CurrentPtLogFile);
        Log.i(TAG, file.toString());

        try
        {
            BufferedWriter writer= new BufferedWriter(new FileWriter(file, true));

            writer.append(category);
            writer.append(":,");
            for (int i = 0; i < toPrint.size(); i++) {
                writer.append(toPrint.get(i).toString());
                writer.append(";");
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
}
