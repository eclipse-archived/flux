package org.eclipse.flux.core.handlers;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.IRepositoryCallback;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectsResponseHandler extends AbstractMsgHandler {

    public ProjectsResponseHandler(IRepositoryCallback repositoryCallback) {
        super(repositoryCallback, GET_PROJECTS_REQUEST);
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        int callbackID = message.optInt(MessageConstants.CALLBACK_ID);
        String requestSenderId = message.getString(MessageConstants.REQUEST_SENDER_ID);
        String username = message.getString(MessageConstants.USERNAME);
        
        JSONArray projects = new JSONArray();
        for (Project fluxProject : repositoryCallback.getSynchronizedProjects()) {
            JSONObject project = new JSONObject();
            project.put(MessageConstants.NAME, fluxProject.id());
            projects.put(project);
        }

        JSONObject content = new JSONObject();
        content.put(MessageConstants.CALLBACK_ID, callbackID);
        content.put(MessageConstants.REQUEST_SENDER_ID, requestSenderId);
        content.put(MessageConstants.USERNAME, username);
        content.put(MessageConstants.PROJECTS, projects);

        repositoryCallback.sendMessage(GET_PROJECTS_RESPONSE, content);
    }
}
