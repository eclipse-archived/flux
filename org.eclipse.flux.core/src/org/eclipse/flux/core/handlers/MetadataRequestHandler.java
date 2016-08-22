package org.eclipse.flux.core.handlers;

import org.eclipse.core.resources.IResource;
import org.eclipse.flux.core.IRepositoryCallback;
import org.eclipse.flux.core.util.JSONUtils;
import org.eclipse.flux.core.util.Utils;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;

public class MetadataRequestHandler extends AbstractMsgHandler {

	public MetadataRequestHandler(IRepositoryCallback repositoryCallback) {
        super(repositoryCallback, GET_METADATA_REQUEST);
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String projectName = message.getString("project");
        String resourcePath = message.getString("resource");
        Project project = repositoryCallback.getProject(projectName);
        if(project != null){
            Resource resource = project.getResource(resourcePath);
            if(resource != null){
                IResource file = Utils.getResourceByPath(projectName, resourcePath);
                message.put("type", "marker");
                message.put("metadata", JSONUtils.toJSON(file.findMarkers(null, true, IResource.DEPTH_INFINITE)));
                repositoryCallback.sendMessage(GET_METADATA_RESPONSE, message);
            }
        }        
    }
}