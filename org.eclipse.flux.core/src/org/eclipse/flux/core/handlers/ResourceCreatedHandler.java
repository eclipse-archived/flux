package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONObject;

public class ResourceCreatedHandler extends AbstractFluxMessageHandler {
    private int callbackID;

    public ResourceCreatedHandler(MessageConnector messageConnector, Repository repository, int callbackID) {
        super(messageConnector, repository, RESOURCE_CREATED);
        this.callbackID = callbackID;
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        String resourcePath = message.getString(MessageConstants.RESOURCE);
        long resourceTimestamp = message.getLong(MessageConstants.TIMESTAMP);
        String resourceHash = message.getString(MessageConstants.HASH);
        String username = message.getString(MessageConstants.USERNAME);
        Project project = repository.getProject(projectName);
        if(project == null || project.hasResource(resourcePath))
            return;
        JSONObject content = new JSONObject();
        content.put(MessageConstants.USERNAME, username);
        content.put(MessageConstants.PROJECT_NAME, projectName);
        content.put(MessageConstants.RESOURCE, resourcePath);
        content.put(MessageConstants.TIMESTAMP, resourceTimestamp);
        content.put(MessageConstants.HASH, resourceHash);
        content.put(MessageConstants.TYPE, getResourceType(message).name().toLowerCase());
        switch(getResourceType(message)){
            case FILE:
                content.put(MessageConstants.CALLBACK_ID, callbackID);
                messageConnector.send(GET_RESOURCE_REQUEST, content);
                break;
            case FOLDER:
                project.createResource(Resource.newFolder(resourcePath, resourceTimestamp));
                messageConnector.send(RESOURCE_STORED, content);
                break;
            default:
                break;
        }
    }
}
