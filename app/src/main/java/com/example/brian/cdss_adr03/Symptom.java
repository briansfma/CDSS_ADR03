package com.example.brian.cdss_adr03;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

class Symptom {
    private String TAG = "Symptom Node";

	private String name;
	// private float score = 0;
	private float relevance; // weight of the symptom
	private float present = 0; // whether if the Symptom was present. In the
								// code, looks at the first attribute and match
								// Yes/No, and then get the appropriate value to
								// put in present
	private String parent; // in order to look for parent
	private float sumattri = 0; // to calculate symptoms score. each time an
								// attribute is asked, the score is added to sym
								// attri. Sum attri/total primary = sym score.
	private int attribegin; // keep track of its children
	private int attriend;// keep track of its children
	private int totalprimaryattri; // to ensure we get 70% +
	private int alreadyAsked = 0; // making sure that once a symptoms is asked,
									// the symptom won't show up on the Other
									// Sym list.

	public Symptom(String nameTemp, float relevanceTemp, String parentTemp,
			int attribeginTemp, int attriendTemp, int totalprimaryattriTemp,
			float presentTemp) {
		name = nameTemp;
		relevance = relevanceTemp;
		parent = parentTemp;
		attribegin = attribeginTemp;
		attriend = attriendTemp;
		totalprimaryattri = totalprimaryattriTemp;
		present = presentTemp;
		alreadyAsked = (int) presentTemp;
	}

	public void printSym(String filename) {

		try (PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(filename, true)))) {
            Log.i(TAG, "->name: " + name);
			// System.out.println("score: " + score);
			Log.i(TAG, "->relevance: " + relevance);
            Log.i(TAG, "->present: " + present);
            Log.i(TAG, "->parent: " + parent);
            Log.i(TAG, "->sumattri: " + sumattri);
            Log.i(TAG, "->attribegin: " + attribegin);
            Log.i(TAG, "->attriend: " + attriend);
            Log.i(TAG, "->totalprimaryattri: " + totalprimaryattri);
            Log.i(TAG, "->already asked: " + alreadyAsked);
			
		} catch (IOException e) {
			// exception handling left as an exercise for the reader
		}

	}

	public String getname() {
		return name;
	}

	// public float getscore() {
	// return score;
	// }

	public float getrelevance() {
		return relevance;
	}

	public float getpresent() {

		return present;
	}

	public int getalreadyAsked() {
		return alreadyAsked;
	}

	public String getparent() {
		return parent;
	}

	public float getsumattri() {
		return sumattri;
	}

	public int getattribegin() {
		return attribegin;
	}

	public int getattriend() {
		return attriend;
	}

	public int gettotalprimaryattri() {
		return totalprimaryattri;
	}

	// public void changescore(float newtotalscore){
	// score = newtotalscore;
	// }
	public void changepresent(float newpresent) {
		present = newpresent;
	}

	public void changesumattri(float newsumattri) {
		sumattri = newsumattri;
	}

	public void changealreadyAsked(int newalreadyAsked) {
		alreadyAsked = newalreadyAsked;
	}

	public void changerelevance(float newrelevance) {
		relevance = newrelevance;
	}

}
