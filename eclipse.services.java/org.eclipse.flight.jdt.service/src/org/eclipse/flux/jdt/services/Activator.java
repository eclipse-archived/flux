/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.jdt.services;

import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.flux.core.LiveEditCoordinator;
import org.eclipse.flux.core.Repository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Martin Lippert
 */
public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		IMessagingConnector messagingConnector = org.eclipse.flux.core.Activator.getDefault().getMessagingConnector();
		Repository repository = org.eclipse.flux.core.Activator.getDefault().getRepository();
		LiveEditCoordinator liveEditCoordinator = org.eclipse.flux.core.Activator.getDefault().getLiveEditCoordinator(); 
		
		LiveEditUnits liveEditUnits = new LiveEditUnits(messagingConnector, liveEditCoordinator, repository);
		new ContentAssistService(messagingConnector, liveEditUnits);
		new NavigationService(messagingConnector, liveEditUnits);
		new RenameService(messagingConnector, liveEditUnits);
		
		if (Boolean.getBoolean("flux-initjdt")) {
			InitializeServiceEnvironment initializer = new InitializeServiceEnvironment(messagingConnector, repository);
			initializer.start();
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
