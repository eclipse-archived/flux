package org.springframework.social.showcase.flux;

import org.springframework.social.github.api.GitHubUserProfile;
import org.springframework.social.github.api.impl.GitHubTemplate;

/**
 * Trying to create a Flux connector similar to Github/Twitter etc connector in spring social.
 */
public class FluxImpl implements Flux {

	private GitHubTemplate github;
	private String accessToken;

	public FluxImpl(String accessToken) {
		this.accessToken = accessToken;
		this.github = new GitHubTemplate(accessToken);
	}

	@Override
	public GitHubUserProfile getUserProfile() {
		return github.userOperations().getUserProfile();
	}
	
	public String getAccessToken() {
		return accessToken;
	}

}
