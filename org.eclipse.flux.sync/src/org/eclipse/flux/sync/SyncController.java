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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Lippert
 */
public class SyncController {

	private final String id;
	private final List<AbstractOperation> history;

	private int counter;
	private SyncConnector connector;
	
	/**
	 * create a new sync controller participant with the given globally unique id
	 * @param id A globally unique id
	 */
	public SyncController(final String id, final SyncConnector connector) {
		this.id = id;
		this.connector = connector;

		this.counter = 0;
		this.history = new ArrayList<AbstractOperation>();
	}

	/**
	 * @return The ID of this sync participant
	 */
	public String getID() {
		return this.id;
	}

	/**
	 * Apply the insert operation to this sync controller. This inserts the operation into the local
	 * history and transforms it to fit into the local history, if necessary.
	 * 
	 * @param operation The insert operation to apply locally
	 */
	public void applyOperation(InsertOperation operation) {
		if (isTransformationRequired(operation)) {
			// here we need to forward-transform the new operation to take missed operations into account
			InsertOperation transformedOperation = forwardTransform(operation);
			this.connector.insert(transformedOperation.getCharacters(), transformedOperation.getPosition());
			this.history.add(transformedOperation);
		}
		else {
			// this is the simple case where the new operation has the same previous operation ID as the last locally executed operation
			this.connector.insert(operation.getCharacters(), operation.getPosition());
			this.history.add(operation);
		}
	}

	/**
	 * @param operation
	 */
	public void applyOperation(DeleteOperation operation) {
		if (isTransformationRequired(operation)) {
			// here we need to forward-transform the new operation to take missed operations into account
			
		}
		else {
			// this is the simple case where the new operation has the same previous operation ID as the last locally executed operation
			this.connector.delete(operation.getPosition(), operation.getLength());
			this.history.add(operation);
		}
	}
	
	public boolean isTransformationRequired(AbstractOperation operation) {
		String previous = operation.getPreviousOperation();
		String lastLocalOperation = getLastOperationID();
		
		return !isEquals(previous, lastLocalOperation);
	}
	
	public boolean isEquals(String op1ID, String op2ID) {
		if (op1ID == null && op2ID == null) return true;
		if (op1ID != null && op2ID != null && op1ID.equals(op2ID)) return true;
		return false;
	}

	/**
	 * @param operation
	 * @return
	 */
	private InsertOperation forwardTransform(InsertOperation operation) {
		if (operation.getPreviousOperation() == null && this.history.size() == 0) {
			return operation;
		}
		
		if (this.history.size() > 0 && this.history.get(history.size() - 1).getId().equals(operation.getPreviousOperation())) {
			return operation;
		}
		
		int startIndex = -1;
		if (operation.getPreviousOperation() == null) {
			startIndex = 0;
		}
		for (int i = this.history.size() - 1; i >= 0; i--) {
			if (this.history.get(i).getId().equals(operation.getPreviousOperation())) {
				startIndex = i + 1;
			}
		}
		
		// do the forward transformation, if necessary
		InsertOperation transformedOperation = operation;
		if (startIndex >= 0 && startIndex < this.history.size()) {
			for (int i = startIndex; i < this.history.size(); i++) {
				AbstractOperation historicOperation = this.history.get(i);
				transformedOperation = transformedOperation.transformForward(historicOperation);
			}
		}
		
		return transformedOperation;
	}

	/**
	 * @param characters The characters to insert
	 * @param position The position where to insert the characters
	 */
	public InsertOperation createInsertOperation(String characters, int position) {
		String previousOperation = null;
		if (history.size() > 0) {
			previousOperation = history.get(history.size() - 1).getId();
		}
		
		String operationID = this.id + "-" + counter++;
		return new InsertOperation(operationID, previousOperation, characters, position);
	}

	/**
	 * @param position The position from which to delete the characters
	 * @param length The number of characters to delete
	 */
	public DeleteOperation createDeleteOperation(int position, int length) {
		String previousOperation = null;
		if (history.size() > 0) {
			previousOperation = history.get(history.size() - 1).getId();
		}
		
		String operationID = this.id + "-" + counter++;
		return new DeleteOperation(operationID, previousOperation, position, length);
	}

	/**
	 * @return
	 */
	public String getLastOperationID() {
		if (history.size() > 0) {
			return history.get(history.size() - 1).getId();
		}
		else {
			return null;
		}
	}

}
