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
package org.eclipse.flux.sync;

/**
 * @author Martin Lippert
 */
public abstract class AbstractOperation {
	
	private final String id;
	private final String previousOperation;

	public AbstractOperation(final String id, final String previousOperation) {
		this.id = id;
		this.previousOperation = previousOperation;
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * @return the previousOperation
	 */
	public String getPreviousOperation() {
		return previousOperation;
	}
	
}
