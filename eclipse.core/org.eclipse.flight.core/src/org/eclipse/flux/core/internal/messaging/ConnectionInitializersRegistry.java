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

public class ConnectionInitializersRegistry implements IChannelListener {
	
	private final static String EXT_PT_ID = "org.eclipse.flux.core.connectionInitialListeners";
	private final static String ATTR_CLASS = "class";
	private final static String ATTR_PRIORITY = "priority";
	
	private static ConnectionInitializersRegistry instance = null;
	
	private List<ListenerDescriptor> descriptors;
	
	public static ConnectionInitializersRegistry getInstance() {
		if (instance == null) {
			instance = new ConnectionInitializersRegistry();
		}
		return instance;
	}
	
	private ConnectionInitializersRegistry() {
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
