package org.eclipse.flux.core.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.Resource.ResourceType;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;

public class ResourceResponseHandler extends AbstractFluxMessageHandler {
    private int callbackID;

    public ResourceResponseHandler(MessageConnector messageConnector, Repository repository, int callbackID) {
        super(messageConnector, repository, GET_RESOURCE_RESPONSE);
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
        
        Project project = repository.getProject(projectName);
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
            updateEclipseEditor(projectName, resourcePath);
            JSONObject content = new JSONObject();
            content.put(MessageConstants.USERNAME, username);
            content.put(MessageConstants.PROJECT_NAME, projectName);
            content.put(MessageConstants.RESOURCE, resourcePath);
            content.put(MessageConstants.TIMESTAMP, resourceTimestamp);
            content.put(MessageConstants.HASH, resourceHash);
            content.put(MessageConstants.TYPE, "file");
            messageConnector.send(RESOURCE_STORED, content);
        }
    }

    private void updateEclipseEditor(String projectName, String resourcePath) throws CoreException {
        Path path = new Path(projectName + "/" + resourcePath);
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        file.refreshLocal(IResource.DEPTH_ZERO, null);
    }
    
    @Override
    public boolean canHandle(String messageType, JSONObject message) {
        return super.canHandle(messageType, message) 
                && message.has("callback_id") 
                && message.optInt("callback_id") == callbackID;
    }
}
