package com.anf.core.services.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import com.anf.core.services.WorkflowNotificationService;
import com.anf.core.workflow.EmailWorkflow;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.exec.WorkItem;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.day.cq.replication.ReplicationActionType;
import com.anf.core.utils.ConfigUtil;
import com.anf.core.utils.EmailUtils;
import com.anf.core.utils.PrettyPrintMap;
import com.anf.core.workflow.EmailWorkflow.EnvironmentType;

@Component(service = WorkflowNotificationService.class,
		   property = {Constants.SERVICE_DESCRIPTION + "= Workflow Notification Service.",
					Constants.SERVICE_VENDOR + "=adobe"})
public class WorkflowNotificationServiceImpl implements WorkflowNotificationService {
	
	private static final String EMAIL_FROM_CONFIG_PID = "com.day.cq.mailer.DefaultMailService";
	private static final String EMAIL_FROM_CONFIG_KEY = "from.address";
	private static final String DATE_FORMAT = "MMMM dd HH:mm:ss";

	private static final int NOTIFICATION_TYPE_PAGE_REQUEST = 1;
	private static final int NOTIFICATION_TYPE_ASSET_REQUEST = 2;
	private static final int NOTIFICATION_TYPE_PAGE_REJECTION = 3;
	private static final int NOTIFICATION_TYPE_ASSET_REJECTION = 4;
	private static final int NOTIFICATION_TYPE_PAGE_COMPLETE = 5;
	private static final int NOTIFICATION_TYPE_ASSET_COMPLETE = 6;
	
	@Reference
	private MessageGatewayService messageGatewayService;
	@Reference
	private ConfigurationAdmin configurationAdmin;
	@Reference
	private ResourceResolverFactory resolverFactory;
	
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	  
	


	/**
	 * sendPageReplicationRequest
	 * 
	 * sends an email notification that cq:PageContent is ready to be activated
	 * to the STAGE or LIVE server.
	 * 
	 * approver - could be a user or a group. 
	 * author - the person who initiated the workflow. they will be copied on the email 
	 * workItem - the current workflow WorkItem
	 * environmentType - stage or live
	 * 
	 */
	@Override
	public void sendPageReplicationRequest(WorkItem workItem, String approver, EnvironmentType environmentType, ReplicationActionType replicationType) throws Exception {
		sendNotification(NOTIFICATION_TYPE_PAGE_REQUEST, workItem, approver, replicationType, environmentType.getName());
	}
	


	/**
	 * sendPageReplicationComplete
	 * 
	 * sends an email notification that cq:PageContent has successfully been completed. 
	 * (Note: 'stage' or 'live' is irrelevant since 'completion' assumes live.)
	 * 
	 * workItem - the workflow work item. this is used to get the information from the email, like comments etc...
	 * approver - an AEM user or group 
	 * replicationType - ACTIVATE or DEACTIVATE
	 * previousComment - comment from the previous step in the workflow 
	 * (Note: have to use the previous step's comment because it is a Process step and will carry forward the comment from the Participant Step that completed the Activate)
	 * 
	 */	
	public void sendPageReplicationComplete(WorkItem workItem, String approver, ReplicationActionType replicationType, String previousComment)  throws Exception {
		sendNotification(NOTIFICATION_TYPE_PAGE_COMPLETE, workItem, approver, replicationType, previousComment);	
	}


	/**
	 * sendPageReplicationRejection
	 * 
	 * sends an email notification that cq:PageContent has been rejected, can be
	 * either from the Preview (i.e. Stage) server OR to Live.
	 * 
	 * workItem - the workflow work item. this is used to get the information for the email, like comments etc... 
	 * approver - an AEM user or group
	 * environmentType - stage or live
	 * replicationType - ACTIVATE or DEACTIVATE
	 * 
	 */
	@Override
	public void sendPageReplicationRejection(WorkItem workItem, String approver, EnvironmentType environmentType, ReplicationActionType replicationType)  throws Exception {
		sendNotification(NOTIFICATION_TYPE_PAGE_REJECTION, workItem, approver, replicationType, environmentType.getName());
	}

	

	/**
	 * sendAssetReplicationRequest
	 * 
	 * sends an email notification that dam:AssetContent is ready to be activated
	 * to the Preview (i.e. Stage) server OR to Live.
	 * 
	 * replicationType - ACTIVATE or DEACTIVATE
	 * 
	 */
	@Override
	public void sendAssetReplicationRequest(WorkItem workItem, String approver, EnvironmentType environmentType, ReplicationActionType replicationType)  throws Exception{
		sendNotification(NOTIFICATION_TYPE_ASSET_REQUEST, workItem, approver, replicationType, environmentType.getName());
	}

