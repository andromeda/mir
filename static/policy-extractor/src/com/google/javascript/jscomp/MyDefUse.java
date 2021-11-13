package com.google.javascript.jscomp;

import java.util.Set;

import com.google.javascript.rhino.Node;

public interface MyDefUse {
	
	Set<Node> getDefNode(String name, Node useNode);	

}
