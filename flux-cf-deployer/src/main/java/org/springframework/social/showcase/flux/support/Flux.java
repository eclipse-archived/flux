package org.springframework.social.showcase.flux.support;

import java.util.List;

import org.springframework.social.github.api.GitHubUserProfile;

public interface Flux {

	GitHubUserProfile getUserProfile();

	String getAccessToken();

	List<String> getProjects() throws Exception;
	
}
