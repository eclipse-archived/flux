package org.eclipse.flux.core.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.core.handlers.MetadataRequestHandler;
import org.eclipse.flux.core.handlers.ProjectRequestHandler;
import org.eclipse.flux.core.handlers.ProjectResponseHandler;
import org.eclipse.flux.core.handlers.ProjectsResponseHandler;
import org.eclipse.flux.core.handlers.ResourceChangedHandler;
import org.eclipse.flux.core.handlers.ResourceCreatedHandler;
import org.eclipse.flux.core.handlers.ResourceDeletedHandler;
import org.eclipse.flux.core.handlers.ResourceRequestHandler;
import org.eclipse.flux.core.handlers.ResourceResponseHandler;
import org.eclipse.flux.core.listeners.ResourceCreatedListener;
import org.eclipse.flux.core.listeners.ResourceDeletedListener;
import org.eclipse.flux.core.listeners.ResourceModifiedListener;
import org.eclipse.flux.core.util.JSONUtils;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.RepositoryEvent;
import org.eclipse.flux.watcher.core.RepositoryEventBus;
import org.eclipse.flux.watcher.core.RepositoryModule;
import org.eclipse.flux.watcher.core.spi.Project;
import org.eclipse.flux.watcher.fs.JDKProjectModule;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class FluxSystemSync implements ISystemSync {
    private static int GET_PROJECT_CALLBACK = "Repository - getProjectCallback".hashCode();
    private static int GET_RESOURCE_CALLBACK = "Repository - getResourceCallback".hashCode();
    
    private String username;
    private Repository repository;
    private RepositoryEventBus repositoryEventBus;
    private MessageConnector messageConnector;
    private Collection<IMessageHandler> messageHandlers;
    
    public FluxSystemSync(MessageConnector messageConnector, String user) {
        this.messageConnector = messageConnector;
        this.username = user;
        this.messageHandlers = new ArrayList<>();
        Injector injector = Guice.createInjector(new RepositoryModule(), new JDKProjectModule());
        this.repository = injector.getInstance(Repository.class);
        this.repositoryEventBus = this.repository.repositoryEventBus();
        
        this.repositoryEventBus.addRepositoryListener(new ResourceCreatedListener(this));
        this.repositoryEventBus.addRepositoryListener(new ResourceModifiedListener(this));
        this.repositoryEventBus.addRepositoryListener(new ResourceDeletedListener(this));

        addMessageHandler(new MetadataRequestHandler(this));
        addMessageHandler(new ProjectsResponseHandler(this));
        addMessageHandler(new ProjectRequestHandler(this));
        addMessageHandler(new ProjectResponseHandler(this, GET_PROJECT_CALLBACK));
        addMessageHandler(new ResourceRequestHandler(this));
        addMessageHandler(new ResourceResponseHandler(this, GET_RESOURCE_CALLBACK));
        addMessageHandler(new ResourceCreatedHandler(this, GET_RESOURCE_CALLBACK));
        addMessageHandler(new ResourceChangedHandler(this, GET_RESOURCE_CALLBACK));
        addMessageHandler(new ResourceDeletedHandler(this));
    }


    public Set<Project> getSynchronizedProjects() {
        return this.repository.getSynchronizedProjects();
    }
    
    public void addProject(String name, String path) {
        this.repository.addProject(name, path);
        sendProjectConnectedMessage(name);
        syncConnectedProject(name);
    }
    
    public void removeProject(String name) {
        this.repository.removeProject(name);
        try {
            JSONObject message = new JSONObject();
            message.put("username", this.username);
            message.put("project", name);
            messageConnector.send("projectDisconnected", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendMetadataUpdate(IResource resource) {
        try {
            String project = resource.getProject().getName();
            String resourcePath = resource.getProjectRelativePath().toString();

            JSONObject message = new JSONObject();
            message.put("username", this.username);
            message.put("project", project);
            message.put("resource", resourcePath);
            message.put("type", "marker");

            IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_INFINITE);
            JSONArray content = JSONUtils.toJSON(markers);
            message.put("metadata", content);
            messageConnector.send(IMessageHandler.METADATA_CHANGED, message);
        } catch (Exception e) {
            e.printStackTrace();
        }        
    }



    public boolean isProjectConnected(String projectName) {
        return Objects.nonNull(this.getWatcherProject(projectName));
    }

    public String getUsername() {
        return this.username;
    }

    private void syncConnectedProject(String projectName){
        try{
            JSONObject message=new JSONObject();
            message.put("username",this.username);
            message.put("project",projectName);
            message.put("includeDeleted",true);
            message.put("callback_id", GET_PROJECT_CALLBACK);
            messageConnector.send("getProjectRequest",message);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void sendProjectConnectedMessage(String projectName){
        try{
            JSONObject message=new JSONObject();
            message.put("username",this.username);
            message.put("project",projectName);
            messageConnector.send("projectConnected",message);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void dispose() {
        for(IMessageHandler messageHandler : messageHandlers){
            messageConnector.removeMessageHandler(messageHandler);
        }        
    }
    
    private void addMessageHandler(IMessageHandler messageHandler){
        this.messageConnector.addMessageHandler(messageHandler);
        this.messageHandlers.add(messageHandler);
    }
    
    @Override
    public void onEvent(RepositoryEvent event) throws Exception {
        
    }

    @Override
    public void sendMessage(String messageType, JSONObject content) throws Exception {
        this.messageConnector.send(messageType, content);
    }

    @Override
    public Project getWatcherProject(String projectName) {
        return this.repository.getProject(projectName);
    }
}