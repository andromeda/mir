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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.gson.JsonObject;
import com.google.javascript.jscomp.LoggerErrorManager;
import com.google.javascript.jscomp.TrackRequiredModules.PolicyEntry;

public class AnalyzeNpmPackage {
	
	public static void main(String[] args) throws IOException {
//		System.setErr(new PrintStream(new OutputStream() {
//		    public void write(int b) {
//		    }
//		}));		
		if (args.length - System.getProperties().size() * 2 > 1) {
			System.out.println("Please provide a single parameter: the path to the npm package");
			return ;
		}
		boolean pkgLevel = false, maybeReaching = false;	
		String pkgLevelS = System.getProperty("npm.pkg.level");
		if (pkgLevelS != null)
			pkgLevel = true;
		String maybeReachingS = System.getProperty("maybe.reaching");
		if (maybeReachingS != null)
			maybeReaching = true;
		String targetModule = System.getProperty("target.module");
		boolean baseStars = false, propStars = false;	
		String baseStarsS = System.getProperty("base.stars");
		if (baseStarsS != null)
			baseStars = true;
		String propStarsS = System.getProperty("prop.stars");
		if (propStarsS != null)
			propStars = true;
		System.out.println("Analyzing " + args[0] + ", pkgLevel="+ pkgLevel + ", maybereaching=" + maybeReaching + ", target=" + targetModule + ", baseStars=" + baseStars + ", propStars=" + propStars);
		JsonObject policy = new AnalyzeNpmPackage().analyze(new File(args[0]).getAbsolutePath(), pkgLevel, maybeReaching, targetModule, baseStars, propStars);
		System.out.println("The policy is:");
		System.out.println(policy.toString());
//		File dir = new File("/home/cstaicu/work/npm1000-13oct2018");
//		File[] files = dir.listFiles();
//		for (int i = 0; i < files.length; i++) {
//			System.out.println(i + "/" + files.length);
//			try {
//				String policy = new AnalyzeNpmPackage().analyze(files[i].getAbsolutePath());
////				System.out.println(policy);
//				Files.write(Paths.get("./out/res.txt"), 
//						("<policy nmpPackage=\"" + files[i].getName() + "\" count=\"" + (i + 1) + "\">\n" + policy + "</policy>\n\n").getBytes(), 
//						StandardOpenOption.APPEND
//				);
//			} catch(Exception e) {}
//		}
	}

	public JsonObject analyze(
			String path, 
			boolean pkgLevel, 
			boolean mustBeReaching, 
			String targetModule, 
			boolean produceBaseStars,
			boolean producePropStars) throws IOException {		
		JsonObject outputJSON = new JsonObject();
		Map<String, PolicyEntry> globalPolicy = new HashMap<String, PolicyEntry>();											

		Files.walk(Paths.get(path)).filter(Files::isRegularFile).filter(new Predicate<Path>() {

			@Override
			public boolean test(Path t) {
				//ignore files in node_modules
				return t.toString().matches(".*.js$") && t.toString().replaceAll(path, "").indexOf("node_modules") == -1;
			}
		}).forEach(new Consumer<Path>() {

			@Override
			public void accept(Path t) {
				System.out.println(t);
				try {
					byte[] encoded = Files.readAllBytes(t);
					String code = new String(encoded);
					if (code.length() < 300000) {
						FileAnalyzer fa = new FileAnalyzer();
						Map<String, PolicyEntry> policy = fa.analyze(code, mustBeReaching, pkgLevel, produceBaseStars, producePropStars);						
						if (pkgLevel == true) {
							for (String entry: policy.keySet()) {
								if (!globalPolicy.containsKey(entry)) {
									if (targetModule == null || entry.indexOf("require('" + targetModule +  "')") != -1)
										globalPolicy.put(entry, policy.get(entry));
								} else {
									globalPolicy.get(entry).merge(policy.get(entry));
								}
							}
						} else {
							outputJSON.add(t.toFile().getCanonicalPath(), FileAnalyzer.printAsJSON(policy));
						}
					}
				} catch (Exception e) {					
					// TODO Auto-generated catch block					
					e.printStackTrace(System.out);
					e.printStackTrace();
				}			
			}
		});
		if (pkgLevel == true) {
			return FileAnalyzer.printAsJSON(globalPolicy);
		} else {
			return outputJSON;
		}
	}

}
