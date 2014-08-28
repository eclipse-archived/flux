package org.springframework.social.showcase.flux;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;

public class CloudFoundry {

	private URL cloudControllerUrl;
	private CloudFoundryClient client;

	public CloudFoundry(String cloudControllerUrl) throws Exception {
		this.cloudControllerUrl = new URI(cloudControllerUrl).toURL();
	}

	public boolean login(String login, String password) {
		try {
			CloudCredentials credentials = new CloudCredentials(login, password);
			CloudFoundryClient client = new CloudFoundryClient(credentials, cloudControllerUrl);
			this.client = client;
			return true;
		} catch (Throwable e) {
			e.printStackTrace();
			this.client = null;
			return false;
		}
	}

}
