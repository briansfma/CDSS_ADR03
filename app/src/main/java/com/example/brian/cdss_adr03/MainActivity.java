package com.example.brian.cdss_adr03;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    final Context context = this;
    private static final int REQUEST_CODE_CAPTURE_PT_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private static final int REQUEST_CODE_CAPTURE_ADD_IMAGE = 4;

    // Google Drive API Client for uploading patient photo, CSV of patient data, and add'l photos
    private GoogleApiClient mGoogleApiClient;

    // Bitmap object for uploading to Google Drive
    private Bitmap mBitmapToSave;

    // File Names
    static String cvsSplitBy = ",";
    static String PtDBFile = "patientdb.txt";       // You should know where we're
                                                    // pulling returning patient data from
    static String CurrentPtLogFile;                 // patient file for current patient. ie
                                                    // for the prescription form.
    static String AndroidLogFile = "Android File.csv";
    static String CurrentPtMHN = "";                // medical history note for the current patient
    static String CurrentPtEHN = "";                // examination history note for current patient

    static String mCurrentPhotoPath;                // File path for patient photo
    private WebView mWebView;                       // For printing HTML received back from Meteor

    // Status Options
    private static final String[] STATUSES = new String[] { "New", "Review", "Check-up" };

    // Information collected from the patient in MainActivity
    private String status;
    private String date;
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

    // Fields to be filled and updated between local information and/or pull from Internet
    ImageButton ptPhoto;
    EditText ptRegNo;
    AutoCompleteTextView ptStatus;
    TextView ptDate;
    EditText ptName;
    EditText ptSDW;
    EditText ptOccup;
    EditText ptPhone;
    EditText ptAddress;
    EditText ptAge;
    EditText ptGender;
    EditText ptHeight;
    EditText ptBPSys;
    EditText ptBPDia;
    EditText ptBMI;
    EditText ptWeight;
    EditText ptPulse;
    EditText ptSpo2;
    EditText ptTemp;
    EditText ptMedHistory;
    EditText ptFamHistory;
    EditText ptPrevRxLab;
    EditText ptCO;
    EditText ptElicit;
    ImageButton addPhoto;
    Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Declare UI elements (too many lines to read) and fill fields if we have data
        // from previous calling activity
        initializeFields();

        // Set behavior of UI fields

        // Registration number: If Patient is found in records, auto-fill basic information
        ptRegNo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                regNo = ptRegNo.getText().toString();

                //checkAndFillReviewPt(regNo, PtDBFile);
            }
        });

        // Patient Height and Weight
        // Auto-calculate BMI upon having entered both height and weight fields
        // (order irrespective)
        ptHeight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!ptHeight.getText().toString().isEmpty()) {
                    height = Integer.parseInt(ptHeight.getText().toString());

                    if (!ptWeight.getText().toString().isEmpty()) {
                        bmi = weight / Math.pow((height * 0.01), 2);
                        DecimalFormat df = new DecimalFormat("####0.0");
                        ptBMI.setText(df.format(bmi));
                        Log.i("BMI", Double.toString(bmi));
                    }
                }
            }
        });
        ptWeight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!ptWeight.getText().toString().isEmpty()) {
                    weight = Integer.parseInt(ptWeight.getText().toString());

                    if (!ptHeight.getText().toString().isEmpty()) {
                        bmi = weight / Math.pow((height * 0.01), 2);
                        DecimalFormat df = new DecimalFormat("####0.0");
                        ptBMI.setText(df.format(bmi));
                        Log.i("BMI", Double.toString(bmi));
                    }
                }
            }
        });

        // Medical History: Switch to the Med History window when clicked
        ptMedHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMedHisIntent();
            }
        });

        // Family History: Switch to the Med History window when clicked
        ptFamHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMedHisIntent();
            }
        });

        // Previous Rx, Lab Result, USG, etc. Switch to the Med History window when clicked
        ptPrevRxLab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMedHisIntent();
            }
        });

        // Chief Complaint: Switch to the Adaptive History window when clicked
        ptCO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (prevMedHis == null) {
                    // Display a quick Dialog to instruct the HA to go back and now ask questions
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Medical History not asked! Please ask patient first.")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    startMedHisIntent();
                                }
                            });
                    // create alert dialog
                    AlertDialog AlertAskQsDialog = builder.create();

                    // show it
                    AlertAskQsDialog.show();
                }
                else {
                    startAdaptiveHisIntent();
                }
            }
        });

        // On Examination: Switch to the Physical Exam window when clicked
        ptElicit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPhysExamIntent();
            }
        });

        // Patient's Photo: Setup image capture when clicked
        ptPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture(REQUEST_CODE_CAPTURE_PT_IMAGE);
            }
        });

        // Additional Photo: Setup image capture when clicked
        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture(REQUEST_CODE_CAPTURE_ADD_IMAGE);
            }
        });

        // Google Drive uploads setup
        setupGoogleDriveClient();

        // Submit Button: When clicked, currently sends an Rx to Google Drive
        // TODO Submit the Rx to Meteor using POST (RESTful API)

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save current contents to Android device
                ArrayList<String> ptDBInfo = new ArrayList<String>();
                ptDBInfo.add(name);
                ptDBInfo.add(status);
                ptDBInfo.add(phone);
                ptDBInfo.add(regNo);
                ptDBInfo.add(sdw);
                ptDBInfo.add(occup);
                ptDBInfo.add(address);
                ptDBInfo.add(gender);
                ptDBInfo.add(date);
                printToPtDB(ptDBInfo);

