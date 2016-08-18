package org.eclipse.flux.core.handlers;

import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.flux.watcher.core.FluxMessage;
import org.eclipse.flux.watcher.core.FluxMessageHandler;
import org.eclipse.flux.watcher.core.FluxMessageType;
import org.eclipse.flux.watcher.core.FluxMessageTypes;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Singleton;

@Singleton
@FluxMessageTypes({FluxMessageType.GET_METADATA_REQUEST})
public class MetadataRequestHandler implements FluxMessageHandler {

	@Override
	public void onMessage(FluxMessage message, Repository repository) throws Exception {
		JSONObject content = message.getContent();
		String projectName = message.getContent().getString("project");
		String resourcePath = message.getContent().getString("resource");
		Project project = repository.getProject(projectName);
		if(project != null){
			Resource resource = project.getResource(resourcePath);
			if(resource != null){
				Path path = new Path(MessageFormat.format("{0}/{1}", projectName, resourcePath));
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				content.put("type", "marker");
				content.put("metadata", toJSON(file.findMarkers(null, true, IResource.DEPTH_INFINITE)));
				message.getSource().sendMessage(new FluxMessage(FluxMessageType.GET_METADATA_RESPONSE, content));
			}
		}
	}
	
	public JSONArray toJSON(IMarker[] markers) throws JSONException{
		JSONArray objects = new JSONArray();
		for(IMarker marker : markers){
			JSONObject object = new JSONObject();
			object.put("description", marker.getAttribute("message", ""));
			object.put("line", marker.getAttribute("lineNumber", 0));
			switch(marker.getAttribute("severity", IMarker.SEVERITY_WARNING)){
				case IMarker.SEVERITY_WARNING:
					object.put("severity", marker.getAttribute("severity", "warning"));
					break;
				case IMarker.SEVERITY_ERROR:
					object.put("severity", marker.getAttribute("severity", "error"));
					break;
			}
			object.put("start", marker.getAttribute("charStart", 0));
			object.put("end", marker.getAttribute("charEnd", 0));
			objects.put(object);
		}
		return objects;
	}
}