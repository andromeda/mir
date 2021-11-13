package de.tudarmstadt.sola.policy.extractor;


import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.google.javascript.jscomp.TrackRequiredModules.PolicyEntry;

class SanityCheckMust {

	@Test
	void test() {
		FileAnalyzer analyzer = new FileAnalyzer();
		Map<String, PolicyEntry> policy = analyzer.analyze("var afs = require('fs'); afs.ab = 23; var sync = afs.readFileSync; sync();  require('child_process').execSync('ls'); require('./myFile'); function fct(){ var a = require('vm'); a.xy = 25; function xxx() {var fs = require('xfs'); afs.readFile(); fs.ab = 23; }; a.runInThisContext(); }; var bc = afs.exists;", false, false, false, false);		
		Assert.assertEquals(FileAnalyzer.prettyPrint(policy), "	require: rx\n" + 
				"	require('child_process'): r\n" + 
				"	require('child_process').execSync: rx\n" + 
				"	require('fs'): r\n" + 
				"	require('fs').ab: w\n" + 
				"	require('fs').exists: r\n" + 
				"	require('fs').readFile: rx\n" + 
				"	require('fs').readFileSync: rx\n" + 
				"	require('vm'): r\n" + 
				"	require('vm').runInThisContext: rx\n" + 
				"	require('vm').xy: w\n" + 
				"	require('xfs'): r\n" + 
				"	require('xfs').ab: w\n");
	}

}