//                // Google Drive upload
//                if (!(mGoogleApiClient == null)) {
//                    saveRxToDrive();
//                }

                // TODO Here is the METEOR server connection
                // Sync once, Rx Form to Meteor server
                new SyncUp().execute();
            }
        });


        // TODO Print button functionality
        // Setup "Print" button retrieve HTML from Meteor (MD client) and print
        /*final Button printButton = (Button)findViewById(R.id.PrintButton);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doWebViewPrint();
            }
        });*/
    }

    // Define active fields on Rx Form
    private void initializeFields() {
        ptPhoto = (ImageButton)findViewById(R.id.PtPhotoButton);
        ptRegNo = (EditText)findViewById(R.id.RegNoField);

        ptStatus = (AutoCompleteTextView)findViewById(R.id.StatusField);    // Autocomplete setup
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, STATUSES);
        ptStatus.setAdapter(adapter);

        ptDate = (TextView)findViewById(R.id.DateField);                    // Set today's date
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy");
        date = ft.format(dNow);
        ptDate.setText(date);

        ptName = (EditText)findViewById(R.id.NameField);
        ptSDW = (EditText)findViewById(R.id.SDWField);
        ptOccup = (EditText)findViewById(R.id.OccupField);
        ptPhone = (EditText)findViewById(R.id.PhoneField);
        ptAddress = (EditText)findViewById(R.id.AddressField);
        ptAge = (EditText)findViewById(R.id.AgeField);
        ptGender = (EditText)findViewById(R.id.GenderField);
        ptHeight = (EditText)findViewById(R.id.HeightField);
        ptBPSys = (EditText)findViewById(R.id.BPSysField);
        ptBPDia = (EditText)findViewById(R.id.BPDiaField);
        ptBMI = (EditText)findViewById(R.id.BMIField);
        ptWeight = (EditText)findViewById(R.id.WeightField);
        ptPulse = (EditText)findViewById(R.id.PulseField);
        ptSpo2 = (EditText)findViewById(R.id.SpO2Field);
        ptTemp = (EditText)findViewById(R.id.TempField);
        ptMedHistory = (EditText)findViewById(R.id.MedHistoryField);
        ptFamHistory = (EditText)findViewById(R.id.FamHistoryField);
        ptPrevRxLab = (EditText)findViewById(R.id.PrevRxLabField);
        ptCO = (EditText)findViewById(R.id.COField);
        ptElicit = (EditText)findViewById(R.id.ElicitField);
        addPhoto = (ImageButton)findViewById(R.id.AddPhotoButton);
        submitButton = (Button)findViewById(R.id.SubmitButton);

        // If we are coming in from another Activity, grab data from the calling Intent
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
        examHisNote = getIntent().getStringArrayListExtra("exam-history-note");

        // Fill in what we can!
        ptStatus.setText(status);
        ptRegNo.setText(regNo);
        ptName.setText(name);
        ptPhone.setText(phone);
        if (age != 0)
            ptAge.setText(Integer.toString(age));
        ptGender.setText(gender);
        ptSDW.setText(sdw);
        ptOccup.setText(occup);
        ptAddress.setText(address);
        if (height != 0)
            ptHeight.setText(Integer.toString(height));
        if (weight != 0)
            ptWeight.setText(Integer.toString(weight));
        if (bmi != 0) {
            DecimalFormat df = new DecimalFormat("####0.0");
            ptBMI.setText(df.format(bmi));
        }
        if (pulse != 0)
            ptPulse.setText(Integer.toString(pulse));
        if (bpSys != 0)
            ptBPSys.setText(Integer.toString(bpSys));
        if (bpDia != 0)
            ptBPDia.setText(Integer.toString(bpDia));
        if (temp != 0) {
            DecimalFormat df = new DecimalFormat("####0.0");
            ptTemp.setText(df.format(temp));
        }
        if (spo2 != 0)
            ptSpo2.setText(Integer.toString(spo2));
        ptMedHistory.setText(ArrayListToText(prevMedHis));
        ptFamHistory.setText(ArrayListToText(prevFamHis));
        ptPrevRxLab.setText(ArrayListToText(prevRxLab));
        ptCO.setText(ArrayListToText(medHisNote));
        ptElicit.setText(ArrayListToText(examHisNote));
    }

    // Takes in an ArrayList and converts it to a multiline String for insertion into a TextView
    private String ArrayListToText(ArrayList<String> note) {
        String toPrint = "";
        if (note != null) {
            for (int i = 0; i < note.size(); i++) {
                toPrint += note.get(i);
                if (i < note.size() - 1) {
                    toPrint += "\n";
                }
            }
        }
        return toPrint;
    }

    private void checkAndFillReviewPt(String regNo, String ptDBFile) {
        FileInputStream fis;
        String line;

        try {
            File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS/Patients");
            fis = new FileInputStream (new File(folder, ptDBFile));
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            //Skip first line of headers
            line = br.readLine();
            // Read first line of real data
            line = br.readLine();

            while (line != null) {
                String[] field = line.split(cvsSplitBy);

                if (field[3].equals(regNo)) {
                    // Write values into the UI fields
                    ptName.setText(field[0]);
                    ptPhone.setText(field[2]);
                    ptSDW.setText(field[4]);
                    ptOccup.setText(field[5]);
                    ptAddress.setText(field[6]);
                    ptAge.setText(field[7]);
                    ptGender.setText(field[8]);

                    break;
                }
                // Read next line of real data
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ptName.getText().toString().equals("")) {
            CharSequence text = "Patient was not found!";
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(getApplicationContext(), text, duration).show();
        }
    }

    private void takePicture(int requestCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, requestCode);
            }
        }
    }

    private void setupGoogleDriveClient() {
        // Google Drive Uploads
        // Start connection from Rx Form to Google Docs
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(MainActivity.this)
                    .addOnConnectionFailedListener(MainActivity.this)
                    .build();
        }
        // Connect the client. Once connected, we execute whatever's in OnConnected.
        mGoogleApiClient.connect();
    }

    private void startMedHisIntent() {
        if (checkReqFieldsOk()) {
            saveVars();

            Intent MedHisIntent = new Intent(MainActivity.this, MedHistoryActivity.class);

            MedHisIntent.putExtra("pt-log-file", CurrentPtLogFile);
            MedHisIntent.putExtra("pt-mhn-file", CurrentPtMHN);
            MedHisIntent.putExtra("pt-ehn-file", CurrentPtEHN);
            MedHisIntent.putExtra("status", status);
            MedHisIntent.putExtra("regNo", regNo);
            MedHisIntent.putExtra("name", name);
            MedHisIntent.putExtra("phone", phone);
            MedHisIntent.putExtra("age", age);
            MedHisIntent.putExtra("gender", gender);
            MedHisIntent.putExtra("sdw", sdw);
            MedHisIntent.putExtra("occup", occup);
            MedHisIntent.putExtra("address", address);
            MedHisIntent.putExtra("height", height);
            MedHisIntent.putExtra("weight", weight);
            MedHisIntent.putExtra("bmi", bmi);
            MedHisIntent.putExtra("pulse", pulse);
            MedHisIntent.putExtra("bpSys", bpSys);
            MedHisIntent.putExtra("bpDia", bpDia);
            MedHisIntent.putExtra("temp", temp);
            MedHisIntent.putExtra("spo2", spo2);
            MedHisIntent.putStringArrayListExtra("prev-med-history", prevMedHis);
            MedHisIntent.putStringArrayListExtra("prev-fam-history", prevFamHis);
            MedHisIntent.putStringArrayListExtra("prev-rx-lab", prevRxLab);
            MedHisIntent.putStringArrayListExtra("med-history-note", medHisNote);
            MedHisIntent.putStringArrayListExtra("exam-history-note", examHisNote);

            startActivity(MedHisIntent);
            finish();
        }
    }

    private void startAdaptiveHisIntent() {
        if (checkReqFieldsOk()) {
            saveVars();

            Intent AdaptiveHistoryIntent = new Intent(MainActivity.this, AdaptiveHistoryActivity.class);

            AdaptiveHistoryIntent.putExtra("pt-log-file", CurrentPtLogFile);
            AdaptiveHistoryIntent.putExtra("pt-mhn-file", CurrentPtMHN);
            AdaptiveHistoryIntent.putExtra("pt-ehn-file", CurrentPtEHN);
            AdaptiveHistoryIntent.putExtra("status", status);
            AdaptiveHistoryIntent.putExtra("regNo", regNo);
            AdaptiveHistoryIntent.putExtra("name", name);
            AdaptiveHistoryIntent.putExtra("phone", phone);
            AdaptiveHistoryIntent.putExtra("age", age);
            AdaptiveHistoryIntent.putExtra("gender", gender);
            AdaptiveHistoryIntent.putExtra("sdw", sdw);
            AdaptiveHistoryIntent.putExtra("occup", occup);
            AdaptiveHistoryIntent.putExtra("address", address);
            AdaptiveHistoryIntent.putExtra("height", height);
            AdaptiveHistoryIntent.putExtra("weight", weight);
            AdaptiveHistoryIntent.putExtra("bmi", bmi);
            AdaptiveHistoryIntent.putExtra("pulse", pulse);
            AdaptiveHistoryIntent.putExtra("bpSys", bpSys);
            AdaptiveHistoryIntent.putExtra("bpDia", bpDia);
            AdaptiveHistoryIntent.putExtra("temp", temp);
            AdaptiveHistoryIntent.putExtra("spo2", spo2);
            AdaptiveHistoryIntent.putStringArrayListExtra("prev-med-history", prevMedHis);
            AdaptiveHistoryIntent.putStringArrayListExtra("prev-fam-history", prevFamHis);
            AdaptiveHistoryIntent.putStringArrayListExtra("prev-rx-lab", prevRxLab);
            AdaptiveHistoryIntent.putStringArrayListExtra("med-history-note", medHisNote);
            AdaptiveHistoryIntent.putStringArrayListExtra("exam-history-note", examHisNote);

            startActivity(AdaptiveHistoryIntent);
            finish();
        }
    }

    private void startPhysExamIntent() {
        if (checkReqFieldsOk()) {
            saveVars();

            Intent PhysExamIntent = new Intent(MainActivity.this, PhysExamActivity.class);

            PhysExamIntent.putExtra("pt-log-file", CurrentPtLogFile);
            PhysExamIntent.putExtra("pt-mhn-file", CurrentPtMHN);
            PhysExamIntent.putExtra("pt-ehn-file", CurrentPtEHN);
            PhysExamIntent.putExtra("status", status);
            PhysExamIntent.putExtra("regNo", regNo);
            PhysExamIntent.putExtra("name", name);
            PhysExamIntent.putExtra("phone", phone);
            PhysExamIntent.putExtra("age", age);
            PhysExamIntent.putExtra("gender", gender);
            PhysExamIntent.putExtra("sdw", sdw);
            PhysExamIntent.putExtra("occup", occup);
            PhysExamIntent.putExtra("address", address);
            PhysExamIntent.putExtra("height", height);
            PhysExamIntent.putExtra("weight", weight);
            PhysExamIntent.putExtra("bmi", bmi);
            PhysExamIntent.putExtra("pulse", pulse);
            PhysExamIntent.putExtra("bpSys", bpSys);
            PhysExamIntent.putExtra("bpDia", bpDia);
            PhysExamIntent.putExtra("temp", temp);
            PhysExamIntent.putExtra("spo2", spo2);
            PhysExamIntent.putStringArrayListExtra("prev-med-history", prevMedHis);
            PhysExamIntent.putStringArrayListExtra("prev-fam-history", prevFamHis);
            PhysExamIntent.putStringArrayListExtra("prev-rx-lab", prevRxLab);
            PhysExamIntent.putStringArrayListExtra("med-history-note", medHisNote);
            PhysExamIntent.putStringArrayListExtra("exam-history-note", examHisNote);

            startActivity(PhysExamIntent);
            finish();
        }
    }

    private boolean checkReqFieldsOk() {
        if (ptRegNo.getText().toString().isEmpty()) {
            CharSequence text = "No Registration Number! Please enter Registration Number";
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getApplicationContext(), text, duration).show();
            return false;
        }
        else if (ptName.getText().toString().isEmpty()) {
            CharSequence text = "No Name Entered! Please enter Name";
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getApplicationContext(), text, duration).show();
            return false;
        }
        else if (ptAge.getText().toString().isEmpty()) {
            CharSequence text = "No Age Entered! Please enter Age";
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getApplicationContext(), text, duration).show();
            return false;
        }
        else if (ptGender.getText().toString().isEmpty()) {
            CharSequence text = "No Gender Entered! Please enter Gender";
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getApplicationContext(), text, duration).show();
            return false;
        }
        else
            return true;
    }

    private void saveVars() {
        // set file names where we keep the local patient log/prescription/note files
        CurrentPtLogFile = date + "_" + regNo + "_" + name + ".csv";
        CurrentPtMHN = date + "_" + regNo + "_" + name + "_MHN.txt";
        CurrentPtEHN = date + "_" + regNo + "_" + name + "_EHN.txt";

        // Record all fields as filled thus far
        regNo = ptRegNo.getText().toString();
        status = ptStatus.getText().toString();
        name = ptName.getText().toString();
        sdw = ptSDW.getText().toString();
        occup = ptOccup.getText().toString();
        phone = ptPhone.getText().toString();
        address = ptAddress.getText().toString();
        if (!ptAge.getText().toString().isEmpty())
            age = Integer.parseInt(ptAge.getText().toString());
        gender = ptGender.getText().toString();
        if (!ptHeight.getText().toString().isEmpty())
            height = Integer.parseInt(ptHeight.getText().toString());
        if (!ptWeight.getText().toString().isEmpty())
            weight = Integer.parseInt(ptWeight.getText().toString());
        if (!ptBPSys.getText().toString().isEmpty())
            bpSys = Integer.parseInt(ptBPSys.getText().toString());
        if (!ptBPDia.getText().toString().isEmpty())
            bpDia = Integer.parseInt(ptBPDia.getText().toString());
        if (!ptPulse.getText().toString().isEmpty())
            pulse = Integer.parseInt(ptPulse.getText().toString());
        if (!ptSpo2.getText().toString().isEmpty())
            spo2 = Integer.parseInt(ptSpo2.getText().toString());
        if (!ptTemp.getText().toString().isEmpty())
            temp = Float.parseFloat(ptTemp.getText().toString());
    }

    public void printToPtDB(ArrayList toPrint) {

        if (checkReqFieldsOk()) {
            File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS/Patients");
            if (!folder.exists()) folder.mkdir();

            File file = new File(folder, PtDBFile);

            try
            {
                BufferedWriter writer= new BufferedWriter(new FileWriter(file, true));

                for (int i = 0; i < toPrint.size(); i++) {
                    writer.append(toPrint.get(i).toString());
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

    class SyncUp extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... arg0) {
            String text =null;
            try {
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost("http://192.168.1.157:3000/api/rx/create");
                // TODO Set new URL every day!

                List<NameValuePair> pairs = new ArrayList<>();
                pairs.add(new BasicNameValuePair("code", ptRegNo.getText().toString()));
                pairs.add(new BasicNameValuePair("status", ptStatus.getText().toString()));
                pairs.add(new BasicNameValuePair("attending_ha", "00000"));
                pairs.add(new BasicNameValuePair("seen_by_md", "false"));
                pairs.add(new BasicNameValuePair("date", ptDate.getText().toString()));
                pairs.add(new BasicNameValuePair("name", ptName.getText().toString()));
                pairs.add(new BasicNameValuePair("sdw", ptSDW.getText().toString()));
                pairs.add(new BasicNameValuePair("occup", ptOccup.getText().toString()));
                pairs.add(new BasicNameValuePair("phone", ptPhone.getText().toString()));
                pairs.add(new BasicNameValuePair("address", ptAddress.getText().toString()));
                pairs.add(new BasicNameValuePair("age", ptAge.getText().toString()));
                pairs.add(new BasicNameValuePair("gender", ptGender.getText().toString()));
                pairs.add(new BasicNameValuePair("cc", ptCO.getText().toString()));
                pairs.add(new BasicNameValuePair("height", ptHeight.getText().toString()));
                pairs.add(new BasicNameValuePair("weight", ptWeight.getText().toString()));
                pairs.add(new BasicNameValuePair("bmi", ptBMI.getText().toString()));
                pairs.add(new BasicNameValuePair("bp", ptBPSys.getText().toString() + "/" + ptBPDia.getText().toString()));
                pairs.add(new BasicNameValuePair("pulse", ptPulse.getText().toString()));
                pairs.add(new BasicNameValuePair("spo2", ptSpo2.getText().toString()));
                pairs.add(new BasicNameValuePair("temp", ptTemp.getText().toString()));
                pairs.add(new BasicNameValuePair("famHis", ptFamHistory.getText().toString()));
                pairs.add(new BasicNameValuePair("medHis", ptMedHistory.getText().toString()));
                pairs.add(new BasicNameValuePair("prevRx", ptPrevRxLab.getText().toString()));

                post.setEntity(new UrlEncodedFormEntity(pairs));

                HttpResponse response = client.execute(post);
                text = EntityUtils.toString(response.getEntity());
                Log.i(TAG,text);


            } catch (ParseException e) {
                e.printStackTrace();
                Log.i("Parse Exception", e + "");
            } catch (IOException e) {
                e.printStackTrace();
            }

            return text;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ptRegNo.setText(result);
        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");

    }

    private void saveRxToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");

        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(uploadPic1Callback);

        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(uploadCSVCallback);

        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(uploadMHNCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_CAPTURE_PT_IMAGE:
                // Called after a photo has been taken.
                if (resultCode == Activity.RESULT_OK) {
                    // Get the dimensions of the View
                    final ImageView ptPhoto = (ImageView) findViewById(R.id.PtPhotoButton);
                    int targetH = ptPhoto.getHeight();

                    // Get the dimensions of the bitmap
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    bmOptions.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                    int photoH = bmOptions.outHeight;

                    // Decode the image file into a Bitmap sized to fill the View
                    bmOptions.inJustDecodeBounds = false;
                    bmOptions.inSampleSize = photoH/targetH;

                    mBitmapToSave = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                    ptPhoto.setImageBitmap(mBitmapToSave);

                    // IF the Google API Client is up, then do the following:

//                    Drive.DriveApi.newDriveContents(mGoogleApiClient)
//                            .setResultCallback(uploadPic1Callback);
                }
                break;
            case REQUEST_CODE_CAPTURE_ADD_IMAGE:
                // Called after a photo has been taken.
                if (resultCode == Activity.RESULT_OK) {
                    // Get the dimensions of the View
                    final ImageView addPhoto = (ImageView) findViewById(R.id.AddPhotoButton);
                    int targetH = addPhoto.getHeight();

                    // Get the dimensions of the bitmap
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    bmOptions.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                    int photoH = bmOptions.outHeight;

                    // Decode the image file into a Bitmap sized to fill the View
                    bmOptions.inJustDecodeBounds = false;
                    bmOptions.inSampleSize = photoH/targetH;

                    mBitmapToSave = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                    addPhoto.setImageBitmap(mBitmapToSave);

//                    Drive.DriveApi.newDriveContents(mGoogleApiClient)
//                            .setResultCallback(uploadPic2Callback);
                }
                break;
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Image successfully saved.");

                    mBitmapToSave = null;
                }
                break;
            case REQUEST_CODE_RESOLUTION:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    final private ResultCallback<DriveApi.DriveContentsResult> uploadPic1Callback = new
            ResultCallback<DriveApi.DriveContentsResult>() {

                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    // If the operation was not successful, we cannot do anything
                    // and must
                    // fail.
                    if (!result.getStatus().isSuccess()) {
                        Log.i(TAG, "Failed to create new contents.");
                        return;
                    }
                    // Otherwise, we can write our data to the new contents.
                    Log.i(TAG, "New image created.");
                    // Get an output stream for the contents.
                    OutputStream outputStream = result.getDriveContents().getOutputStream();
                    // Write the bitmap data from it.
                    ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();

                    // Compress image
                    final Bitmap image = mBitmapToSave;
                    image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);

                    // Write compressed data to byte array
                    try {
                        outputStream.write(bitmapStream.toByteArray());
                    } catch (IOException e1) {
                        Log.i(TAG, "Unable to write image contents.");
                    }
                    // For the image:
                    // Create the initial metadata - MIME type and title.
                    MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                            .setTitle("Android File 01.png")
                            .setMimeType("image/jpeg").build();
                    // Create the file in the root folder of Drive
                    Drive.DriveApi.getRootFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, metadataChangeSet, result.getDriveContents())
                            .setResultCallback(fileCallback);
                }
            };

    final private ResultCallback<DriveApi.DriveContentsResult> uploadPic2Callback = new
            ResultCallback<DriveApi.DriveContentsResult>() {

                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    // If the operation was not successful, we cannot do anything
                    // and must
                    // fail.
                    if (!result.getStatus().isSuccess()) {
                        Log.i(TAG, "Failed to create new contents.");
                        return;
                    }
                    // Otherwise, we can write our data to the new contents.
                    Log.i(TAG, "New image created.");
                    // Get an output stream for the contents.
                    OutputStream outputStream = result.getDriveContents().getOutputStream();
                    // Write the bitmap data from it.
                    ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();

                    // Compress image
                    final Bitmap image = mBitmapToSave;
                    image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);

                    // Write compressed data to byte array
                    try {
                        outputStream.write(bitmapStream.toByteArray());
                    } catch (IOException e1) {
                        Log.i(TAG, "Unable to write image contents.");
                    }
                    // For the image:
                    // Create the initial metadata - MIME type and title.
                    MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                            .setTitle("Android File 02.png")
                            .setMimeType("image/jpeg").build();
                    // Create the file in the root folder of Drive
                    Drive.DriveApi.getRootFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, metadataChangeSet, result.getDriveContents())
                            .setResultCallback(fileCallback);
                }
            };

    final private ResultCallback<DriveApi.DriveContentsResult> uploadCSVCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {

                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    // If the operation was not successful, we cannot do anything
                    // and must
                    // fail.
                    if (!result.getStatus().isSuccess()) {
                        Log.i(TAG, "Failed to create new contents.");
                        return;
                    }
                    // Otherwise, we can write our data to the new contents.
                    Log.i(TAG, "New CSV created.");

                    // Get an output stream for the contents.
                    OutputStream outputStream = result.getDriveContents().getOutputStream();

                    // Read CSV back from internal storage
                    FileInputStream fis;
                    String content = "";
                    try {
                        File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS");
                        fis = new FileInputStream (new File(folder, AndroidLogFile));
                        byte[] input = new byte[fis.available()];
                        while (fis.read(input) != -1) {
                            content += new String(input);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Write compressed data to byte array
                    try {
                        outputStream.write(content.getBytes());
                    } catch (IOException e1) {
                        Log.i(TAG, "Unable to write CSV contents.");
                    }

                    // For the new text file:
                    // Create the initial metadata - MIME type and title.
                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("Android File.csv")
                            .setMimeType("text/csv").build();
                    // Create a file in the root folder
                    Drive.DriveApi.getRootFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, changeSet, result.getDriveContents())
                            .setResultCallback(fileCallback);
                }
            };

    final private ResultCallback<DriveApi.DriveContentsResult> uploadMHNCallback = new
            ResultCallback<DriveApi.DriveContentsResult>() {

                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    // If the operation was not successful, we cannot do anything
                    // and must
                    // fail.
                    if (!result.getStatus().isSuccess()) {
                        Log.i(TAG, "Failed to create new contents.");
                        return;
                    }
                    // Otherwise, we can write our data to the new contents.
                    Log.i(TAG, "New MHN created.");

                    // Get an output stream for the contents.
                    OutputStream outputStream = result.getDriveContents().getOutputStream();

                    // Read CSV back from internal storage
                    FileInputStream fis;
                    String content = "";
                    try {
                        File folder = new File(Environment.getExternalStorageDirectory() + "/CDSS");
                        fis = new FileInputStream (new File(folder, CurrentPtMHN));
                        byte[] input = new byte[fis.available()];
                        while (fis.read(input) != -1) {
                            content += new String(input);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Write compressed data to byte array
                    try {
                        outputStream.write(content.getBytes());
                    } catch (IOException e1) {
                        Log.i(TAG, "Unable to write MHN contents.");
                    }

                    // For the new text file:
                    // Create the initial metadata - MIME type and title.
                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("Android File MHN.txt")
                            .setMimeType("text/plain").build();
                    // Create a file in the root folder
                    Drive.DriveApi.getRootFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, changeSet, result.getDriveContents())
                            .setResultCallback(fileCallback);
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        String msg = ("Error while trying to create the file");
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                        return;
                    }
                    String msg = ("Created a file with content: " + result.getDriveFile().getDriveId());
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                }
            };

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("ddMMyy_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void doWebViewPrint() {
        // Create a WebView object specifically for printing
        WebView webView = new WebView(context);
        webView.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i(TAG, "page finished loading " + url);
                createWebPrintJob(view);
                mWebView = null;
            }
        });

        // Print an existing web page (remember to request INTERNET permission!):
        webView.loadUrl("http://192.168.1.157:3000/"); // TODO Remember to set new URL every day!

        // Keep a reference to WebView object until you pass the PrintDocumentAdapter
        // to the PrintManager
        mWebView = webView;
    }

    private void createWebPrintJob(WebView webView) {

        // Get a PrintManager instance
        PrintManager printManager = (PrintManager) context
                .getSystemService(Context.PRINT_SERVICE);

        // Create a job name
        String jobName = getString(R.string.app_name) + " Document";

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            // Get a print adapter instance
            PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
            // Create the print job with name and adapter instance
            PrintJob printJob = printManager.print(jobName, printAdapter,
                    new PrintAttributes.Builder().build());
        }
        else { // For API version 19 and 20
            // Get a print adapter instance
            PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter();
            // Create the print job with name and adapter instance
            PrintJob printJob = printManager.print(jobName, printAdapter,
                    new PrintAttributes.Builder().build());
        }
    }

    public void printToLog(String CurrentPtLogFile, String category, ArrayList toPrint) {

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