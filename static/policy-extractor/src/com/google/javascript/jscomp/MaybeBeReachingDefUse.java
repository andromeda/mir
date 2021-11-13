/*
 * Copyright 2008 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

import com.google.javascript.jscomp.ControlFlowGraph.AbstractCfgNodeTraversalCallback;
import com.google.javascript.jscomp.ControlFlowGraph.Branch;
import com.google.javascript.jscomp.MustBeReachingDefUse.Definition;
import com.google.javascript.jscomp.graph.GraphNode;
import com.google.javascript.jscomp.graph.LatticeElement;
import com.google.javascript.rhino.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Computes reaching definition for all use of each variables.
 *
 * A definition of {@code A} in {@code A = foo()} is a reaching definition of
 * the use of {@code A} in {@code alert(A)} if all paths from entry node must
 * reaches that definition and it is the last definition before the use.
 *
 */
final class MaybeBeReachingDefUse extends DataFlowAnalysis<Node, MaybeBeReachingDefUse.MaybeDef> implements MyDefUse {

	// The scope of the function that we are analyzing.
	private final AbstractCompiler compiler;
	private final Set<Var> escaped;
	private final Map<String, Var> allVarsInFn;
	private final List<Var> orderedVars;

	MaybeBeReachingDefUse(ControlFlowGraph<Node> cfg, Scope jsScope, AbstractCompiler compiler,
			SyntacticScopeCreator scopeCreator) {
		super(cfg, new MaybeDefJoin());
		this.compiler = compiler;
		this.escaped = new HashSet<>();
		this.allVarsInFn = new HashMap<>();
		this.orderedVars = new ArrayList<>();
		computeEscaped(jsScope.getParent(), escaped, compiler, scopeCreator);
		NodeUtil.getAllVarsDeclaredInFunction(allVarsInFn, orderedVars, compiler, scopeCreator, jsScope.getParent());
	}

	/**
	 * Abstraction of a local variable definition. It represents the node which a
	 * local variable is defined as well as a set of other local variables that this
	 * definition reads from. For example N: a = b + foo.bar(c). The definition node
	 * will be N, the depending set would be {b,c}.
	 */
	static class Definition {
		final Node node;
		final Set<Var> depends = new HashSet<>();
		private boolean unknownDependencies = false;

