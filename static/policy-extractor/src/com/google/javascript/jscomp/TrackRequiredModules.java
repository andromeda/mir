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
 * @author Cristian-Alexandru Staic
 */

package com.google.javascript.jscomp;

import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.jscomp.NodeTraversal.ScopedCallback;
import com.google.javascript.rhino.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Track all Read Write
 *
 */
public class TrackRequiredModules extends AbstractPostOrderCallback implements CompilerPass, ScopedCallback {

	private List builtIns = Arrays.asList(new String[] { "__dirname", "__filename", "exports", "global", "module",
			"require", "Array", "ArrayBuffer", "Atomics", "BigInt", "BigInt64Array", "BigUint64Array", "Boolean",
			"constructor", "DataView", "Date", "decodeURI", "decodeURIComponent", "encodeURI", "encodeURIComponent",
			"Error", "escape", "eval", "EvalError", "Float32Array", "Float64Array", "Function", "hasOwnProperty",
			"Infinity", "Int16Array", "Int32Array", "Int8Array", "isFinite", "isNaN", "isPrototypeOf", "JSON", "Map",
			"Math", "NaN", "Number", "Object", "parseFloat", "parseInt", "Promise", "propertyIsEnumerable", "Proxy",
			"RangeError", "ReferenceError", "Reflect", "RegExp", "Set", "SharedArrayBuffer", "String", "Symbol",
			"SyntaxError", "toLocaleString", "toString", "TypeError", "Uint16Array", "Uint32Array", "Uint8Array",
			"Uint8ClampedArray", "undefined", "unescape", "URIError", "valueOf", "WeakMap", "WeakSet", "Buffer",
			"clearImmediate", "clearInterval", "clearTimeout", "console", "Intl", "process", "setImmediate",
			"setInterval", "setTimeout", "URL", "URLSearchParams" });

	private static class AccessPathsMapping {

		final Map<String, List<Node>> accessPaths = new LinkedHashMap<>();
		final Map<Node, Set<String>> names = new LinkedHashMap<>();

		private void put(String accessPath, Node n) {
			List<Node> list;
			if (accessPaths.containsKey(accessPath))
				list = accessPaths.get(accessPath);
			else {
				list = new ArrayList<Node>();
				accessPaths.put(accessPath, list);
			}
			if (list.indexOf(n) == -1)
				list.add(n);
			if (!names.containsKey(n))
				names.put(n, new HashSet<String>());
			Set<String> namesList = names.get(n);
			namesList.add(accessPath);
		}

		private Set<String> getAccessPath(Node n) {
			return names.get(n);
		}

		@Override
		public String toString() {
			return accessPaths.keySet().toString();
		}

	}

	private final transient AbstractCompiler compiler;
	private final AccessPathsMapping apMapping = new AccessPathsMapping();
	private MyDefUse defUse;
	private ControlFlowGraph<Node> cfg;
	private Stack<ControlFlowGraph> cfgsStack = new Stack<ControlFlowGraph>();
	private Stack<MyDefUse> defusesStack = new Stack<MyDefUse>();
	private Scope scope;
	private Map<String, PolicyEntry> policy = new HashMap<String, PolicyEntry>();
	private boolean maybeReaching = false;
	private boolean pkgLevel;
	private boolean produceBaseStars;
	private boolean producePropStars;

	public class PolicyEntry {

		private boolean write;
		private boolean read;
		private boolean execute;
		private boolean importFlag;

		@Override
		public String toString() {
			String res = "";
			if (importFlag)
				res += "i";
			if (read)
				res += "r";// (res.length() > 0) ? ",r" : "r";
			if (write)
				res += "w";
			if (execute)
				res += "x";// (res.length() > 0) ? ",x" : "x";
			return res;
		}

		public void setWrite(boolean write) {
			this.write = write;
		}

		public void setRead(boolean read) {
			this.read = read;
		}

		public void setExecute(boolean execute) {
			this.execute = execute;
		}
		
		public void setImport(boolean impFlag) {
			this.importFlag = impFlag;
		}

