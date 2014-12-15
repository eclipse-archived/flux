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
package org.eclipse.flux.client.util;

/**
 * A listener is an entity that is interested to track the value of an observable.
 */
public interface Listener<T> {

	/**
	 * Called when an Observable this listener is attached has a 'new' value for
	 * this listener. This method is called by the Observable's on all its attached
	 * listeners whenever its value is changed. It is also called initially, when
	 * a listener is added to the Observable.
	 * <p>
	 * This means that listeners attached to an Observable are guaranteed to be called
	 * at least once by that Observable even if its value never changes.
	 */
	public void newValue(Observable<T> o, T v);
	
}
