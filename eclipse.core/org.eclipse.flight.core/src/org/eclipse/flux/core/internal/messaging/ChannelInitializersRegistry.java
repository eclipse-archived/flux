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
package org.eclipse.flux.core.internal.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.flux.core.IChannelListener;

/**
 * Loads channel listeners from extension point
 * 
 * @author aboyko
 *
 */
public class ChannelInitializersRegistry implements IChannelListener {
	
	private final static String EXT_PT_ID = "org.eclipse.flux.core.connectionInitialListeners";
	private final static String ATTR_CLASS = "class";
	private final static String ATTR_PRIORITY = "priority";
	
	private static ChannelInitializersRegistry instance = null;
	
	private List<ListenerDescriptor> descriptors;
	
	public static ChannelInitializersRegistry getInstance() {
		if (instance == null) {
			instance = new ChannelInitializersRegistry();
		}
		return instance;
	}
	
	private ChannelInitializersRegistry() {
		IConfigurationElement[] configurationElementsFor = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_PT_ID);
		descriptors = new ArrayList<ListenerDescriptor>(configurationElementsFor.length);
		for (IConfigurationElement element : configurationElementsFor) {
			try {
				descriptors.add(new ListenerDescriptor((IChannelListener)element.createExecutableExtension(ATTR_CLASS), Priority.valueOf(element.getAttribute(ATTR_PRIORITY))));
			} catch (InvalidRegistryObjectException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		Collections.sort(descriptors, new Comparator<ListenerDescriptor>() {

			@Override
			public int compare(ListenerDescriptor o1, ListenerDescriptor o2) {
				return o1.priority.ordinal() - o2.priority.ordinal();
			}
			
		});
	}
	
	@Override
	public void connected(String userChannel) {
		for (ListenerDescriptor descriptor : descriptors) {
			try {
				descriptor.listener.connected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	@Override
	public void disconnected(String userChannel) {
		for (int i = descriptors.size() - 1; i >= 0; i--) {
			try {
				descriptors.get(i).listener.disconnected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	private enum Priority {
		HIGHEST,
		HIGH,
		MEDIUM,
		LOW,
		LOWEST
	}
	
	private class ListenerDescriptor {
		
		private Priority priority;
		private IChannelListener listener;
		
		ListenerDescriptor(IChannelListener listener, Priority priority) {
			this.listener = listener;
			this.priority = priority;
		}

	}

}