		public void merge(PolicyEntry policyEntry) {			
			read |= policyEntry.read;
			write |= policyEntry.write;
			execute |= policyEntry.execute;
			importFlag |= policyEntry.importFlag;
		}

	}

	public TrackRequiredModules(
			AbstractCompiler compiler, 
			boolean maybeReaching, 
			boolean pkgLevel, 
			boolean produceBaseStars, 
			boolean producePropStars) {
		this.compiler = compiler;
		this.pkgLevel = pkgLevel;
		this.maybeReaching = maybeReaching;
		this.producePropStars = producePropStars;
		this.produceBaseStars = produceBaseStars;
	}

	@Override
	public void process(Node externs, Node root) {
		String mappings, newMappings = "";
		do {
			mappings = newMappings;
			NodeTraversal.traverse(compiler, root, this);
			newMappings = apMapping.toString();
			// System.out.println(newMappings);
		} while (!mappings.equals(newMappings));

	}

	public Map<String, PolicyEntry> getPolicy() {
		return policy;
	}

	@Override
	public void enterScope(NodeTraversal t) {
		// System.out.println("Enter scope" + t);
		if (!t.getScope().isFunctionBlockScope()) {
			return; // Only want to do the following if its a function block scope.
		}

		// Compute the forward reaching definition.
		ControlFlowAnalysis cfa = new ControlFlowAnalysis(compiler, false, true);
		Node functionScopeRoot = t.getScopeRoot().getParent();
		SyntacticScopeCreator scopeCreator = (SyntacticScopeCreator) t.getScopeCreator();
		// Process the body of the function.
		cfa.process(null, functionScopeRoot);
		cfgsStack.push(cfg);
		defusesStack.push(defUse);
		cfg = cfa.getCfg();
		if (maybeReaching)
			defUse = new MaybeBeReachingDefUse(cfg, t.getScope(), compiler, scopeCreator);
		else
			defUse = new MustBeReachingDefUse(cfg, t.getScope(), compiler, scopeCreator);
		((DataFlowAnalysis) defUse).analyze();
		// System.out.println(cfg.getEdges().get(2).getNodeA().getValue().getFirstChild().getFirstChild());
	}

