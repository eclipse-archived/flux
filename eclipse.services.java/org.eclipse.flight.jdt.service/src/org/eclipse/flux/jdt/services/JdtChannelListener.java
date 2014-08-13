package org.eclipse.flux.jdt.services;

import org.eclipse.flux.core.IChannelListener;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.flux.core.LiveEditCoordinator;
import org.eclipse.flux.core.Repository;

public class JdtChannelListener implements IChannelListener {
	
	private LiveEditUnits liveEditUnits;
	private ContentAssistService contentAssistService;
	private NavigationService navigationService;
	private RenameService renameService;
	private InitializeServiceEnvironment initializer;

	@Override
	public void connected(String userChannel) {
		IMessagingConnector messagingConnector = org.eclipse.flux.core.Activator
				.getDefault().getMessagingConnector();
		Repository repository = org.eclipse.flux.core.Activator.getDefault()
				.getRepository();
		LiveEditCoordinator liveEditCoordinator = org.eclipse.flux.core.Activator
				.getDefault().getLiveEditCoordinator();

		this.liveEditUnits = new LiveEditUnits(messagingConnector,
				liveEditCoordinator, repository);
		this.contentAssistService = new ContentAssistService(messagingConnector, liveEditUnits);
		this.navigationService = new NavigationService(messagingConnector, liveEditUnits);
		this.renameService = new RenameService(messagingConnector, liveEditUnits);

		this.initializer = new InitializeServiceEnvironment(
				messagingConnector, repository);
		initializer.start();
	}

	@Override
	public void disconnected(String userChannel) {
		liveEditUnits.dispose();
		contentAssistService.dispose();
		navigationService.dispose();
		renameService.dispose();
		initializer.dispose();
	}

}
