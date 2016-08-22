package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.IRepositoryCallback;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectsResponseHandler extends AbstractMsgHandler {

    public ProjectsResponseHandler(IRepositoryCallback repositoryCallback) {
        super(repositoryCallback, "getProjectsRequest");
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        int callbackID = message.optInt(MessageConstants.CALLBACK_ID);
        String requestSenderId = message.getString(MessageConstants.REQUEST_SENDER_ID);
        String username = message.getString(MessageConstants.USERNAME);
        
        JSONArray projects = new JSONArray();
        for (Project fluxProject : repositoryCallback.getSynchronizedProjects()) {
            JSONObject project = new JSONObject();
            project.put("name", fluxProject.id());
            projects.put(project);
        }

        JSONObject content = new JSONObject();
        content.put("callback_id", callbackID);
        content.put("requestSenderID", requestSenderId);
        content.put("username", username);
        content.put("projects", projects);

        repositoryCallback.sendMessage("getProjectsResponse", content);
    }
}
