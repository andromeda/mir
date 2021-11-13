/**
 * Copyright 2018 Software Lab, TU Darmstadt, Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 * @author Cristian-Alexandru Staicu
 */
package de.tudarmstadt.sola.policy.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.Es6ModuleTranspilation;
import com.google.javascript.jscomp.ComposeWarningsGuard;
import com.google.javascript.jscomp.DependencyOptions;
import com.google.javascript.jscomp.ErrorFormat;
import com.google.javascript.jscomp.ErrorHandler;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.rhino.Node;
import com.google.javascript.jscomp.TrackRequiredModules;
import com.google.javascript.jscomp.WarningsGuard;
import com.google.javascript.jscomp.TrackRequiredModules.PolicyEntry;
import com.google.javascript.jscomp.deps.ModuleLoader.ResolutionMode;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.Compiler;

public class FileAnalyzer {

	public Map<String, PolicyEntry> analyze(String code, boolean maybeBeReaching, boolean pkgLevel,
			boolean produceBaseStars, boolean producePropStars) {
		Compiler compiler = new Compiler();
		CompilerOptions options = new CompilerOptions();
		options.setEmitUseStrict(false);

		// Ignore Closure's module resolution
		options.setWarningsGuard(new ComposeWarningsGuard(new WarningsGuard() {
			
			@Override
			public CheckLevel level(JSError error) {
				String errorString = error.toString();	
				if (errorString.contains("name module is not defined")
						|| errorString.contains("Invalid module path"))
					return CheckLevel.OFF;
				else 
					return CheckLevel.WARNING;
			}
		}));
		options.setChecksOnly(true);
		options.setModuleResolutionMode(ResolutionMode.NODE);
		compiler.compile(SourceFile.fromCode("fs", "module.exports = function() {}"), SourceFile.fromCode("source-code", wrap(code)), options);
//		return new HashMap<String, PolicyEntry>();
		Node root = root(compiler);
		Node externsRoot = externs(root);
		Node mainRoot = main(root);
		TrackRequiredModules trackRequired = new TrackRequiredModules(compiler, maybeBeReaching, pkgLevel,
				produceBaseStars, producePropStars);
		trackRequired.process(externsRoot, mainRoot);
		return trackRequired.getPolicy();
		
	}

	public static String prettyPrint(Map<String, PolicyEntry> policy) {
		String res = "";
		List<String> keys = new ArrayList<String>(policy.keySet());
		Collections.sort(keys);
		for (String entry : keys)
			res += "\t" + (entry + ": " + policy.get(entry)) + "\n";
		return res;
	}

	public static JsonObject printAsJSON(Map<String, PolicyEntry> policy) {
		JsonObject res = new JsonObject();
		List<String> keys = new ArrayList<String>(policy.keySet());
		Collections.sort(keys);
		for (String entry : keys)
			res.addProperty(entry, policy.get(entry).toString());
		return res;
	}

	private Node root(Compiler compiler) {
		return compiler.getRoot();
	}

	private Node externs(Node root) {
		return root.getFirstChild();
	}

	private Node main(Node root) {
		return root.getSecondChild();
	}

	private String wrap(String code) {
		if (code.indexOf("import ") == -1)
			return "(function() {" + code + "})();";	
		else 
			return code;
	}

}
