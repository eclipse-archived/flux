package org.eclipse.flux.ui.integration;

import org.eclipse.core.resources.IProject;
import org.eclipse.flux.core.IChannelListener;
import org.eclipse.flux.core.IRepositoryListener;
import org.eclipse.flux.core.LiveEditCoordinator;
import org.eclipse.flux.core.Repository;
import org.eclipse.flux.ui.integration.handlers.LiveEditConnector;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;

public class UiConnectionListener implements IChannelListener {

	@Override
	public void connected(String userChannel) {
		org.eclipse.flux.core.Activator.getDefault().getRepository()
				.addRepositoryListener(new IRepositoryListener() {
					@Override
					public void projectDisconnected(IProject project) {
						updateProjectLabel(project);
					}

					@Override
					public void projectConnected(IProject project) {
						updateProjectLabel(project);
					}
				});

		if (Boolean.getBoolean("flux-eclipse-editor-connect")) {
			Repository repository = org.eclipse.flux.core.Activator
					.getDefault().getRepository();
			LiveEditCoordinator liveEditCoordinator = org.eclipse.flux.core.Activator
					.getDefault().getLiveEditCoordinator();
			new LiveEditConnector(liveEditCoordinator, repository);
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
		// nothing
	}

}
