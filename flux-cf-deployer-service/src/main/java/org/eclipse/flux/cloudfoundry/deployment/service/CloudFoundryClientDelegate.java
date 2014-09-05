package org.eclipse.flux.cloudfoundry.deployment.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.json.JSONObject;

/**
 * Defines API for Cloud Foundry operations, like pushing an application.
 */
public class CloudFoundryClientDelegate {

	private String cfUser;
	private URL cloudControllerUrl;
	private String password;
	private String orgSpace; // org + "/" + space

	private CloudFoundryClient client;
	private String[] spaces;

	private final MessageConnector connector;

	private final Map<String, StreamingLogToken> activeApplicationLogs = new HashMap<String, StreamingLogToken>();

	public CloudFoundryClientDelegate(String cfUser, String password,
			URL cloudControllerUrl, String space, MessageConnector connector) {
		this.cfUser = cfUser;
		this.password = password;
		this.cloudControllerUrl = cloudControllerUrl;
		this.client = createClient(cfUser, password, cloudControllerUrl, space);
		this.connector = connector;
	}

	private CloudFoundryClient createClient(String cfUser, String password,
			URL cloudControllerUrl, String orgSpace) {
		if (orgSpace != null) {
			String[] pieces = getOrgSpace(orgSpace);
			String org = pieces[0];
			String space = pieces[1];
			return new CloudFoundryClient(
					new CloudCredentials(cfUser, password), cloudControllerUrl,
					org, space);
		} else {
			return new CloudFoundryClient(
					new CloudCredentials(cfUser, password), cloudControllerUrl);
		}
	}

	private String[] getOrgSpace(String orgSpace) {
		return orgSpace.split("/");
	}

	public void push(String appName, File location) {
		CloudFoundryClient client = this.client;
		final CloudFoundryApplication localApp = new CloudFoundryApplication(
				appName, location, client);

		String deploymentName = localApp.getName();

		new ApplicationOperation<Void>(deploymentName) {
			@Override
			protected Void doRun(CloudFoundryClient client) {
				// Check whether it exists. if so, stop it first, otherwise
				// create it
				CloudApplication existingApp = null;

				List<CloudApplication> applications = client.getApplications();

				if (applications != null) {
					for (CloudApplication deployedApp : applications) {
						if (deployedApp.getName().equals(appName)) {
							existingApp = deployedApp;
							break;
						}
					}
				}
				if (existingApp == null) {
					client.createApplication(appName, new Staging(null,
							localApp.getBuildpack()), localApp.getMemory(),
							localApp.getUrls(), localApp.getServices());
				} else {
					stopApplication(existingApp);
				}

				doUploadStart(localApp);
				return null;
			}

		}.run(client);
	}

	protected void doUploadStart(CloudFoundryApplication localApp) {
		final File location = localApp.getLocation();
		new ApplicationOperation<Void>(localApp.getName()) {

			protected Void doRun(CloudFoundryClient client) {
				try {
					addLogListener(appName);
					client.uploadApplication(appName, location);
					client.startApplication(appName);
				} catch (IOException e) {
					handleMessage(e, null, appName);
				}
				return null;
			}

			protected void onError(Throwable t) {
				removeLogListener(appName);
			}

		}.run(this.client);
	}
	
	protected void addLogListener(String appName) {
		if (appName != null && !activeApplicationLogs.containsKey(appName)) {
			StreamingLogToken logToken = this.client.streamLogs(appName,
					new DeployedApplicationLogListener(appName));
			if (logToken != null) {
				activeApplicationLogs.put(appName, logToken);
			}
		}
	}

	protected void removeLogListener(String appName) {
		StreamingLogToken token = activeApplicationLogs.remove(appName);
		if (token != null) {
			token.cancel();
		}
	}

	protected void stopApplication(CloudApplication app) {
		if (app != null && app.getState() != AppState.STOPPED) {
			client.stopApplication(app.getName());
		}
	}

	public String getSpace() {
		return orgSpace;
	}

	public synchronized void setSpace(String space) {
		try {
			if (equal(this.orgSpace, space) && client != null) {
				return;
			}
			this.orgSpace = space;
			client = createClient(cfUser, password, cloudControllerUrl, space);
		} catch (Throwable e) {
			// something went wrong, if we still have a client, its pointing at
			// the wrong space. So...
			// get rid of that client.
			handleMessage(e, null, null);
			client = null;
		}
	}

	private boolean equal(String s1, String s2) {
		if (s1 == null) {
			return s1 == s2;
		} else {
			return s1.equals(s2);
		}
	}

	public synchronized String[] getSpaces() {
		// We cache this. Assume it really never changes (or at least very
		// rarely).
		if (this.spaces == null) {
			this.spaces = fetchSpaces();
		}
		return this.spaces;
	}

	private String[] fetchSpaces() {
		List<CloudSpace> spaces = client.getSpaces();
		if (spaces != null) {
			String[] array = new String[spaces.size()];
			for (int i = 0; i < array.length; i++) {
				CloudSpace space = spaces.get(i);
				array[i] = space.getOrganization().getName() + "/"
						+ space.getName();
			}
			return array;
		}
		return new String[0];
	}

	protected void handleMessage(String message, String streamType,
			String appName) {
		try {
			if (connector == null) {
				throw new Error(message);
			}
			JSONObject json = new JSONObject();
			json.put(MessageConstants.USERNAME, this.cfUser);
			json.put(MessageConstants.CF_APP, appName);
	
			if (this.orgSpace != null) {
				String[] pieces = getOrgSpace(this.orgSpace);
				String org = pieces[0];
				String space = pieces[1];
				json.put(MessageConstants.CF_ORG, org);
				json.put(MessageConstants.CF_ORG_SPACE, space);
			}
			json.put(MessageConstants.CF_MESSAGE, message);
			json.put(MessageConstants.CF_STREAM, streamType);
	
			connector.send(MessageConstants.CF_APP_LOG, json);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	protected void handleMessage(Throwable error, String message, String appName) {
		if (message == null) {
			message = "Cloud Foundry Deployment Service Error";
		}
		if (error != null) {
			message += " - " + error.getMessage() + '\n';
		}
		handleMessage(message, MessageConstants.CF_STREAM_CLIENT_ERROR, appName);
	}

	class DeployedApplicationLogListener implements ApplicationLogListener {

		private final String appName;

		public DeployedApplicationLogListener(String appName) {
			this.appName = appName;
		}

		public void onComplete() {
			// Nothing for now
		}

		public void onError(Throwable error) {
			handleMessage(error, null, appName);
		}

		public void onMessage(ApplicationLog log) {
			if (log != null) {
				org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType type = log
						.getMessageType();
				String streamType = null;
				if (type != null) {
					switch (type) {
					case STDERR:
						streamType = MessageConstants.CF_STREAM_STDERROR;
						break;
					case STDOUT:
						streamType = MessageConstants.CF_STREAM_STDOUT;
						break;
					}
				}
				handleMessage(log.getMessage(), streamType, appName);
			}
		}

	}

	abstract class ApplicationOperation<T> {

		protected final String appName;

		public ApplicationOperation(String appName) {
			this.appName = appName;
		}

		public T run(CloudFoundryClient client) {
			try {
				return doRun(client);
			} catch (Throwable t) {
				onError(t);
				handleMessage(t, null, appName);
			}
			return null;
		}

		abstract protected T doRun(CloudFoundryClient client);

		protected void onError(Throwable t) {

		};
	}

}
