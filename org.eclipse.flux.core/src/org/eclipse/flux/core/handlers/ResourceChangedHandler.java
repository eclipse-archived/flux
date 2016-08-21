package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.Resource.ResourceType;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;
//Add listener for notifyResourceChanged
public class ResourceChangedHandler extends AbstractFluxMessageHandler {
    private int callbackId;
    
    public ResourceChangedHandler(MessageConnector messageConnector, Repository repository, int callbackID) {
        super(messageConnector, repository, RESOURCE_CHANGED);
        this.callbackId = callbackID;
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String username = message.getString(MessageConstants.USERNAME);
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        String resourcePath = message.getString(MessageConstants.RESOURCE);
        long resourceTimestamp = message.getLong(MessageConstants.TIMESTAMP);
        String resourceHash = message.getString(MessageConstants.HASH);
        Project project = repository.getProject(projectName);
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
            messageConnector.send(GET_RESOURCE_REQUEST, content);        
        }
    }
}