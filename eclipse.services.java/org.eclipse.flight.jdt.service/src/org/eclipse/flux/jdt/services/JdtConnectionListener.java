package org.eclipse.flux.jdt.services;

import org.eclipse.flux.core.IChannelListener;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.flux.core.LiveEditCoordinator;
import org.eclipse.flux.core.Repository;

public class JdtConnectionListener implements IChannelListener {

	@Override
	public void connected(String userChannel) {
		IMessagingConnector messagingConnector = org.eclipse.flux.core.Activator
				.getDefault().getMessagingConnector();
		Repository repository = org.eclipse.flux.core.Activator.getDefault()
				.getRepository();
		LiveEditCoordinator liveEditCoordinator = org.eclipse.flux.core.Activator
				.getDefault().getLiveEditCoordinator();

		LiveEditUnits liveEditUnits = new LiveEditUnits(messagingConnector,
				liveEditCoordinator, repository);
		new ContentAssistService(messagingConnector, liveEditUnits);
		new NavigationService(messagingConnector, liveEditUnits);
		new RenameService(messagingConnector, liveEditUnits);

		InitializeServiceEnvironment initializer = new InitializeServiceEnvironment(
				messagingConnector, repository);
		initializer.start();
	}

	@Override
	public void disconnected(String userChannel) {
		// nothing
	}

}
