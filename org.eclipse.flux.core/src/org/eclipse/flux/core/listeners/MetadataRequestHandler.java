package org.eclipse.flux.core.listeners;

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
import org.json.JSONObject;

import com.google.inject.Singleton;

@Singleton
@FluxMessageTypes({FluxMessageType.GET_METADATA_REQUEST})
public class MetadataRequestHandler implements FluxMessageHandler {

	@Override
	public void onMessage(FluxMessage message, Repository repository) throws Exception {
		JSONObject content = message.content();
		String projectName = message.content().getString("project");
		String resourcePath = message.content().getString("resource");
		Project project = repository.getProject(projectName);
		if(project != null){
			Resource resource = project.getResource(resourcePath);
			if(resource != null){
				Path path = new Path(MessageFormat.format("{0}/{1}", projectName, resourcePath));
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				content.put("type", "marker");
				content.put("metadata", new JSONArray(toJSON(file.findMarkers(null, true, IResource.DEPTH_INFINITE))));
				message.source().sendMessage(new FluxMessage(FluxMessageType.GET_METADATA_RESPONSE, content));
			}
		}
	}
	
	private String toJSON(IMarker[] markers) {
		StringBuilder result = new StringBuilder();
		boolean flag = false;
		result.append("[");
		for (IMarker m : markers) {
			if (flag) {
				result.append(",");
			}

			result.append("{");
			result.append("\"description\":" + JSONObject.quote(m.getAttribute("message", "")));
			result.append(",\"line\":" + m.getAttribute("lineNumber", 0));
			result.append(",\"severity\":\"" + (m.getAttribute("severity", IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_ERROR ? "error" : "warning")
					+ "\"");
			result.append(",\"start\":" + m.getAttribute("charStart", 0));
			result.append(",\"end\":" + m.getAttribute("charEnd", 0));
			result.append("}");

			flag = true;
		}
		result.append("]");
		return result.toString();
	}
}