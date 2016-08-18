package org.eclipse.flux.core.handlers;

import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.flux.watcher.core.FluxMessage;
import org.eclipse.flux.watcher.core.FluxMessageHandler;
import org.eclipse.flux.watcher.core.FluxMessageType;
import org.eclipse.flux.watcher.core.FluxMessageTypes;
import org.eclipse.flux.watcher.core.Repository;

import com.google.inject.Singleton;

@Singleton
@FluxMessageTypes(FluxMessageType.GET_RESOURCE_RESPONSE)
public class EclipseResourceResponseHandler implements FluxMessageHandler {

	@Override
	public void onMessage(FluxMessage message, Repository repository) throws Exception {
		final String resourcePath = message.getContent().getString("resource");
		final String project = message.getContent().getString("project");
		Path path = new Path(MessageFormat.format("{0}/{1}", project, resourcePath));
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		file.refreshLocal(IResource.DEPTH_ZERO, null);
	}

}
