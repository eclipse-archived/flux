package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.IRepositoryCallback;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.Resource.ResourceType;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;
//Add listener for notifyResourceChanged
public class ResourceChangedHandler extends AbstractMsgHandler {
    private int callbackId;
    
    public ResourceChangedHandler(IRepositoryCallback repositoryCallback, int callbackID) {
        super(repositoryCallback, RESOURCE_CHANGED);
        this.callbackId = callbackID;
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String username = message.getString(MessageConstants.USERNAME);
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        String resourcePath = message.getString(MessageConstants.RESOURCE);
        long resourceTimestamp = message.getLong(MessageConstants.TIMESTAMP);
        String resourceHash = message.getString(MessageConstants.HASH);
        Project project = repositoryCallback.getProject(projectName);
        if(project == null) 
            return;
        Resource localResource = project.getResource(resourcePath);
        if(localResource == null || localResource.type() != ResourceType.FILE)
            return;
        if (localResource != null && IsResourcesNotEquals(localResource, resourceHash, resourceTimestamp)) {
            JSONObject content = new JSONObject();
            content.put(MessageConstants.CALLBACK_ID, this.callbackId);
            content.put(MessageConstants.USERNAME, username);
            content.put(MessageConstants.PROJECT_NAME, projectName);
            content.put(MessageConstants.RESOURCE, resourcePath);
            content.put(MessageConstants.TIMESTAMP, resourceTimestamp);
            content.put(MessageConstants.HASH, resourceHash);
            repositoryCallback.sendMessage(GET_RESOURCE_REQUEST, content);        
        }
    }
}