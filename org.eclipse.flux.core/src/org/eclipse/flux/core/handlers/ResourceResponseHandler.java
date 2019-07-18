package org.eclipse.flux.core.handlers;

import org.eclipse.core.resources.IResource;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.sync.ISystemSync;
import org.eclipse.flux.core.util.Utils;
import org.eclipse.flux.watcher.core.RepositoryEvent;
import org.eclipse.flux.watcher.core.RepositoryEventType;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.Resource.ResourceType;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;

public class ResourceResponseHandler extends AbstractMsgHandler {
    private int callbackID;

    public ResourceResponseHandler(ISystemSync repositoryCallback, int callbackID) {
        super(repositoryCallback, GET_RESOURCE_RESPONSE);
        this.callbackID = callbackID;
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        String resourcePath = message.getString(MessageConstants.RESOURCE);
        long resourceTimestamp = message.getLong(MessageConstants.TIMESTAMP);
        String resourceHash = message.getString(MessageConstants.HASH);
        String resourceContent = message.getString(MessageConstants.CONTENT);
        String username = message.getString(MessageConstants.USERNAME);
        
        Project project = repositoryCallback.getWatcherProject(projectName);
        if(project == null)
            return;
        boolean isResourceStore = false;
        Resource localResource = project.getResource(resourcePath);
        Resource newResource = Resource.newFile(resourcePath, resourceTimestamp, resourceContent.getBytes());
        if(localResource != null && localResource.type() == ResourceType.FILE)
        {
            if(IsResourcesNotEquals(localResource, resourceHash, resourceTimestamp)){
                project.updateResource(newResource);
                isResourceStore = true;
            }
        }
        else {
            project.createResource(newResource);
            isResourceStore = true;
        }

        if(isResourceStore){
            Utils.getResourceByPath(projectName, resourcePath).refreshLocal(IResource.DEPTH_ZERO, null);
            JSONObject content = new JSONObject();
            content.put(MessageConstants.USERNAME, username);
            content.put(MessageConstants.PROJECT_NAME, projectName);
            content.put(MessageConstants.RESOURCE, resourcePath);
            content.put(MessageConstants.TIMESTAMP, resourceTimestamp);
            content.put(MessageConstants.HASH, resourceHash);
            content.put(MessageConstants.TYPE, "file");
            repositoryCallback.sendMessage(RESOURCE_STORED, content);
            if(localResource != null){
                RepositoryEvent event = new RepositoryEvent(RepositoryEventType.PROJECT_RESOURCE_MODIFIED, localResource, project);
                repositoryCallback.onEvent(event);
            }
        }
    }
    
    @Override
    public boolean canHandle(String messageType, JSONObject message) {
        return super.canHandle(messageType, message) 
                && message.has("callback_id") 
                && message.optInt("callback_id") == callbackID;
    }
}
