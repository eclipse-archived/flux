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
 * This is an operation to insert a number of characters at the given position
 * 
 * @author Martin Lippert
 */
public class InsertOperation extends AbstractOperation {
	
	private final String characters;
	private final int position;

	/**
	 * create a new insert operation
	 * @param id The globally unique ID of this insertion, usually consists of the ID of the sync participant and an ID for the operation
	 * @param previousOperation The previous operation to this one
	 * @param characters The characters to be inserted into the document
	 * @param position The position at which the characters should be inserted
	 */
	public InsertOperation(final String id, final String previousOperation, final String characters, final int position) {
		super(id, previousOperation);
		this.characters = characters;
		this.position = position;
	}
	
	/**
	 * @return the characters
	 */
	public String getCharacters() {
		return characters;
	}
	
	/**
	 * @return the position
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @param historicOperation
	 * @return
	 */
	public InsertOperation transformForward(AbstractOperation historicOperation) {
		InsertOperation transformed = null;
		if (historicOperation instanceof InsertOperation) {
			InsertOperation insertOp = (InsertOperation) historicOperation;
			if (this.getPosition() < insertOp.getPosition()) {
				return this;
			}
			else if (this.getPosition() > insertOp.getPosition()) {
				return new InsertOperation(this.getId(), insertOp.getId(), this.getCharacters(), this.getPosition() + insertOp.getCharacters().length());
			}
		}
		else if (historicOperation instanceof DeleteOperation) {
			
		}
		return transformed;
	}

}
