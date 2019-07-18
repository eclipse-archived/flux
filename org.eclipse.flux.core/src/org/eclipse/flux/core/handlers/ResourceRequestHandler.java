package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.sync.ISystemSync;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.Resource.ResourceType;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONException;
import org.json.JSONObject;

public class ResourceRequestHandler extends AbstractMsgHandler {
    public ResourceRequestHandler(ISystemSync repositoryCallback) {
        super(repositoryCallback, GET_RESOURCE_REQUEST);
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        Project project = repositoryCallback.getWatcherProject(message.getString(MessageConstants.PROJECT_NAME));
        if (project == null)
            return;
        Resource resource = project.getResource(message.getString(MessageConstants.RESOURCE));
        if (resource == null || compareTimestamp(message, resource))
            return;
        message.put(MessageConstants.TIMESTAMP, resource.timestamp());
        message.put(MessageConstants.HASH, resource.hash());
        message.put(MessageConstants.TYPE, resource.type().name().toLowerCase());
        if (resource.type() == ResourceType.FILE && !compareHash(message, resource)) {
            message.put(MessageConstants.CONTENT, new String(resource.content()));
        }
        repositoryCallback.sendMessage(GET_RESOURCE_RESPONSE, message);
    }

    private boolean compareHash(JSONObject message, Resource resource) throws JSONException {
        return message.has(MessageConstants.HASH) && !message.getString(MessageConstants.HASH).equals(resource.hash());
    }

    private boolean compareTimestamp(JSONObject message, Resource resource) throws JSONException {
        return message.has(MessageConstants.TIMESTAMP) && message.getLong(MessageConstants.TIMESTAMP) != resource.timestamp();
    }
}