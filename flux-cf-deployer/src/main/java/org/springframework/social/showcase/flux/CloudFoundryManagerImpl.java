package org.springframework.social.showcase.flux;

import java.security.Principal;

/**
 * Maps user to objects that keep track of the state of the app
 * for each users. This probably totally the wrong way of doing things.
 * 
 * @author Kris De Volder
 */
public class CloudFoundryManagerImpl implements CloudFoundryManager {

	public CloudFoundry getConnection(Principal currentUser) {
		return null;
	}

	@Override
	public void putConnection(Principal currentUser, CloudFoundry cloudFoundry) {
		throw new Error("putConnection not implemented yet");
	}

}
