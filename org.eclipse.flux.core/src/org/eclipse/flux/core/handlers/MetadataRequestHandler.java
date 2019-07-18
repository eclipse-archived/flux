package org.eclipse.flux.core.handlers;

import org.eclipse.core.resources.IResource;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.sync.ISystemSync;
import org.eclipse.flux.core.util.JSONUtils;
import org.eclipse.flux.core.util.Utils;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;

public class MetadataRequestHandler extends AbstractMsgHandler {

	public MetadataRequestHandler(ISystemSync repositoryCallback) {
        super(repositoryCallback, GET_METADATA_REQUEST);
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        String resourcePath = message.getString(MessageConstants.RESOURCE);
        Project project = repositoryCallback.getWatcherProject(projectName);
        if(project != null){
            Resource resource = project.getResource(resourcePath);
            if(resource != null){
                IResource file = Utils.getResourceByPath(projectName, resourcePath);
                message.put(MessageConstants.TYPE, "marker");
                message.put(MessageConstants.METADATA, JSONUtils.toJSON(file.findMarkers(null, true, IResource.DEPTH_INFINITE)));
                repositoryCallback.sendMessage(GET_METADATA_RESPONSE, message);
            }
        }        
    }
}