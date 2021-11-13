package de.tudarmstadt.sola.policy.extractor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;

class RunIntegrationTests {
	
	public static final String PATH_TO_TESTS = "../tests";
	public static int noCorrect = 0;
	public static int noTotal = 0;
	public static int noMissed = 0;	
	public static int noAdditional = 0;		

	public static void main(String[] args) throws IOException {
		File testsDir = new File(PATH_TO_TESTS);
		File[] tests = testsDir.listFiles();
		HashMap<String, Set<Diff>> res = new HashMap<String, Set<Diff>>();
		for (File test : tests) {
			if (test.isDirectory()) {
				AnalyzeNpmPackage analysis = new AnalyzeNpmPackage();
				JsonObject inferredStatically = 
						analysis.analyze(test.getCanonicalPath(), false, false, null, true, true);			
				File correct = new File(test, "correct.pwd.json");
				try {
					JsonReader readerCorrect = new JsonReader(new FileReader(correct));
					Gson gson = new Gson();
					HashMap<String, LinkedTreeMap<String, Object>> jsonCorrect = gson.fromJson(readerCorrect, HashMap.class);					
					Set<Diff> compare = compare(inferredStatically, jsonCorrect);
					res.put(test.getName(), compare);	
				} catch(Exception e) {
					System.out.println("Error in " + correct);
					e.printStackTrace();
				}			
			}
		}	
		for (File testEntry : tests) {
			if (testEntry.isDirectory()) {
				System.out.println("For test " + testEntry.getName() + ":");
				Set<Diff> entries = res.get(testEntry.getName());
				if (entries != null) {
					for (Diff diff : entries) {
						System.out.println(diff);
					}				
				}
			}
		}
		System.out.println("Number of correct entries: " + noCorrect + " out of " + noTotal);
		System.out.println("Missing entries: " + noMissed);
		System.out.println("Additional unexpected entries: " + noAdditional);		
	}
	
	private static class Diff {
		public final String targetFile;
		public final String targetMember;
		public final String saPermission;
		public final String correctPermission;
		
		public Diff(String targetFile, 
				String targetMember, 
				String staticAnalysisPerm,
				String correctPerm) {
			this.targetFile = targetFile;
			this.targetMember = targetMember;
			this.saPermission = staticAnalysisPerm;
			this.correctPermission = correctPerm;
		}
		
		@Override
		public String toString() {
			return "FILE[" + targetFile + "] ENTRY[" + targetMember + "] STATIC_ANALYSIS[" + saPermission + "] CORRECT[" + correctPermission + "]";
		}
	}
	
	public static Set<Diff> compare(
			JsonObject staticPolicy, 
			HashMap<String, LinkedTreeMap<String, Object>> correctPolicy) {
		Set<Diff> res = new HashSet<Diff>(); 
		for (Entry<String, JsonElement> fileEntry : staticPolicy.entrySet()) {
			String policyEntryFolder = new File(fileEntry.getKey()).getName();
			LinkedTreeMap<String, Object> correctPermissions;
			policyEntryFolder = "PWD_REPLACE/" + policyEntryFolder;
			correctPermissions = correctPolicy.get(policyEntryFolder);
			if (correctPermissions == null && policyEntryFolder.equals("main.js"))
				correctPermissions = correctPolicy.get("main");
					
			Set<Entry<String, JsonElement>> staticPerms = ((JsonObject) fileEntry.getValue()).entrySet();
			Set<Entry<String, Object>> correctPerms;
			if (correctPermissions != null) {
				correctPerms = correctPermissions.entrySet();				
			} else {				
				correctPerms = new HashSet<>();
			}
			for (Entry<String, JsonElement> sp : staticPerms) {
				String permsSt = sp.getValue().toString().replaceAll("\"", "");
				boolean found = false;
				noTotal++;
				for (Entry<String, Object> cp : correctPerms) {
					if (sp.getKey().equals(cp.getKey())) {
						found = true;					
						String permsCorr = cp.getValue().toString().replaceAll("\"", "");				
						if (!permsSt.equals(permsCorr)) {
							res.add(new Diff(policyEntryFolder, sp.getKey(), permsSt, permsCorr));						
						} else {
							noCorrect++;						
						}						
					}
				}				
				if (!found) {					
					res.add(new Diff(policyEntryFolder, sp.getKey(), permsSt, ""));
					noAdditional++;
				}
			}
			
			for (Entry<String, Object> cp : correctPerms) {
				String permsCorr = cp.getValue().toString().replaceAll("\"", "");
				boolean found = false;
				for (Entry<String, JsonElement> sp : staticPerms) {
					if (sp.getKey().equals(cp.getKey())) {
						found = true;						 					
					}
				}
				if (!found) {
					res.add(new Diff(policyEntryFolder, cp.getKey(), "", permsCorr));
					noMissed++;
				}
			}			
		} 		
		
		return res;
	}

}