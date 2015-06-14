package com.example.brian.cdss_adr03;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;

class Condition {
    private String TAG = "Condition Node";

	private String name;
	private float score = 0; //for ranking the different conditions
	private int risk; //weight of the condition so that possible emergency cases are checked first. not really useful right now.
	private float sumsym = 0; //sum of symptoms scores
	private int symbegin; // to find its children
	private int symend;// to find its children
	private int totalprimarysym; // sum of the total primary symptoms.

	public Condition(String nameTemp, int riskTemp, int symbeginTemp,
			int symendTemp, int totalprimarysymTemp) {
		name = nameTemp;
		risk = riskTemp;
		symbegin = symbeginTemp;
		symend = symendTemp;
		totalprimarysym = totalprimarysymTemp;
	}

	public void printCond(String filename) {

		try (PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(filename, true)))) {
            Log.i(TAG, "name: " + name);
            Log.i(TAG, "score: " + score);
            Log.i(TAG, "risk: " + risk);
            Log.i(TAG, "sumsym: " + sumsym);
            Log.i(TAG, "symbegin: " + symbegin);
            Log.i(TAG, "symend: " + symend);
            Log.i(TAG, "totalprimarysym: " + totalprimarysym);

				} catch (IOException e) {
					// exception handling left as an exercise for the reader
				}
		}

	public String getname() {
		return name;
	}

	public float getscore() {
		return score;
	}

	public int getrisk() {
		return risk;
	}

	public float getsumsym() {
		return sumsym;
	}

	public int getsymbegin() {
		return symbegin;
	}

	public int getsymend() {
		return symend;
	}

	public int gettotalprimarysym() {
		return totalprimarysym;
	}

	public void changescore(float newtotalscore) {
		score = newtotalscore;
	}

	public void changesumsym(float newsumsym) {
		sumsym = newsumsym;
	}

}

class ConditionComparator implements Comparator<com.example.brian.cdss_adr03.Condition> {
	public int compare(com.example.brian.cdss_adr03.Condition cond1, com.example.brian.cdss_adr03.Condition cond2) {
		int result = Float.compare(cond2.getscore(), cond1.getscore());
		return result;
	}
}