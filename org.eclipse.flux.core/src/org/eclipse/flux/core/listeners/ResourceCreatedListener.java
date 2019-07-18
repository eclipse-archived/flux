package org.eclipse.flux.core.listeners;

import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.sync.ISystemSync;
import org.eclipse.flux.watcher.core.RepositoryEvent;
import org.eclipse.flux.watcher.core.RepositoryEventType;
import org.eclipse.flux.watcher.core.RepositoryEventTypes;
import org.eclipse.flux.watcher.core.RepositoryListener;
import org.json.JSONObject;

@RepositoryEventTypes(RepositoryEventType.PROJECT_RESOURCE_CREATED)
public class ResourceCreatedListener implements RepositoryListener {
    private ISystemSync repositoryCallback;
    
    public ResourceCreatedListener(ISystemSync repositoryCallback){
        this.repositoryCallback = repositoryCallback;
    }
    @Override
    public void onEvent(RepositoryEvent event) throws Exception {
        JSONObject message = new JSONObject();
        message.put(MessageConstants.USERNAME, repositoryCallback.getUsername());
        message.put(MessageConstants.PROJECT_NAME, event.project().id());
        message.put(MessageConstants.RESOURCE, event.resource().path());
        message.put(MessageConstants.TIMESTAMP, event.resource().timestamp());
        message.put(MessageConstants.HASH, event.resource().hash());
        message.put(MessageConstants.TYPE, event.resource().type().name().toLowerCase());
        repositoryCallback.sendMessage(IMessageHandler.RESOURCE_CREATED, message);
        repositoryCallback.sendMessage(IMessageHandler.RESOURCE_STORED, message);
    }
}
