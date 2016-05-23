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
 * This is an operation to delete a number of characters at the given position
 * 
 * @author Martin Lippert
 */
public class DeleteOperation extends AbstractOperation {
	
	private final int position;
	private final int length;

	/**
	 * create a new delete operation
	 * @param id The globally unique ID of this deletion, usually consists of the ID of the sync participant and an ID for the operation
	 * @param previousOperation The previous operation to this one
	 * @param position The position at which the characters should be inserted
	 * @param length The number of characters to be deleted
	 */
	public DeleteOperation(final String id, final String previousOperation, final int position, final int length) {
		super(id, previousOperation);
		this.position = position;
		this.length = length;
	}
	
	/**
	 * @return the position
	 */
	public int getPosition() {
		return position;
	}
	
	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

}
