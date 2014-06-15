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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

/**
 * This component connects Java development tools (JDT) services to the Flux message bus.
 */
public class JDTComponent {
	@Activate
	public void activate(ComponentContext context) {
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
}
