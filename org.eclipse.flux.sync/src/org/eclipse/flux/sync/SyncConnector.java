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
 * The sync connector interface can be implemented to connect text change events to a specific text document.
 * This can be used to bridge between existing text documents like an IDocument in Eclipse or a text widget somewhere
 * (or whatever) and the events from the sync mechanism.
 * 
 * An object of this sync connector is called by the sync mechanism whenever the text should change due to events
 * from outside.
 * 
 * @author Martin Lippert
 */
public interface SyncConnector {

	/**
	 * Insert the given characters at the given position into the text document.
	 * 
	 * @param characters The characters to be inserted
	 * @param position The position where to insert the characters, starting with 0 to insert right at the beginning
	 */
	void insert(String characters, int position);
	
	/**
	 * Delete the given number of characters at the given position
	 * 
	 * @param position The position where to delete the characters, starting with 0 to delete the first character
	 * @param length The number of characters to delete from the text document
	 */
	void delete(int position, int length);

}
