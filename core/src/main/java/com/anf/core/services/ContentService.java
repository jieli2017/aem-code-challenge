package com.anf.core.services;

import org.apache.sling.api.SlingHttpServletRequest;

import com.google.gson.JsonObject;

public interface ContentService {
	JsonObject commitUserDetails(SlingHttpServletRequest req, JsonObject formJsonObject);
}