	/**
	 * sendAssetReplicationComplete
	 * 
	 * sends an email notification that dam:AssetContent replication has successfully been completed. 
	 * (Note: 'stage' or 'live' is irrelevant since 'completion' assumes live.)
	 * 
	 * workItem - the workflow work item. this is used to get the information from the email, like comments etc...
	 * approver - an AEM user or group 
	 * replicationType - ACTIVATE or DEACTIVATE
	 * previousComment - comment from the previous step in the workflow 
	 * (Note: have to use the previous step's comment because it is a Process step and will carry forward the comment from the Participant Step that completed the Activate)
	 * 
	 */
	 public void sendAssetReplicationComplete(WorkItem workItem, String approver, ReplicationActionType replicationType, String previousComment)  throws Exception{
		 sendNotification(NOTIFICATION_TYPE_ASSET_COMPLETE, workItem, approver, replicationType, previousComment);
	 }


	/**
	 * sendAssetReplicationRejection
	 * 
	 * sends an email notification that dam:AssetContent replication has been rejected, can be
	 * either from the Preview (i.e. Stage) server OR to Live.
	 * 
	 * workItem - the workflow work item. this is used to get the information from the email, like comments etc... 
	 * approver - an AEM user or group
	 * environmentType - stage or live
	 * replicationType - ACTIVATE or DEACTIVATE
	 * 
	 */
	@Override
	 public void sendAssetReplicationRejection(WorkItem workItem, String approver, EnvironmentType environmentType, ReplicationActionType replicationType)  throws Exception {
		 sendNotification(NOTIFICATION_TYPE_ASSET_REJECTION, workItem, approver, replicationType, environmentType.getName());
	 }
		
