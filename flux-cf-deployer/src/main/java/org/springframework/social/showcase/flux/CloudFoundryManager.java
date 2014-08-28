package org.springframework.social.showcase.flux;

import java.security.Principal;

public interface CloudFoundryManager {

	CloudFoundry getConnection(Principal currentUser);
	void putConnection(Principal currentUser, CloudFoundry cloudFoundry);

}