		Definition(Node node) {
			this.node = node;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Definition)) {
				return false;
			}
			Definition otherDef = (Definition) other;
			// If the var has the same definition node we can assume they have the
			// same depends set.
			return otherDef.node == node;
		}

		@Override
		public String toString() {
			return "Definition@" + node;
		}

		@Override
		public int hashCode() {
			return node.hashCode();
		}
	}

	/**
	 * Must reaching definition lattice representation. It captures a product
	 * lattice for each local (non-escaped) variable. The sub-lattice is a n + 2
	 * element lattice with all the {@link Definition} in the program, TOP and
	 * BOTTOM.
	 *
	 * <p>
	 * Since this is a Must-Define analysis, BOTTOM represents the case where there
	 * might be more than one reaching definition for the variable.
	 *
	 *
	 * (TOP) / | | \ N1 N2 N3 ....Nn \ | | / (BOTTOM)
	 *
	 */
	static final class MaybeDef implements LatticeElement {

		// TODO(user): Use bit vector instead of maps might get better
		// performance. Change it after this is tested to be fully functional.

		// When a Var "A" = "TOP", "A" does not exist in reachingDef's keySet.
		// When a Var "A" = Node N, "A" maps to that node.
		// When a Var "A" = "BOTTOM", "A" maps to null.
		final Map<Var, Set<Definition>> reachingDef;

		public MaybeDef() {
			reachingDef = new HashMap<>();
		}

		public MaybeDef(Collection<Var> vars) {
			this();
			for (Var var : vars) {
				Set<Definition> defs = new HashSet<Definition>();
				defs.add(new Definition(var.getScope().getRootNode()));
				reachingDef.put(var, defs);
			}
		}

		/**
		 * Copy constructor.
		 *
		 * @param other
		 *            The constructed object is a replicated copy of this element.
		 */
		public MaybeDef(MaybeDef other) {
			reachingDef = new HashMap<>(other.reachingDef);
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof MaybeDef))
				return false;
			MaybeDef otherMD = (MaybeDef) other;
			if (otherMD.reachingDef.equals(this.reachingDef))
				return true;
			for (Var keyA: reachingDef.keySet()) {
				Set<Definition> defsA = reachingDef.get(keyA); 
				if (!otherMD.reachingDef.containsKey(keyA))
					return false;
				Set<Definition> defsB = reachingDef.get(keyA);
				if (defsA.size() != defsB.size())
					return false;
				for (Definition defA: defsA) {
					if (!defsB.contains(defA))
						return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			return reachingDef.hashCode();
		}
	}

	private static class MaybeDefJoin extends JoinOp.BinaryJoinOp<MaybeDef> {
		@Override
		public MaybeDef apply(MaybeDef a, MaybeDef b) {
//			System.out.println(a.reachingDef.size() + " " + b.reachingDef.size());
			MaybeDef result = new MaybeDef();
			Map<Var, Set<Definition>> resultMap = result.reachingDef;

			// Take the join of all variables that are not TOP in this.
			for (Map.Entry<Var, Set<Definition>> varEntry : a.reachingDef.entrySet()) {
				Var var = varEntry.getKey();
				Set<Definition> aDef = varEntry.getValue();				
				if (aDef == null) {
					// "a" is BOTTOM implies that the variable has more than one possible
					// definition. We set the join of this to be BOTTOM regardless of what
					// "b" might be.
					resultMap.put(var, null);
					continue;
				}

				// if (b.reachingDef.containsKey(var)) {
				// Definition bDef = b.reachingDef.get(var);
				//
				// if (aDef.equals(bDef)) {
				// resultMap.put(var, aDef);
				// } else {
				// resultMap.put(var, null);
				// }
				// } else {
				if (aDef != null) {
					if (!resultMap.containsKey(var))
						resultMap.put(var, aDef);
					else 
						resultMap.get(var).addAll(aDef);
				}
				// }
			}
		

			// Take the join of all variables that are not TOP in other but it is TOP
			// in this.
			for (Map.Entry<Var, Set<Definition>> entry : b.reachingDef.entrySet()) {
				Var var = entry.getKey();
				// if (!a.reachingDef.containsKey(var)) {
				if (entry.getValue() != null) {
					if (!resultMap.containsKey(var))
						resultMap.put(var, entry.getValue());
					else {
						Set<Definition> currSet = resultMap.get(var);
						if (currSet == null) {
							resultMap.put(var, entry.getValue()); 
						} else {
							resultMap.get(var).addAll(entry.getValue());
						}
					}
				}
				// }
			}
			
			return result;
		}
	}

	@Override
	boolean isForward() {
		return true;
	}

	@Override
	MaybeDef createEntryLattice() {
		return new MaybeDef(allVarsInFn.values());
	}

	@Override
	MaybeDef createInitialEstimateLattice() {
		return new MaybeDef();
	}

	@Override
	MaybeDef flowThrough(Node n, MaybeDef input) {
		// TODO(user): We are doing a straight copy from input to output. There
		// might be some opportunities to cut down overhead.
		MaybeDef output = new MaybeDef(input);
		// TODO(user): This must know about ON_EX edges but it should handle
		// it better than what we did in liveness. Because we are in a forward mode,
		// we can used the branched forward analysis.
		computeMustDef(n, n, output, false);
		return output;
	}

	/**
	 * @param n
	 *            The node in question.
	 * @param cfgNode
	 *            The node to add
	 * @param conditional
	 *            true if the definition is not always executed.
	 */
	private void computeMustDef(Node n, Node cfgNode, MaybeDef output, boolean conditional) {
		switch (n.getToken()) {

		case BLOCK:
		case ROOT:
		case FUNCTION:
			return;

		case WHILE:
		case DO:
		case IF:
			computeMustDef(NodeUtil.getConditionExpression(n), cfgNode, output, conditional);
			return;

		case FOR:
			computeMustDef(NodeUtil.getConditionExpression(n), cfgNode, output, conditional);
			return;

		case FOR_IN:
		case FOR_OF:
			// for(x in y) {...}
			Node lhs = n.getFirstChild();
			Node rhs = lhs.getNext();
			if (NodeUtil.isNameDeclaration(lhs)) {
				lhs = lhs.getLastChild(); // for(var x in y) {...}
			}
			if (lhs.isName()) {
				// TODO(lharker): This doesn't seem right - given for (x in y), the value set to
				// x isn't y
				addToDefIfLocal(lhs.getString(), cfgNode, rhs, output);
			} else if (lhs.isDestructuringLhs()) {
				lhs = lhs.getFirstChild();
			}
			if (lhs.isDestructuringPattern()) {
				computeMustDef(lhs, cfgNode, output, true);
			}
			return;

		case AND:
		case OR:
			computeMustDef(n.getFirstChild(), cfgNode, output, conditional);
			computeMustDef(n.getLastChild(), cfgNode, output, true);
			return;

		case HOOK:
			computeMustDef(n.getFirstChild(), cfgNode, output, conditional);
			computeMustDef(n.getSecondChild(), cfgNode, output, true);
			computeMustDef(n.getLastChild(), cfgNode, output, true);
			return;

		case LET:
		case CONST:
		case VAR:
			for (Node c = n.getFirstChild(); c != null; c = c.getNext()) {
				if (c.hasChildren()) {
					if (c.isName()) {
						computeMustDef(c.getFirstChild(), cfgNode, output, conditional);
						addToDefIfLocal(c.getString(), conditional ? null : cfgNode, c.getFirstChild(), output);
					} else {
//						checkState(c.isDestructuringLhs(), c);
						computeMustDef(c.getSecondChild(), cfgNode, output, conditional);
						computeMustDef(c.getFirstChild(), cfgNode, output, conditional);
					}
				}
			}
			return;

		case DEFAULT_VALUE:
			if (n.getFirstChild().isDestructuringPattern()) {
				computeMustDef(n.getSecondChild(), cfgNode, output, true);
				computeMustDef(n.getFirstChild(), cfgNode, output, conditional);
			} else if (n.getFirstChild().isName()) {
				computeMustDef(n.getSecondChild(), cfgNode, output, true);
				addToDefIfLocal(n.getFirstChild().getString(), conditional ? null : cfgNode, null, output);
			} else {
				computeMustDef(n.getFirstChild(), cfgNode, output, conditional);
				computeMustDef(n.getSecondChild(), cfgNode, output, true);
			}
			break;

		case NAME:
			if (NodeUtil.isLhsByDestructuring(n)) {
				addToDefIfLocal(n.getString(), conditional ? null : cfgNode, null, output);
			} else if ("arguments".equals(n.getString())) {
				escapeParameters(output);
			}
			return;

		default:
			if (NodeUtil.isAssignmentOp(n)) {
				if (n.getFirstChild().isName()) {
					Node name = n.getFirstChild();
					computeMustDef(name.getNext(), cfgNode, output, conditional);
					addToDefIfLocal(name.getString(), conditional ? null : cfgNode, n.getLastChild(), output);
					return;
				} else if (NodeUtil.isNormalGet(n.getFirstChild())) {
					// Treat all assignments to arguments as redefining the
					// parameters itself.
					Node obj = n.getFirstFirstChild();
					if (obj.isName() && "arguments".equals(obj.getString())) {
						// TODO(user): More accuracy can be introduced
						// i.e. We know exactly what arguments[x] is if x is a constant
						// number.
						escapeParameters(output);
					}
				} else if (n.getFirstChild().isDestructuringPattern()) {
					computeMustDef(n.getSecondChild(), cfgNode, output, conditional);
					computeMustDef(n.getFirstChild(), cfgNode, output, conditional);
					return;
				}
			}

			// DEC and INC actually defines the variable.
			if (n.isDec() || n.isInc()) {
				Node target = n.getFirstChild();
				if (target.isName()) {
					addToDefIfLocal(target.getString(), conditional ? null : cfgNode, null, output);
					return;
				}
			}

			for (Node c = n.getFirstChild(); c != null; c = c.getNext()) {
				computeMustDef(c, cfgNode, output, conditional);
			}
		}
	}

	/**
	 * Set the variable lattice for the given name to the node value in the def
	 * lattice. Do nothing if the variable name is one of the escaped variable.
	 *
	 * @param node
	 *            The CFG node where the definition should be record to.
	 *            {@code null} if this is a conditional define.
	 */
	private void addToDefIfLocal(String name, Node node, Node rValue, MaybeDef def) {
		Var var = allVarsInFn.get(name);

		// var might be null if the variable is defined in the externs
		if (var == null) {
			return;
		}

		for (Var other : def.reachingDef.keySet()) {
			Set<Definition> otherDefs = def.reachingDef.get(other);
			if (otherDefs != null)
				for (Definition otherDef : otherDefs) {
					if (otherDef == null) {
						continue;
					}
					if (otherDef.depends.contains(var)) {
						def.reachingDef.put(other, null);
					}
				}
		}

		// if (!escaped.contains(var)) {
		if (node == null) {
			def.reachingDef.put(var, null);
		} else {
			if (!def.reachingDef.containsKey(var))
				def.reachingDef.put(var, new HashSet<Definition>());
			Set<Definition> reachingDefs;
			if (!def.reachingDef.containsKey(var) || def.reachingDef.get(var) == null) {
				reachingDefs = new HashSet<Definition>();
				def.reachingDef.put(var, reachingDefs);
			} else {
				reachingDefs = def.reachingDef.get(var);
			}
			Definition definition = new Definition(node);
			if (rValue != null) {
				computeDependence(definition, rValue);
			}
			reachingDefs.add(definition);
		}
		// }
	}

	private void escapeParameters(MaybeDef output) {
		for (Var v : allVarsInFn.values()) {
			if (isParameter(v)) {
				// Assume we no longer know where the parameter comes from
				// anymore.
				output.reachingDef.put(v, null);
			}
		}

		// Also, assume we no longer know anything that depends on a parameter.
		for (Entry<Var, Set<Definition>> pair : output.reachingDef.entrySet()) {
			Set<Definition> vals = pair.getValue();
			if (vals == null) {
				continue;
			}
			for (Definition value : vals)
				for (Var dep : value.depends) {
					if (isParameter(dep)) {
						output.reachingDef.put(pair.getKey(), null);
					}
				}
		}
	}

	private static boolean isParameter(Var v) {
		return v.isParam();
	}

	/**
	 * Computes all the local variables that rValue reads from and store that in the
	 * def's depends set.
	 */
	private void computeDependence(final Definition def, Node rValue) {
		NodeTraversal.traverse(compiler, rValue, new AbstractCfgNodeTraversalCallback() {
			@Override
			public void visit(NodeTraversal t, Node n, Node parent) {
				if (n.isName()) {
					Var dep = allVarsInFn.get(n.getString());
					if (dep == null) {
						def.unknownDependencies = true;
					} else {
						def.depends.add(dep);
					}
				}
			}
		});
	}

	/**
	 * Gets the must reaching definition of a given node.
	 *
	 * @param name
	 *            name of the variable. It can only be names of local variable that
	 *            are not function parameters, escaped variables or variables
	 *            declared in catch.
	 * @param useNode
	 *            the location of the use where the definition reaches.
	 */
	Set<Definition> getDef(String name, Node useNode) {
//		checkArgument(getCfg().hasNode(useNode));
		GraphNode<Node, Branch> n = getCfg().getNode(useNode);
		FlowState<MaybeDef> state = n.getAnnotation();
		return state.getIn().reachingDef.get(allVarsInFn.get(name));
	}

	public Set<Node> getDefNode(String name, Node useNode) {
		Set<Definition> defs = getDef(name, useNode);
		Set<Node> res = new HashSet<Node>();
		if (defs == null)
			return null;
		for (Definition def : defs)
			res.add(def.node);
		return res;
	}

	boolean dependsOnOuterScopeVars(Definition def) {
		if (def.unknownDependencies) {
			return true;
		}

		for (Var s : def.depends) {
			// Don't inline try catch
			if (s.getScope().isCatchScope()) {
				return true;
			}
		}
		return false;
	}
}