	@Override
	public void visit(NodeTraversal t, Node n, Node parent) {
		Set<String> aps;		
		switch (n.getToken()) {
		case FUNCTION:
			break;
		case NAME:
			if (parent.isCall() && parent.getFirstChild() == n)
				break;
			aps = getAccessPaths(n);
			if (aps != null)
				for (String ap : aps) {
					// System.out.println(ap + " w");
					if (!policy.containsKey(ap))
						policy.put(ap, new PolicyEntry());
					policy.get(ap).setRead(true);
				}
			//TODO handle globals here
//			if (!canResolve(n)) {
//				System.out.println(n.getString());
//			}
			break;
		case VAR:
		case LET:
		case CONST:				
			if (n.getFirstChild() != null) {
				aps = getAccessPaths(n.getFirstChild().getFirstChild());				
				if (aps != null)
					for (String ap : aps)
						if (ap != null && n.getFirstChild().isName()) {
							apMapping.put(ap, n);
						}
			}
			break;
		case OBJECTLIT:
//			if (!policy.containsKey("Object"))
//				policy.put("Object", new PolicyEntry());
//			policy.get("Object").setRead(true);
			break;
		case ASSIGN:			
			// write entries in the policy
			// check if you are writing to a 3rd party module
			aps = getAccessPaths(n.getFirstChild());
			if (aps != null) {
				for (String ap : aps) {
					// System.out.println(ap + " w");
					if (!policy.containsKey(ap))
						policy.put(ap, new PolicyEntry());
					policy.get(ap).setWrite(true);
				}
			}			
			aps = getAccessPaths(n.getSecondChild());
			if (aps != null)
				for (String ap : aps)
					if (ap != null) {						
						if (!policy.containsKey(ap))
							policy.put(ap, new PolicyEntry());
						policy.get(ap).setRead(true);
						if (n.getFirstChild().isName()) {
							apMapping.put(ap, n);						
							// System.out.println(n.getFirstChild().getString() + " <--- " + ap);
						}
					}					
			break;
		case GETELEM:				
			aps = getAccessPaths(n.getFirstChild());
			Node prop = n.getSecondChild(); // TODO attempt to resolve this
			if (aps != null) {
				for (String ap : aps) {									
					if (!policy.containsKey(ap))
						policy.put(ap, new PolicyEntry());
					policy.get(ap).setRead(true);
					if (producePropStars) {
						String starBasePerm = ap + ".*";
						if (!policy.containsKey(starBasePerm))
							policy.put(starBasePerm, new PolicyEntry());
						policy.get(starBasePerm).setRead(true);
					}
				}
			}			
			break;
		case GETPROP:			
			// read entries in the policy
			// check if you are accessing a 3rd party module's method
			aps = getAccessPaths(n);
			if (aps != null)
				for (String ap : aps)
					if (ap != null && !((n.getParent().isAssign() || n.getParent().isVar())
							&& n.isFirstChildOf(n.getParent()))) {
						// System.out.println(ap + " r");						
//						if (parent.getFirstChild() != n || (parent.getToken() != Token.CALL && parent.getToken() != Token.NEW)) {							
							if (!policy.containsKey(ap))
								policy.put(ap, new PolicyEntry());
							policy.get(ap).setRead(true);
//						}
					}
			Set<String> baseAps = getAccessPaths(n.getFirstChild());
			if (baseAps != null) {
				for (String ap : baseAps) {									
					if (!policy.containsKey(ap))
						policy.put(ap, new PolicyEntry());
					policy.get(ap).setRead(true);
				}
			}
			if (produceBaseStars) {				
				if ((baseAps == null || baseAps.size() == 0) && !canResolve(n.getFirstChild())) { // ||					
					String propName = n.getSecondChild().getString();
					String starPerm = "*." + propName;
					if (!policy.containsKey(starPerm))
						policy.put(starPerm, new PolicyEntry());
					policy.get(starPerm).setRead(true);				
				}
			}
			break;
		case CLASS:
			break;
		case CALL:
		case NEW:
			// handle imports
			aps = getAccessPaths(n);			
			// execute entries in the policy
			// check if you are executing a method from a required module
			aps = getAccessPaths(n.getFirstChild());			
			if (aps != null)
				for (String ap : aps)
					if (ap != null) {
						// System.out.println(ap + " x");
						if (!policy.containsKey(ap))
							policy.put(ap, new PolicyEntry());
						policy.get(ap).setExecute(true);
						policy.get(ap).setRead(true);
					}
			break;				
		case IMPORT:
			break;
		default:
			break;
		}
	}

	@Override
	public void exitScope(NodeTraversal arg0) {
		if (!arg0.getScope().isFunctionBlockScope()) {
			return; // Only want to do the following if its a function block scope.
		}
		if (defusesStack.size() > 0) {
			defUse = defusesStack.pop();
			cfg = cfgsStack.pop();
		}
	}
	
	public boolean canResolve(Node n) {
		if (n.isArrayLit() || n.isObjectLit() || n.isNumber() || n.isString() || n.isQuotedString())
			return true;
		if (n.isAssign())
			return canResolve(n.getSecondChild());
		if (n.isVar() || n.isLet() || n.isConst())
			return canResolve(n.getFirstChild().getFirstChild());
		if (n.isAdd() || n.isOr())
			return canResolve(n.getFirstChild()) && canResolve(n.getFirstChild());
		if (n.isGetProp())
			return canResolve(n.getFirstChild());
		if (n.isName()) {		
			Set<Node> defs = resolveInScope(n);			
			if (defs != null) {
				for (Node def : defs)				
					if (!canResolve(def)) {						
						return false;
					}
				return true;
			}
			return false;
		}
		return false;
	}

