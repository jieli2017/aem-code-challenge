package com.anf.core.services.impl;

import com.anf.core.services.ContentService;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = ContentService.class)
public class ContentServiceImpl implements ContentService {

    /***Begin Code - Jie Li ***/

    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String PN_MAX_AGE = "maxAge";
    private static final String PN_MIN_AGE = "minAge";
    private static final String PATH_AGE_CONFIG = "/etc/age";
    private static final String FIELD_AGE = "age";
    private static final String FORM_DATA_ERROR = "form data error.";
    private static final String FORM_AGE_ERROR = "You are not eligible";

    private Integer maxAge;
    private Integer minAge;

    @Override
    public JsonObject commitUserDetails(SlingHttpServletRequest req, JsonObject formJsonObject) {
        // Add your logic. Modify method signature as per need.
        if (formJsonObject == null || formJsonObject.isJsonNull()) {
            return generateJson(false, FORM_DATA_ERROR);
        }
        this.getAgeConfig(req);
        if (!validateAge(formJsonObject)) {
            return generateJson(false, FORM_AGE_ERROR);
        }
        return generateJson(true, StringUtils.EMPTY);
    }

    private boolean validateAge(JsonObject formJsonObject) {
        if (formJsonObject.has(FIELD_AGE)) {
            String ageString = formJsonObject.get(FIELD_AGE).toString();
            int age =  Integer.valueOf(ageString);
            if (age > this.minAge && age < this.maxAge) {
                return true;
            }
        }
        return false;
    }

    private void getAgeConfig(SlingHttpServletRequest req) {
        if (maxAge == null && minAge == null) {
            ResourceResolver resourceResolver = req.getResourceResolver();
            if (resourceResolver != null) {
                Resource ageResource = resourceResolver.getResource(PATH_AGE_CONFIG);
                if (ageResource != null) {
                    this.maxAge = ageResource.getValueMap().get(PN_MAX_AGE, Integer.class);
                    this.minAge = ageResource.getValueMap().get(PN_MIN_AGE, Integer.class);
                    //this.maxAge = Integer.valueOf(maxAge);
                    //this.minAge = Integer.valueOf(minAge);
                }
            }
        }
    }

    private JsonObject generateJson(boolean isSuccess, String message) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(KEY_SUCCESS, isSuccess);
        jsonObject.addProperty(KEY_MESSAGE, message);
        return jsonObject;
    }

    /***END Code*****/
}
