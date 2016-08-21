package org.eclipse.flux.core.handlers;

import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.core.util.JSONUtils;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;

public class MetadataRequestHandler extends AbstractFluxMessageHandler {

	public MetadataRequestHandler(MessageConnector messageConnector, Repository repository) {
        super(messageConnector, repository, GET_METADATA_REQUEST);
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String projectName = message.getString("project");
        String resourcePath = message.getString("resource");
        Project project = repository.getProject(projectName);
        if(project != null){
            Resource resource = project.getResource(resourcePath);
            if(resource != null){
                Path path = new Path(MessageFormat.format("{0}/{1}", projectName, resourcePath));
                IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
                message.put("type", "marker");
                message.put("metadata", JSONUtils.toJSON(file.findMarkers(null, true, IResource.DEPTH_INFINITE)));
                messageConnector.send(GET_METADATA_RESPONSE, message);
            }
        }        
    }
}