	private Set<Node> resolveInScope(Node n) {
		try {
//			 System.out.println("====" + n.getString());
			Node cfgNode = n;
			if (cfg != null) {
				while (cfgNode != null && !cfg.hasNode(cfgNode))
					cfgNode = cfgNode.getParent();
				Set<Node> defNodes = defUse.getDefNode(n.getString(), cfgNode);
				if (defNodes == null) {						
					ControlFlowGraph[] parentsCFGs = cfgsStack.toArray(new ControlFlowGraph[0]);
					MyDefUse[] parentsDefs = defusesStack.toArray(new MyDefUse[0]);
					for (int i = parentsCFGs.length - 1; i > 0; i--) {
						// System.out.println(i + " " + n);
						try {
							cfgNode = n;
							while (cfgNode != null && !parentsCFGs[i].hasNode(cfgNode))
								cfgNode = cfgNode.getParent();
							// System.out.println(cfgNode.getFirstChild().getFirstChild().getSecondChild());
							defNodes = parentsDefs[i].getDefNode(n.getString(), null);
							if (defNodes != null) {
								// System.out.println("===");
								// System.out.println(defNode);
								// System.out.println(apMapping.getAccessPath(defNode));
								break;
							}
						} catch (Exception e) {
							//e.printStackTrace();
						}
					}
				}
				return defNodes;
			}
			} catch (Exception e) {
				e.printStackTrace();
			}				
		return null;
	}

	public Set<String> getAccessPaths(Node n) {
		Set<String> res = new HashSet<String>();
		if (n == null)
			return null;		
		if (n.isNew() && n.getFirstChild().isName()) {
			if (builtIns.contains(n.getFirstChild().getString())) {
				res.add(n.getFirstChild().getString());
				return res;
			}
		}		
		Set<String> baseAPs;
		if (n.isGetElem()) {
			if (producePropStars) {
				baseAPs = getAccessPaths(n.getFirstChild());
				if (baseAPs == null) {
					return null;
				} else { 
					for (String baseAP : baseAPs)
						res.add(baseAP + ".*");
				}
			}
			return res;
		}		
		if (n.isGetProp()) {			
			baseAPs = getAccessPaths(n.getFirstChild());			
			if ((baseAPs == null || baseAPs.size() == 0)) {
				if (produceBaseStars && !canResolve(n.getFirstChild())) {					
					res.add("*." + n.getSecondChild().getString());
					return res;
				} else 
					return null;
			} else {
				String propName = null;
				if (n.getSecondChild().isString())
					propName = n.getSecondChild().getString();
				else if (producePropStars)
					propName = "*";
				if (propName != null) {
					for (String baseAP : baseAPs)
						res.add(baseAP + "." + n.getSecondChild().getString());
					return res;
				} else {
					return null;
				}
			}
		}
		if (n.hasChildren() 
				&& n.getFirstChild().isName() && n.getFirstChild().getString() == "require" 
				&& n.getSecondChild() != null && n.getSecondChild().isString()) {
			String moduleName = n.getSecondChild().getString();
//			 System.out.println(moduleName + " " + (moduleName.indexOf(".")));
			if (pkgLevel == false || moduleName.indexOf(".") == -1) { // ignore relative paths
				moduleName = "require('" + moduleName + "')";
				apMapping.put(moduleName, n);
				if (!policy.containsKey(moduleName))
					policy.put(moduleName, new PolicyEntry());				
				policy.get(moduleName).setImport(true);
				if (!policy.containsKey("require"))
					policy.put("require", new PolicyEntry());				
				policy.get("require").setRead(true);
				res.add(moduleName);
				return res;
			}
		}
		if (n.isName()) {			
					
			Set<Node> defNodes = resolveInScope(n);
			if (defNodes != null) {						
				for (Node defNode : defNodes) {
					Set<String> aps = apMapping.getAccessPath(defNode);
					if (aps != null)
						for (String ap : aps)
							res.add(ap);
				}
				return res;
			}
				
//			System.out.println("=====" + n.getString());
			if (builtIns.contains(n.getString())) {
				String builtIn = n.getString();
//				apMapping.put(builtIn, n);
//				if (!policy.containsKey(builtIn))
//					policy.put(builtIn, new PolicyEntry());
//				policy.get(builtIn).setRead(true);
				res.add(builtIn);
				return res;
			}
		}
		return null;

	}

}