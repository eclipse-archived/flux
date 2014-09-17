/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.jdt.services;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.text.edits.TextEdit;

/**
 * POJO for passing completion proposal replacement text info
 * 
 * @author aboyko
 *
 */
class ProposalReplcamentInfo {
	
	public String replacement;
	public TextEdit extraChanges;
	public List<Integer> positions;
	
	public ProposalReplcamentInfo() {
		this.replacement = "";
		this.extraChanges = null;
		this.positions = new ArrayList<Integer>();
	}

}
