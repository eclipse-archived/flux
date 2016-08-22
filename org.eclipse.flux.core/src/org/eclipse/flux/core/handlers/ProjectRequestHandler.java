package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.IRepositoryCallback;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectRequestHandler extends AbstractMsgHandler {

    public ProjectRequestHandler(IRepositoryCallback repositoryCallback) {
        super(repositoryCallback, GET_PROJECT_REQUEST);
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        Project project = repositoryCallback.getProject(projectName);
        if(project == null)
            return;
        JSONArray files = new JSONArray();
        for(Resource resource : project.getResources()){
            JSONObject file = new JSONObject();
            file.put(MessageConstants.PATH, resource.path());
            file.put(MessageConstants.TIMESTAMP, resource.timestamp());
            file.put(MessageConstants.HASH, resource.hash());
            file.put(MessageConstants.TYPE, resource.type().name().toLowerCase());
            files.put(file);
        }
        message.put(MessageConstants.FILES, files);
        repositoryCallback.sendMessage(GET_PROJECT_RESPONSE, message);
    }
}