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

import java.util.Collection;
import java.util.HashSet;

/**
 * Concrete implementation of Observable that provides a 'setter' method
 * to change the current value.
 */
public class Variable<T> implements Observable<T> {

	private T value;

	private Collection<Listener<T>> listeners;
	
	public Variable(T initialValue) {
		this.value = initialValue;
		changed(initialValue); //Initialization is treated as a change event.
	}

	private void changed(T value) {
		for (Listener<T> l : listeners) {
			l.changed(this, value);
		}
	}

	@Override
	public T getValue() {
		return value;
	}
	
	public synchronized void setValue(T v) {
		if (equal(this.value, v)) {
			return;
		}
		this.value = v;
	}

	private boolean equal(T a, T b) {
		if (a==null) {
			return b==null;
		}
		return a.equals(b);
	}

	@Override
	public synchronized void addListener(Listener<T> l) {
		if (this.listeners==null) {
			this.listeners = createCollection();
		}
		this.listeners.add(l);
	}

	private HashSet<Listener<T>> createCollection() {
		return new HashSet<Listener<T>>();
	}

	@Override
	public synchronized void removeListener(Listener<T> l) {
		if (this.listeners!=null) {
			this.listeners.remove(l);
		}
	}

}
