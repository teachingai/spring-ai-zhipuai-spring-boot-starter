package com.p6spy.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(P6SpyProperties.PREFIX)
public class P6SpyProperties {

	public static final String PREFIX = "spring.datasource.p6spy";
	
	/**
	 * Enable P6Spy.
	 */
	private boolean enabled = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
}