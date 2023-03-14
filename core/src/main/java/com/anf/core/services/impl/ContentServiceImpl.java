package com.anf.core.services.impl;

import java.time.Instant;

import javax.jcr.Node;
import javax.jcr.Session;

import com.adobe.aemds.guide.utils.JcrResourceConstants;
import com.anf.core.services.ContentService;
import com.day.cq.commons.jcr.JcrUtil;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = ContentService.class)
public class ContentServiceImpl implements ContentService {

    /***Begin Code - Jie Li ***/

    private static final Logger LOG = LoggerFactory.getLogger(ContentServiceImpl.class);;

    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String PN_MAX_AGE = "maxAge";
    private static final String PN_MIN_AGE = "minAge";
    private static final String PATH_AGE_CONFIG = "/etc/age";
    private static final String PATH_VAR_ANF = "/var/anf-code-challenge";
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_LAST_NAME = "lastName";
    private static final String FIELD_AGE = "age";
    private static final String FORM_DATA_ERROR = "form data error.";
    private static final String FORM_AGE_ERROR = "You are not eligible";
    private static final String FORM_FIELD_REQUIRED_ERROR = "%s is required.";

    private Integer maxAge;
    private Integer minAge;

    @Override
    public JsonObject commitUserDetails(SlingHttpServletRequest req, JsonObject formJsonObject) {
        // Add your logic. Modify method signature as per need.
        if (formJsonObject == null) {
            return generateJson(false, FORM_DATA_ERROR);
        }
        this.getAgeConfig(req);
        JsonObject resultObject = validateFields(formJsonObject);
        if (!resultObject.get(KEY_SUCCESS).getAsBoolean()) {
            return resultObject;
        }
        return saveFormData(req, formJsonObject);
    }

    private JsonObject saveFormData(SlingHttpServletRequest req, JsonObject formJsonObject) {
        try {
            Session session = req.getResourceResolver().adaptTo(Session.class);
            if (session != null) {
                Node anfNode = JcrUtil.createPath(PATH_VAR_ANF, JcrResourceConstants.NT_SLING_FOLDER, session);
                if (anfNode != null) {
                    Node dataNode = JcrUtil.createUniqueNode(anfNode, String.valueOf(Instant.now().getEpochSecond()),
                      JcrResourceConstants.NT_SLING_FOLDER, session);
                    if (dataNode != null) {
                        JcrUtil.setProperty(dataNode, FIELD_FIRST_NAME, formJsonObject.get(FIELD_FIRST_NAME).getAsString());
                        JcrUtil.setProperty(dataNode, FIELD_LAST_NAME, formJsonObject.get(FIELD_LAST_NAME).getAsString());
                        JcrUtil.setProperty(dataNode, FIELD_AGE, formJsonObject.get(FIELD_AGE).getAsString());
                        session.save();
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to save form data.", e);
            return generateJson(false, "Failed to save form data.");
        }
        return generateJson(true, StringUtils.EMPTY);
    }

    private JsonObject validateFields(JsonObject formJsonObject) {
        if (formJsonObject.has(FIELD_FIRST_NAME)) {
            String firstName = formJsonObject.get(FIELD_FIRST_NAME).getAsString();
            if (StringUtils.isBlank(firstName)) {
                return generateJson(false, String.format(FORM_FIELD_REQUIRED_ERROR, FIELD_FIRST_NAME));
            }
        } else {
            return generateJson(false, String.format(FORM_FIELD_REQUIRED_ERROR, FIELD_FIRST_NAME));
        }
        if (formJsonObject.has(FIELD_LAST_NAME)) {
            String lastName = formJsonObject.get(FIELD_LAST_NAME).getAsString();
            if (StringUtils.isBlank(lastName)) {
                return generateJson(false, String.format(FORM_FIELD_REQUIRED_ERROR, FIELD_LAST_NAME));
            }
        } else {
            return generateJson(false, String.format(FORM_FIELD_REQUIRED_ERROR, FIELD_LAST_NAME));
        }

        if (formJsonObject.has(FIELD_AGE)) {
            String ageString = formJsonObject.get(FIELD_AGE).getAsString();
            if (StringUtils.isBlank(ageString)) {
                return generateJson(false, String.format(FORM_FIELD_REQUIRED_ERROR, FIELD_AGE));
            }
            int age =  Integer.valueOf(ageString);
            if (age < this.minAge || age > this.maxAge) {
                return generateJson(false, FORM_AGE_ERROR);
            }
        } else {
            return generateJson(false, String.format(FORM_FIELD_REQUIRED_ERROR, FIELD_AGE));
        }
        return generateJson(true, StringUtils.EMPTY);
    }

    private void getAgeConfig(SlingHttpServletRequest req) {
        if (maxAge == null && minAge == null) {
            ResourceResolver resourceResolver = req.getResourceResolver();
            if (resourceResolver != null) {
                Resource ageResource = resourceResolver.getResource(PATH_AGE_CONFIG);
                if (ageResource != null) {
                    this.maxAge = ageResource.getValueMap().get(PN_MAX_AGE, Integer.class);
                    this.minAge = ageResource.getValueMap().get(PN_MIN_AGE, Integer.class);
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
