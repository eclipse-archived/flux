package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.client.MessageHandler;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.Resource.ResourceType;
import org.json.JSONObject;

public abstract class AbstractFluxMessageHandler extends MessageHandler {
    protected Repository repository;
    protected MessageConnector messageConnector;
    
    public AbstractFluxMessageHandler(MessageConnector messageConnector, Repository repository, String type) {
        super(type);
        this.repository = repository;
        this.messageConnector = messageConnector;
    }

    @Override
    public void handle(String type, JSONObject message) {
        try{
            onMessage(type, message);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    protected abstract void onMessage(String type, JSONObject message) throws Exception;
    
    protected ResourceType getResourceType(JSONObject jsonObject){
        String type = jsonObject.optString(MessageConstants.TYPE, ResourceType.UNKNOWN.name());
        return ResourceType.valueOf(type.toUpperCase());
    }
    
    protected boolean IsResourcesNotEquals(Resource resource, String hash, long timestamp){
        boolean isLocalResourceOutdated = resource.timestamp() < timestamp;
        boolean isEquals = resource.hash().equals(hash);
        return isLocalResourceOutdated && !isEquals;
    }
}