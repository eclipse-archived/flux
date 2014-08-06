/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *     IBM Corporation - converted to DS component
 *******************************************************************************/
package org.eclipse.flux.jdt.services;

import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.flux.core.LiveEditCoordinator;
import org.eclipse.flux.core.Repository;
import org.eclipse.flux.core.ServiceConnector;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

/**
 * This component connects Java development tools (JDT) services to the Flux message bus.
 */
public class JDTComponent {
	@Activate
	public void activate(final ComponentContext context) {
		
		String username = System.getProperty("flux.user.name");
		String token = System.getProperty("flux.user.token");
		
		if (username == null) {
			new ServiceConnector("JDT") {

				@Override
				public void startService(String user, String token) {
					startJdtService(user, token);
				}

				@Override
				public void stopService() {
					try {
						System.out.println("FULL STOP");
						context.getBundleContext().getBundle(0).stop();
					} catch (BundleException e) {
						e.printStackTrace();
					}
				}
				
			};
		} else {
			startJdtService(username, token);
		}
	}
	
	private void startJdtService(String username, String token) {
		try {
			org.eclipse.flux.core.Activator.getDefault().startService(username, token);
			IMessagingConnector messagingConnector = org.eclipse.flux.core.Activator.getDefault().getMessagingConnector();
			Repository repository = org.eclipse.flux.core.Activator.getDefault().getRepository();
			LiveEditCoordinator liveEditCoordinator = org.eclipse.flux.core.Activator.getDefault().getLiveEditCoordinator();

			LiveEditUnits liveEditUnits = new LiveEditUnits(messagingConnector, liveEditCoordinator, repository);
			new ContentAssistService(messagingConnector, liveEditUnits);
			new NavigationService(messagingConnector, liveEditUnits);
			new RenameService(messagingConnector, liveEditUnits);
			
			InitializeServiceEnvironment initializer = new InitializeServiceEnvironment(messagingConnector, repository);
			initializer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
