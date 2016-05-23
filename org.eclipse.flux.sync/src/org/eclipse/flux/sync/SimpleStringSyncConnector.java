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
 * A simple string-based implementation of the Sync connector that keeps the internal String in sync with other
 * participants when connected to a Sync Controller.
 * 
 * @author Martin Lippert
 */
public class SimpleStringSyncConnector implements SyncConnector {
	
	private StringBuilder text;

	/**
	 * create a new simple string sync connector with the given initial text content
	 * 
	 * @param initalText The initial text content, <code>null</code> is not allowed here and will throw a {@link NullPointerException}
	 */
	public SimpleStringSyncConnector(final String initalText) {
		this.text = new StringBuilder(initalText);
	}
	
	/**
	 * the content of the synced string
	 * 
	 * @return The content of the current sync
	 */
	public synchronized String getText() {
		return this.text.toString();
	}

	@Override
	public synchronized void insert(String characters, int position) {
		this.text.insert(position, characters);
	}

	@Override
	public synchronized void delete(int position, int length) {
		this.text.delete(position, position + length);
	}
	
}