	 protected void sendNotification(int notificationType, WorkItem workItem, String approver, ReplicationActionType replicationType, String additionInfo) throws Exception {
		ResourceResolver resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
		String initiator = workItem.getWorkflow().getInitiator();
		String path = workItem.getWorkflow().getWorkflowData().getPayload().toString();
		SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
		String envType = null;
		String time = null;
		String startComment = null;
		String workflowTitle = workItem.getWorkflow().getWorkflowData().getMetaDataMap().get("workflowTitle", String.class);
		// can't use the current workItem's comment.
		//String comment = workItem.getMetaDataMap().get("comment", String.class);		
		String comment = null;
		String emailTemplatePath = null;
		String emailSubject = null;
		
		switch(notificationType) {
			case NOTIFICATION_TYPE_PAGE_REQUEST: 
				emailTemplatePath = EmailUtils.REQUEST_PAGE_REPLICATION_EMAIL_TEMPLATE_PATH;
				emailSubject = EmailUtils.REQUEST_REPLICATION_EMAIL_SUBJECT;
				time = format.format(workItem.getTimeStarted());
				startComment = workItem.getWorkflow().getWorkflowData().getMetaDataMap().get("startComment", String.class);	
				envType = additionInfo;
				break;
			case NOTIFICATION_TYPE_ASSET_REQUEST: 
				emailTemplatePath = EmailUtils.REQUEST_ASSET_REPLICATION_EMAIL_TEMPLATE_PATH;
				emailSubject = EmailUtils.REQUEST_REPLICATION_EMAIL_SUBJECT;
				time = format.format(workItem.getTimeStarted());
				startComment = workItem.getWorkflow().getWorkflowData().getMetaDataMap().get("startComment", String.class);	
				envType = additionInfo;
				break;
			case NOTIFICATION_TYPE_PAGE_REJECTION: 
				emailTemplatePath = EmailUtils.REJECT_PAGE_REPLICATION_EMAIL_TEMPLATE_PATH;
				emailSubject = EmailUtils.REJECT_REPLICATION_EMAIL_SUBJECT;
				time = format.format(workItem.getTimeStarted());
				// the original title given by the initiator of the workflow.
				workflowTitle = workItem.getWorkflowData().getMetaDataMap().get("workflowTitle", String.class);
				// the comment explains why this was rejected? approver should give a comment.	
				comment = workItem.getMetaDataMap().get("comment", String.class);
				envType = additionInfo;
				break;
			case NOTIFICATION_TYPE_ASSET_REJECTION: 
				emailTemplatePath = EmailUtils.REJECT_ASSET_REPLICATION_EMAIL_TEMPLATE_PATH;
				emailSubject = EmailUtils.REJECT_REPLICATION_EMAIL_SUBJECT;
				time = format.format(workItem.getTimeStarted());
				// the original title given by the initiator of the workflow.
				workflowTitle = workItem.getWorkflowData().getMetaDataMap().get("workflowTitle", String.class);
				// the comment explains why this was rejected? approver should give a comment.	
				comment = workItem.getMetaDataMap().get("comment", String.class);
				envType = additionInfo;
				break;
			case NOTIFICATION_TYPE_PAGE_COMPLETE: 
				emailTemplatePath = EmailUtils.PAGE_REPLICATION_COMPLETE_EMAIL_TEMPLATE_PATH;
				emailSubject = EmailUtils.REPLICATION_COMPLETE_EMAIL_SUBJECT;
				// time the acitvation completed, not the time the workflow ended.
				time = format.format(new Date());
				comment = additionInfo;
				break;
			case NOTIFICATION_TYPE_ASSET_COMPLETE: 
				emailTemplatePath = EmailUtils.ASSET_REPLICATION_COMPLETE_EMAIL_TEMPLATE_PATH;
				emailSubject = EmailUtils.REPLICATION_COMPLETE_EMAIL_SUBJECT;
				// time the acitvation completed, not the time the workflow ended.
				time = format.format(new Date());
				comment = additionInfo;
				break;
			default: 
				break;
		}
		// Ignore the undefined notification type and return
		if(emailTemplatePath == null) {
			return;
		}

		String replicationTypeDisplayString = "";
		
		switch (replicationType) {
		case ACTIVATE :
			replicationTypeDisplayString = "Activation";
			break;
		case DEACTIVATE :
			replicationTypeDisplayString = "DE-Activation";
			break;
		default:
			throw new Exception("aemtrpsys EXCEPTION, bad process args");
		}
		
		try {
		UserManager userManager = AccessControlUtil.getUserManager(resourceResolver.adaptTo(Session.class));
		
		Set<String> toList = EmailUtils.getEmailAddress(resourceResolver, userManager, approver);		
		if(toList.isEmpty()){
			throw new Exception("aemtrpsys EXCEPTION, workflow approver = " + approver + ", no email address(es) in profile");
		}	
		

		Set<String> ccList = EmailUtils.getEmailAddress(resourceResolver, userManager, initiator);
		if(ccList.isEmpty()){
			throw new Exception("aemtrpsys EXCEPTION, workflow initiator = " + initiator + ", no email address(es) in profile");
		}
		
		///////////////////////////////////////////////////////////////////////////////////////
		// build out the email message ...
		String urlDomain = ConfigUtil.getDomainUrl(configurationAdmin);
		// we assume that a page will always have a .html extension.
		// String pageUrl = urlDomain + "/cf#" + path + ".html";
		
		// get last part of the path e.g. /content/geometrixx/en returns en.html
		String pageUrl = path;
		int pathLastSlashIndex = path.lastIndexOf("/");
		if(pathLastSlashIndex >= 0) {
			pageUrl = pageUrl.substring(pathLastSlashIndex + 1);
		}
		
		// en returns en.html and 1.jpg returns 1.jpg
		if(!pageUrl.contains(".")) {
			pageUrl = pageUrl + ".html";
		}
		String inboxUrl = urlDomain + "";
		
		// get first name and last name
		Authorizable authorizable = userManager.getAuthorizable(initiator);
		String profilePath = authorizable.getPath() + "/profile";
		Resource profileResource = resourceResolver.getResource(profilePath);
		Node profileNode = profileResource.adaptTo(Node.class);
		String firstName = "";
		String lastName = "";
		if(profileNode.hasProperty("givenName")) {
			firstName = profileNode.getProperty("givenName").getString();
		}
		if(profileNode.hasProperty("familyName")) {
			lastName = profileNode.getProperty("familyName").getString();
		}
		
		// Email Subject
		emailSubject = EmailUtils.replaceEmailValue(emailSubject, "($workflow_title)", workflowTitle);
		emailSubject = EmailUtils.replaceEmailValue(emailSubject, "($replication_type)", replicationTypeDisplayString);
		
		// Email Message
		Resource templateResource = resourceResolver.getResource(emailTemplatePath);
		String emailMessage = EmailUtils.getEmailTemplate(templateResource);
		emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($author_name)", initiator);
		emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($first_name)", firstName);
		emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($last_name)", lastName);
		emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($content_path)", pageUrl);
		emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($domain_url)", inboxUrl);
		if(envType != null) {
			emailSubject = EmailUtils.replaceEmailValue(emailSubject, "($env_type)", envType);
			emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($env_type)", envType);
		}
		if(startComment != null) {
			emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($start_comment)", startComment);
		}

		emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($replication_type)", replicationTypeDisplayString);
		emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($time)", time);
		emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($workflow_title)", workflowTitle);		
		emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($comment)", comment);
		
		sendEmail(toList, ccList, emailSubject, emailMessage);
		
		}
		finally{
			
			// alwasy close the jcr session.
			resourceResolver.close();
			
		}
	}

		/**
		 * Used to test basic email channel.
		 */

