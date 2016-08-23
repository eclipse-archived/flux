package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.IRepositoryCallback;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectResponseHandler extends AbstractMsgHandler {
    private int callbackID;
    
    public ProjectResponseHandler(IRepositoryCallback repositoryCallback, int callbackID) {
        super(repositoryCallback, GET_PROJECT_RESPONSE);
        this.callbackID = callbackID;
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        JSONArray files = message.getJSONArray(MessageConstants.FILES);
        JSONArray deleted = message.getJSONArray(MessageConstants.DELETED);
        Project project = repositoryCallback.getWatcherProject(message.getString(MessageConstants.PROJECT_NAME));
        if(project == null)
            return;
        for(int i = 0; i < files.length(); i++){
            JSONObject resource = files.getJSONObject(i);
            String path = resource.getString(MessageConstants.PATH);
            long timestamp = resource.getLong(MessageConstants.TIMESTAMP);
            String hash = resource.optString(MessageConstants.HASH);
            Resource localResource = project.getResource(path);
            switch(getResourceType(resource)){
                case FILE:
                    if(localResource == null || IsResourcesNotEquals(localResource, hash, timestamp)){
                        JSONObject content = new JSONObject();
                        content.put(MessageConstants.CALLBACK_ID, callbackID);
                        content.put(MessageConstants.PROJECT_NAME, project.id());
                        content.put(MessageConstants.RESOURCE, path);
                        content.put(MessageConstants.TIMESTAMP, timestamp);
                        content.put(MessageConstants.HASH, hash);
                        repositoryCallback.sendMessage(GET_RESOURCE_REQUEST, content);
                    }
                    break;
                case FOLDER:
                    if(localResource == null)
                        project.createResource(Resource.newFolder(path, timestamp));
                    break;
                default:
                    break;
            }
        }
        if(deleted != null){
            for(int i = 0; i < deleted.length(); i++){
                JSONObject resource = deleted.getJSONObject(i);
                String path = resource.getString(MessageConstants.PATH);
                long timestamp = resource.getLong(MessageConstants.TIMESTAMP);
                Resource localResource = project.getResource(path);
                if(localResource != null && localResource.timestamp() < timestamp)
                    project.deleteResource(localResource);
            }
        }
        
    }

    @Override
    public boolean canHandle(String messageType, JSONObject message) {
        return super.canHandle(messageType, message) && message.has("callback_id")
                && message.optInt("callback_id") == this.callbackID;
    }

}
