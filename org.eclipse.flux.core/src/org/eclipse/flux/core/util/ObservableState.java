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
public class ObservableState<T> implements Observable<T> {

	private T value;

	private Collection<Listener<T>> listeners;
	
	public ObservableState(T initialValue) {
		this.value = initialValue;
		notifyNewValue(initialValue); //Initialization is treated as a change event.
	}

	@SuppressWarnings("unchecked")
	private void notifyNewValue(T value) {
		Listener<T>[] listeners = null; 
		synchronized (this) {
			if (this.listeners!=null) {
				listeners = this.listeners.toArray(new Listener[this.listeners.size()]);
			}
		}
		//Careful... we want to make sure we notify listeners outside synch block!
		// TODO: actually it would be better to notify listeneres in a separate thread/stack. 
		//       as we do not know which locks might be held by code calling into
		//       to the 'setValue'.
		if (listeners!=null) {
			for (Listener<T> l : listeners) {
				notifyNewValue(l, value);
			}
		}
	}

	private void notifyNewValue(Listener<T> l, T value) {
		l.newValue(this, value);
	}

	@Override
	public T getValue() {
		return value;
	}
	
	public synchronized void setValue(T v) {
		synchronized (this) {
			if (equal(this.value, v)) {
				return;
			}
			this.value = v;
		}
		notifyNewValue(v);
	}

	private boolean equal(T a, T b) {
		if (a==null) {
			return b==null;
		}
		return a.equals(b);
	}

	@Override
	public void addListener(Listener<T> l) {
		synchronized (this) {
			if (this.listeners==null) {
				this.listeners = createCollection();
			}
			this.listeners.add(l);
		}
		notifyNewValue(l, value);
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
