package com.anf.core.servlets;

import com.adobe.xfa.Int;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.eclipse.jetty.util.StringUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.management.ObjectName;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Component(service = Servlet.class, property = {Constants.SERVICE_DESCRIPTION + "= Update token displayText",
        "sling.servlet.paths=" + "/bin/updatenode", "sling.servlet.methods=" + HttpConstants.METHOD_GET
})
public class UpdateNodeServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateNodeServlet.class);

    private final String PREFIX = "default";
    private final String PARAM_UPDATENODE = "updatenode";
    private final String PARAM_TOKENJSON = "tokenjson";
    private final String PARAM_JSON = "json";
    private JSONObject tokenJson;
    private JSONObject JsonDisplayText;

    public JSONObject getJsonFromFile(ResourceResolver resolver, String filePath,  PrintWriter out) {
        JSONObject jsonObj = new JSONObject();
        try {
            Resource resource = resolver.getResource(filePath);
            Node jcnode = resource.adaptTo(Node.class).getNode("jcr:content");
            InputStream content = jcnode.getProperty("jcr:data").getBinary().getStream();
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader br = new BufferedReader(new
                    InputStreamReader(content, StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            jsonObj = new JSONObject(sb.toString());

        } catch (RepositoryException | JSONException | IOException |NullPointerException e) {
            LOGGER.error(filePath, e);
            out.println("path:"  + filePath + " " + e.getMessage());
            return jsonObj;
//            throw new RuntimeException(e);
        }

        return jsonObj;
    }

    public static Object getValue(JSONObject json, String key) throws JSONException {
        // get corresponding value as object according to multi depth jsonPath key in json object
        String[] keys = key.split("\\.");
        JSONObject obj = json;
        for (int i = 0; i < keys.length - 1; i++) {
            obj = obj.getJSONObject(keys[i]);
        }
        String lastKey = keys[keys.length - 1];
        if (obj.has(lastKey)) {
            Object value = obj.get(lastKey);
            return value;
        } else {
            return null;
        }
    }
    public void updateTokenItem(ResourceResolver resourceResolver, Resource resource, PrintWriter out) {
        Iterator<Resource> tokenItem = resource.listChildren();
        while (tokenItem.hasNext()) {
            Resource child = tokenItem.next();
            String childNodeName = child.getName();
            out.print(" Node[" + childNodeName + "]");
            ModifiableValueMap map = child.adaptTo(ModifiableValueMap.class);
            String tokenKey = map.get("token", String.class);
            String displayText = map.get("displaytext", String.class);

            try {
                // get tokey key from tokenJson file
                String tokenJSONKey;
                Object tokenJSONObject = getValue(tokenJson, tokenKey);
                if (tokenJSONObject instanceof String) {
                    String tokenValue = (String) tokenJSONObject;
                    tokenJSONKey = tokenValue.substring(2, tokenValue.length() - 2);
                    out.print(">>" + tokenJSONKey + ">>");
                    // get value by tokenkey from displayText Json file，test instanceof Type, update if differently
                    Object displayTextObject = getValue(JsonDisplayText, tokenJSONKey);
                    if (displayTextObject instanceof String) {
                        String displayTextValue = ((String) displayTextObject).trim();
                        // displayText is not Exist, or is not equal to new displayText String when both String is trimed.
                        if (StringUtil.isNotBlank(displayText) && displayTextValue.equals(displayText.trim())) {
                            out.println("[updated]");
                        } else {
                            map.put("displaytext", displayTextValue);
                            out.println(" update to => " + displayTextValue);
                            resourceResolver.commit();
                        }
                    } else if (displayTextObject instanceof Integer) {
                        Integer displayTextValue = (Integer) displayTextObject;
                        // displayText is not Exist, or is not equal as Integer to new displayText Integer.
                        if (StringUtil.isNotBlank(displayText) && Integer.parseInt(displayText) != displayTextValue ) {
                            out.print(" old integer ==> " + displayText);
                            map.put("displaytext", displayTextValue);
                            out.println(" update to integer ==> " + displayTextValue);
                            resourceResolver.commit();
                        } else {
                            out.println("[updated]");
                        }
                    } else if (displayTextObject instanceof Boolean) {
                        Boolean displayTextValue = (Boolean) displayTextObject;
                        // displayText is not Exist, or is not equal as Boolean to new displayText Boolean.
                        if ( StringUtil.isNotBlank(displayText) && Boolean.parseBoolean( displayText) != displayTextValue) {
                            out.print(" old boolean ==> " + displayText);
                            map.put("displaytext", displayTextValue);
                            out.println(" update to boolean ==> " + displayTextValue);
                            resourceResolver.commit();
                        } else {
                            out.println("[updated]");
                        }
                    } else {
                        out.println("===" + childNodeName + ">>>" + tokenKey + "<Error> corresponding displayText not found!");
                    }
                } else {
                    out.println("===" + childNodeName + ">>>" + tokenKey + "<Error> is not valid Key!");
                }
            } catch (JSONException e) {
                out.println("===" + childNodeName + ">>>" + tokenKey + " <Error> " + e.getMessage());
            } catch (PersistenceException e) {
                out.println("===" + childNodeName + ">>>" + tokenKey + " <Error> " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
//        super.doGet(request, response);
        String updateNodePath = request.getParameter(PARAM_UPDATENODE);
        String tokenJsonPath = request.getParameter(PARAM_TOKENJSON);
        String jsonPath = request.getParameter(PARAM_JSON);
        PrintWriter out = response.getWriter();
        if (StringUtil.isNotBlank(updateNodePath) && StringUtil.isNotBlank(tokenJsonPath) && StringUtil.isNotBlank(jsonPath)) {
            out.println("updateNode Path:" + updateNodePath);
            out.println("tokenJson Path:" + tokenJsonPath);
            out.println("json Path:" + jsonPath);
        } else {
            throw new ServletException("param not include:" + PARAM_UPDATENODE + ", " + PARAM_TOKENJSON + ", " + PARAM_JSON);
        }

        ResourceResolver resourceResolver = request.getResourceResolver();

        if (StringUtil.isNotBlank(tokenJsonPath)) {
            tokenJson = getJsonFromFile(resourceResolver, tokenJsonPath + "/jcr:content/renditions/original", out);
        }

        if (StringUtil.isNotBlank(jsonPath)) {
            JsonDisplayText = getJsonFromFile(resourceResolver, jsonPath + "/jcr:content/renditions/original", out);
        }

        if (StringUtil.isNotBlank(updateNodePath)) {
            Resource resource = resourceResolver.getResource(updateNodePath + "/jcr:content/root/");

            // get node contains tokenItems, eg: default/tokenItems
            Iterator<Resource> nodeItem = resource.listChildren();
            while (nodeItem.hasNext()) {
                Resource child = nodeItem.next();
                Iterator<Resource> tokenItemNode = child.listChildren();
                while(tokenItemNode.hasNext()) {
                    Resource nodeItemChild = tokenItemNode.next();
                    String nodeItemChildName = nodeItemChild.getName();
                    if (nodeItemChildName.equals("tokenItems")) {
                        out.println(" === === >>> >>> [" + child.getName() + "/" + nodeItemChildName + "]");
                        updateTokenItem(resourceResolver, nodeItemChild, out);
                    }
                }
            }

        }
    }
}
