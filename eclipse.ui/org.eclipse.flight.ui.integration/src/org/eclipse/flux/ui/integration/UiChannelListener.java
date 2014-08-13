package org.eclipse.flux.ui.integration;

import org.eclipse.core.resources.IProject;
import org.eclipse.flux.core.IChannelListener;
import org.eclipse.flux.core.IRepositoryListener;
import org.eclipse.flux.core.LiveEditCoordinator;
import org.eclipse.flux.core.Repository;
import org.eclipse.flux.ui.integration.handlers.LiveEditConnector;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;

public class UiChannelListener implements IChannelListener {
	
	private LiveEditConnector liveEditConnector = null;
	
	private IRepositoryListener repositoryListener = new IRepositoryListener() {
		@Override
		public void projectDisconnected(IProject project) {
			updateProjectLabel(project);
		}

		@Override
		public void projectConnected(IProject project) {
			updateProjectLabel(project);
		}
	};

	@Override
	public void connected(String userChannel) {
		Repository repository = org.eclipse.flux.core.Activator
				.getDefault().getRepository();
		
		repository.addRepositoryListener(repositoryListener);

		if (Boolean.getBoolean("flux-eclipse-editor-connect")) {
			LiveEditCoordinator liveEditCoordinator = org.eclipse.flux.core.Activator
					.getDefault().getLiveEditCoordinator();
			liveEditConnector = new LiveEditConnector(liveEditCoordinator, repository);
		}
	}

	protected static void updateProjectLabel(final IProject project) {
		final CloudProjectDecorator projectDecorator = CloudProjectDecorator
				.getInstance();
		if (projectDecorator != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					projectDecorator
							.fireLabelProviderChanged(new LabelProviderChangedEvent(
									projectDecorator, project));
				}
			});
		}
	}

	@Override
	public void disconnected(String userChannel) {
		org.eclipse.flux.core.Activator
			.getDefault().getRepository().removeRepositoryListener(repositoryListener);
		if (liveEditConnector != null) {
			liveEditConnector.dispose();
		}
	}

}
