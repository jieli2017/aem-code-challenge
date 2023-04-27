package com.anf.core.utils;

import java.io.IOException;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigUtil {
	
	

	public static final String DOMAIN_URL_CONFIG_KEY = "domain.url";

	public static final String AEM_TRP_SYS_CONFIG_PID = "com.trp.aem.sys";
	

	public static String getDomainUrl(ConfigurationAdmin ca) throws IOException {
		return getConfiguration(ca, AEM_TRP_SYS_CONFIG_PID, DOMAIN_URL_CONFIG_KEY);
	}
	
	
	public static String getConfiguration(ConfigurationAdmin ca, String pid, String key) throws IOException {
		String configValue = "";
		Configuration conf = ca.getConfiguration(pid);
		if (conf != null && conf.getProperties() != null) {
			configValue = (String) conf.getProperties().get(key);
		}
		return configValue;
	}
}
