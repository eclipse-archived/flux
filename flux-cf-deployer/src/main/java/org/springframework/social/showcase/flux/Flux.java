package org.springframework.social.showcase.flux;

import org.springframework.social.github.api.GitHubUserProfile;

public interface Flux {

	GitHubUserProfile getUserProfile();

	String getAccessToken();
	
}
