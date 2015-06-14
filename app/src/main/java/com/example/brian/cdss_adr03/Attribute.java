package com.example.brian.cdss_adr03;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

class Attribute {
    private String TAG = "Attribute Node";

	private String name;
	private String historynote; // for mhn
	private float relevance; // weight of the attribute
	private String valuespecificrelevance; // some attribute's score is based on
											// the attribute value. ie yes =1,
											// no =0. This part contains the
											// lookup score if required...
	private String value; // possible values for a given condition
	private int present = 0; // the score based on attirbute value and
								// corresponding score from type.
	private int parent; // to find its parent
	private int alreadyAsked = 0; // making sure that once an attribute is
									// asked, the attribute won't show up on
									// main screen again. so only new attri
									// shows up.
	private String valueEntered = ""; // recording the patient's response.

	public Attribute(String nameTemp, String historynoteTemp,
			float relevanceTemp, String valuespecificrelevanceTemp,
			String valueTemp, int parentTemp) {
		name = nameTemp;
		historynote = historynoteTemp;
		relevance = relevanceTemp;
		valuespecificrelevance = valuespecificrelevanceTemp;
		value = valueTemp;
		parent = parentTemp;
	}

	public void printAttri(String filename) {
		try (PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(filename, true)))) {

			Log.i(TAG, "--->name: " + name);
            Log.i(TAG, "--->History Note: " + historynote);
            Log.i(TAG, "--->relevance: " + relevance);
            Log.i(TAG, "--->value specific relevance: " + valuespecificrelevance);
            Log.i(TAG, "--->value: " + value);
            Log.i(TAG, "--->present: " + present);
            Log.i(TAG, "--->parent: " + parent);
            Log.i(TAG, "--->value Entered: " + valueEntered);
            Log.i(TAG, "->already asked: " + alreadyAsked);

		} catch (IOException e) {
			// exception handling left as an exercise for the reader
		}

	}

	public String getname() {
		return name;
	}

	public String gethistorynote() {
		return historynote;
	}

	public float getrelevance() {
		return relevance;
	}

	public String getvaluespecificrelevance() {
		return valuespecificrelevance;
	}

	public String getvalue() {
		return value;
	}

	public int getpresent() {
		return present;
	}

	public int getparent() {
		return parent;
	}

	public int getalreadyAsked() {
		return alreadyAsked;
	}

	public String getvalueEntered() {
		return valueEntered;
	}

	public void changealreadyAsked(int newalreadyAsked) {
		alreadyAsked = newalreadyAsked;
	}

	public void changepresent(int newpresent) {
		present = newpresent;
	}

	public void changerelevance(float newrelevance) {
		relevance = newrelevance;
	}

	public void changevalueEntered(String newvalueEntered) {
		valueEntered = newvalueEntered;
	}
}
