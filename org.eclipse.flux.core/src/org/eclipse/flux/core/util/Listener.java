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
package org.eclipse.flux.core.util;

/**
 * A listener is an entity that is interested to receive notifications when
 * an Observable's value has changed.
 */
public interface Listener<T> {

	/**
	 * Called when an Observable this listener is attached has its value changed.
	 */
	public void changed(Observable<T> o, T v);
	
}
