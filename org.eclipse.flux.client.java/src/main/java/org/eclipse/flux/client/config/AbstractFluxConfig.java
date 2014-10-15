package org.eclipse.flux.client.config;

public abstract class AbstractFluxConfig implements FluxConfig {

	private String user;
	
	public AbstractFluxConfig(String user) {
		this.user = user;
	}
	
	@Override
	public String getUser() {
		return user;
	}
	
	@Override
	public String toString() {
		return getClass().getName()+"("+user+")";
	}
	
}
