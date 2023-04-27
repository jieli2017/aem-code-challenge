package com.anf.core.utils;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.WorkItem;

public class EmailUtils {
	
	protected static final Logger log = LoggerFactory.getLogger(EmailUtils.class);
	
    
    ////////////////////////////////////////////////////////////////////////////
    // Email Subject Lines
    ///////////////////////////////////////////////////////////////////////
	
	//Requests
    public static final String REQUEST_REPLICATION_EMAIL_SUBJECT = "AEM ($env_type) ($replication_type) Requested – ($workflow_title)";
    
    //Completions
    public static final String REPLICATION_COMPLETE_EMAIL_SUBJECT = "AEM ($replication_type) COMPLETE – ($workflow_title)";
    
	//Rejections
    public static final String REJECT_REPLICATION_EMAIL_SUBJECT = "AEM ($env_type) ($replication_type) REJECTED – ($workflow_title)";
    
    

    ////////////////////////////////////////////////////////////////////////////
    // Paths to Email Templates
    ///////////////////////////////////////////////////////////////////////
    
    
    // Pages
    public static final String REQUEST_PAGE_REPLICATION_EMAIL_TEMPLATE_PATH = "/etc/templates/aem-trp-sys/request-page-replication-email-template.txt";   
    public static final String PAGE_REPLICATION_COMPLETE_EMAIL_TEMPLATE_PATH = "/etc/templates/aem-trp-sys/page-replication-complete-email-template.txt"; 
    public static final String REJECT_PAGE_REPLICATION_EMAIL_TEMPLATE_PATH = "/etc/templates/aem-trp-sys/reject-page-replication-email-template.txt";
        
    // Assets
    public static final String REQUEST_ASSET_REPLICATION_EMAIL_TEMPLATE_PATH = "/etc/templates/aem-trp-sys/request-asset-replication-email-template.txt";
    public static final String ASSET_REPLICATION_COMPLETE_EMAIL_TEMPLATE_PATH = "/etc/templates/aem-trp-sys/asset-replication-complete-email-template.txt";
    public static final String REJECT_ASSET_REPLICATION_EMAIL_TEMPLATE_PATH = "/etc/templates/aem-trp-sys/reject-asset-replication-email-template.txt";
    
//    public static final String REQUEST_DEACTIVATE_PREVIEW_EMAIL_SUBJECT = "AEM Content Preview De-Activaion Requested – ($content_path)";
    
    public static String getEmailTemplate(final Resource resource) throws Exception {
        if (resource != null) {
            try {
                InputStream inputStream = resource.adaptTo(InputStream.class);
                String templateString = IOUtils.toString(inputStream, "UTF-8");
                if (StringUtils.isNotEmpty(templateString)) {
                    return templateString;
                }
            } catch (Exception e) {
        		throw new Exception("aemtrpsys EXCEPTION, can't read email template", e);
            }
        } else {
    		throw new Exception("aemtrpsys EXCEPTION, can't read email template");
        }
        return ""; // we can return default email template
    }

   
    public static String replaceEmailValue(final String emailMessage, final String originalString, String replacementString) {
    	// sometimes the replacementString can be null or blank. standardize the return to be " "
    	if(StringUtils.isEmpty(replacementString)){
    		replacementString = " ";
    	}
    	return StringUtils.replace(emailMessage, originalString, replacementString);
    }




	public static Set<String> getEmailAddress(ResourceResolver resourceResolver, UserManager userManager, String authId) throws Exception {
		
	
		// remove any white space 
		authId = StringUtils.trimToEmpty(authId);
		log.info("aemtrpsys getEmailAddress for {}", authId);
		Set<String> userEmailSet = new HashSet<String>();
        Authorizable authorizable = userManager.getAuthorizable(authId);
        

		log.info("aemtrpsys getEmailAddress, {} is Group ? = {}", authId, authorizable.isGroup());
        
        
        if(authorizable.isGroup()) {
        	Group group = (Group) authorizable;
        	Iterator<Authorizable> members = group.getMembers();
        	while(members.hasNext()) {
        		Authorizable next = members.next();
        		log.info("aemtrpsys getEmailAddress, {} is Group ? = {}", next.getID(), next.isGroup());
        		if(next.isGroup()) {
            		log.info("aemtrpsys getEmailAddress, ***recursion");
        			Set<String> addresses = getEmailAddress(resourceResolver, userManager, next.getID());  
            		log.info("aemtrpsys getEmailAddress, adding these strings to userEmailSet : {}", addresses.toArray(new String[]{}));
        			userEmailSet.addAll(addresses);
        		} else {        			
        			User user = (User)next;
        			userEmailSet.add(getEmailAddress(resourceResolver, user));
        		}
        	}
        }
        else {
        	userEmailSet.add(getEmailAddress(resourceResolver, authorizable));
        }
		return userEmailSet;
	}
	
	private static String getEmailAddress(ResourceResolver resourceResolver, Authorizable auth) throws Exception {
		String userEmail = "";
		
		String profilePath = auth.getPath() + "/profile";
		log.info("aemtrpsys email address retrieved for JCR profile, profilePath = {}", profilePath);
		Resource profileResource = resourceResolver.getResource(profilePath);
		Node profileNode = profileResource.adaptTo(Node.class);
		if(profileNode.hasProperty("email")) {
			userEmail = profileNode.getProperty("email").getString();
		} else {
			log.warn("aemtrpsys no email address set for JCR profile, profilePath = {}", profilePath);
		}

		return userEmail;
	}


	public static String getVersionLabel(Node pageNode) throws Exception, RepositoryException {
		String versionLabel = "";
		
		VersionHistory versionHistory = pageNode.getVersionHistory(); 
		VersionIterator vit = versionHistory.getAllVersions();
		while (vit.hasNext()){
			Version v = vit.nextVersion();
			versionLabel = v.getName().equalsIgnoreCase("jcr:rootVersion")?"1.0":v.getName();
		}
		
		return versionLabel;
	}

}