		public void testBasicService(String address){
			
			log.info("aemtrpsys = email, sending test email to {}", address);
			
			try {

			// Declare a MessageGateway service
			MessageGateway<Email> messageGateway;

			// Set up email message
			Email email = new SimpleEmail();
			email.addTo(address);
			email.setSubject("AEM test email");
			email.setFrom(ConfigUtil.getConfiguration(configurationAdmin, EMAIL_FROM_CONFIG_PID, EMAIL_FROM_CONFIG_KEY) );
			email.setMsg("Test email from AEM.");

			// send email
			messageGateway = messageGatewayService.getGateway(Email.class);
			messageGateway.send((Email) email);
			
			}
			catch (Throwable th) {			
				log.error("aemtrpsys = email, Could not send test email, ", th);
			}		
			
		}
		
		
		/**
		 * Used to test email that takes data from the workflow item. sometimes getting data from a workflow item to
		 * populate email information can cause issues. 
		 */
		public void testAdvancedService(WorkItem workItem, String address)  throws Exception {

			ResourceResolver resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
			
			String initiator = workItem.getWorkflow().getInitiator();
			String path = workItem.getWorkflow().getWorkflowData().getPayload().toString();
			SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
			String timeStarted = format.format(workItem.getTimeStarted());
			String workflowName = "";
			//String workflowName = workItem.getWorkflow().getWorkflowModel().getTitle();
			String comment = (String)workItem.getMetaDataMap().get("comment");
			
			StringBuffer emailMessage = new StringBuffer();

			emailMessage.append("initiator = " + initiator);
			emailMessage.append("\n");
			emailMessage.append("path = " + path);
			emailMessage.append("\n");
			emailMessage.append("timeStarted = " + timeStarted);
			emailMessage.append("\n");
			emailMessage.append("workflowName = " + workflowName);
			emailMessage.append("\n");
			emailMessage.append("comment = " + comment);
			emailMessage.append("\n");
			
			// Dump all the metadata maps

			//workFlow
			Map<String, Object> workFlowMap = (Map<String, Object>)workItem.getWorkflow().getMetaDataMap();
			PrettyPrintMap<String, Object> workFlowPrettyPringMap = new PrettyPrintMap<String, Object>(workFlowMap);
			emailMessage.append("Workflow metaDataMap = " + workFlowPrettyPringMap.toString());

			emailMessage.append("\n");

			//workItem
			Map<String, Object> workItemMap = (Map<String, Object>)workItem.getMetaDataMap();
			PrettyPrintMap<String, Object> workItemPrettyPringMap = new PrettyPrintMap<String, Object>(workItemMap);
			emailMessage.append("Workflow Item metaDataMap = " + workItemPrettyPringMap.toString());

			emailMessage.append("\n");
			//workFlowData
			Map<String, Object> workDataMap = (Map<String, Object>)workItem.getWorkflowData().getMetaDataMap();
			PrettyPrintMap<String, Object> workDataPrettyPringMap = new PrettyPrintMap<String, Object>(workDataMap);
			emailMessage.append("Workflow Data metaDataMap = " + workDataPrettyPringMap.toString());
			
			emailMessage.append("\n");
			
			String workflowTitle = workItem.getWorkflow().getWorkflowData().getMetaDataMap().get("workflowTitle", String.class);

			emailMessage.append("workflowTitle (using get method) = " + workflowTitle);
			

			emailMessage.append("\n");
			
			String startComment = workItem.getWorkflow().getWorkflowData().getMetaDataMap().get("startComment", String.class);

			emailMessage.append("startComment (using get method) = " + startComment);

			try {

			// Declare a MessageGateway service
			MessageGateway<Email> messageGateway;

			// Set up email message
			Email email = new SimpleEmail();
			email.addTo(address);
			email.setSubject("AEM Advance test email");
			email.setFrom(ConfigUtil.getConfiguration(configurationAdmin, EMAIL_FROM_CONFIG_PID, EMAIL_FROM_CONFIG_KEY) );
			email.setMsg(emailMessage.toString());

			// send email
			messageGateway = messageGatewayService.getGateway(Email.class);
			messageGateway.send((Email) email);
			
			}
			catch (Throwable th) {			
				log.error("aemtrpsys = email, Could not send test email, ", th);
			}
			finally{
				
				// alwasy close the jcr session.
				resourceResolver.close();
				
			}
			
			
		}

	protected void sendEmail(Set<String> toList, Set<String> ccList, String emailSubject, String emailMessage) throws Exception {
			
			MessageGateway<Email> messageGateway;
			Email email = new SimpleEmail();		
			for(String toAddress : toList) {
				email.addTo(toAddress);
			}		
			for(String ccAddress : ccList) {
				email.addCc(ccAddress);
			}
			email.setSubject(emailSubject);
			email.setFrom(ConfigUtil.getConfiguration(configurationAdmin, EMAIL_FROM_CONFIG_PID, EMAIL_FROM_CONFIG_KEY) );
			email.setMsg(emailMessage);
	
			// send email
			messageGateway = messageGatewayService.getGateway(Email.class);
			messageGateway.send((Email) email);
		
		}
	
}