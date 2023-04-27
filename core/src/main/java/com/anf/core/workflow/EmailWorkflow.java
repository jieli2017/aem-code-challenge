package com.anf.core.workflow;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import com.anf.core.services.WorkflowNotificationService;
import com.anf.core.utils.EmailUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.mailer.MessageGatewayService;
import com.day.cq.replication.ReplicationActionType;
import com.anf.core.services.WorkflowNotificationService;

/**
 * A Workflow represents the current state of an instance of a WorkflowModel. 
 * In terms of petri nets the state of a workflow instance can be 
 * considered as the collections of tokens (or WorkItems) that flow through the workflow instance.
 */
@Component(service = WorkflowProcess.class,
		property = {Constants.SERVICE_DESCRIPTION + "= Workflow Email process.",
				Constants.SERVICE_VENDOR + "=adobe"})
public class EmailWorkflow implements WorkflowProcess {
	
	
	public enum EnvironmentType {
		INVALID("invalid"), STAGE("stage"), LIVE("live");
	 
		private String name;
	 
		private EnvironmentType(String n) {
			name = n;
		}
	 
		public String getName() {
			return name;
		}
		
		public static EnvironmentType fromString(String name){
			if(name.equals(EnvironmentType.STAGE.getName())) return EnvironmentType.STAGE;
			if(name.equals(EnvironmentType.LIVE.getName())) return EnvironmentType.LIVE;
			return EnvironmentType.INVALID;
		}
	 
	}
	
	public enum ActionType {
		INVALID("invalid"), TEST("test"), TEST_ADVANCED("test-advanced"), ACTIVATE("activate"), DEACTIVATE("deactivate");
	 
		private String name;
	 
		private ActionType(String n) {
			name = n;
		}
	 
		public String getName() {
			return name;
		}
		
		public static ActionType fromString(String name){
			if(name.equals(ActionType.TEST.getName())) return ActionType.TEST;
			if(name.equals(ActionType.TEST_ADVANCED.getName())) return ActionType.TEST_ADVANCED;
			if(name.equals(ActionType.ACTIVATE.getName())) return ActionType.ACTIVATE;
			if(name.equals(ActionType.DEACTIVATE.getName())) return ActionType.DEACTIVATE;
			return ActionType.INVALID;
		}
	 
	}
	
	public enum EmailType {
		INVALID("invalid"), REQUEST("request"), COMPLETE("complete"), REJECT("reject");
	 
		private String name;
	 
		private EmailType(String n) {
			name = n;
		}
	 
		public String getName() {
			return name;
		}
		
		public static EmailType fromString(String name){
			if(name.equals(EmailType.REQUEST.getName())) return EmailType.REQUEST;
			if(name.equals(EmailType.COMPLETE.getName())) return EmailType.COMPLETE;
			if(name.equals(EmailType.REJECT.getName())) return EmailType.REJECT;
			return EmailType.INVALID;
		}
	 
	}
	
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private static final String EMAIL_FROM_CONFIG_PID = "com.day.cq.mailer.DefaultMailService";
	private static final String EMAIL_FROM_CONFIG_KEY = "from.address";
	
	// following constants are used in JSON configuration used for the proccess args.
	private static final String ACTION_TYPE_KEY = "action"; // test, activate or deactivate
	private static final String EMAIL_TYPE_KEY = "email"; // request, complete or reject
	private static final String ENVIRONMENT_TYPE_KEY = "environment"; // stage or live
	private static final String TEST_ADDRESS_KEY = "address"; // only used for testing.
	private static final String TEST_PRINCIPAL_KEY = "principal"; // only used for testing.
	private static final String APPROVER_KEY = "approver"; // the id for the approver user or group.
	

	public static final String TYPE_JCR_PATH = "JCR_PATH";
	public static final String TYPE_PAGE_CONTENT = "cq:PageContent";
	public static final String TYPE_DAM_ASSET_CONTENT = "dam:AssetContent";
	

	@Reference
	private ConfigurationAdmin ca;
	
	@Reference
	private MessageGatewayService messageGatewayService;
	
	@Reference
	private ResourceResolverFactory resolverFactory;
	
	@Reference
	private WorkflowNotificationService workflowNotificationService;

	public void execute(WorkItem item, WorkflowSession wfsession, MetaDataMap args) throws WorkflowException {
		
		ResourceResolver resourceResolver = null;
		
		try {
			
			resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
			
			String processArgs = args.get("PROCESS_ARGS", String.class);
			
			ActionType actionType = ActionType.fromString(getActionType(processArgs));
			log.info("aemtrpsys actionType = {}", actionType.getName());
			//
			EmailType emailType = EmailType.fromString(getEmailType(processArgs));
			log.info("aemtrpsys emailType = {}", emailType);
			//
			EnvironmentType environmentType = EnvironmentType.fromString(getEnvironmentType(processArgs));			
			log.info("aemtrpsys environmentType = {}", environmentType.getName()); 
			//
			String testAddress = getTestAddress(processArgs);			
			log.info("aemtrpsys testAddress = {}", testAddress);
			//
			String principal = getPrincipal(processArgs);			
			log.info("aemtrpsys principal = {}", principal);
			//
			String approver = getApprover(processArgs);
			log.info("aemtrpsys approver = {}", approver);
			//
			String initiator = item.getWorkflow().getInitiator();
			log.info("aemtrpsys = initiator = {}", initiator);
			
			
			
			////////////////////////////////////////////////////////////////////////////////////////////////////
			// We only deal two kinds of content for activation/deactivation Pages or Assets.
			// We can determine these from node types =   cq:Page or dam:AssetContent.
			//
			String nodeType = null;
			if (item.getWorkflowData().getPayloadType().equals(TYPE_JCR_PATH)) {
				
				String path = item.getWorkflow().getWorkflowData().getPayload().toString() + "/jcr:content";
				Resource resource = resourceResolver.getResource(path);
				Node node = resource.adaptTo(Node.class);

				try {
					// Session jcrSession = wfsession.adaptTo(Session.class);
					// Node node = (Node) jcrSession.getItem(tPath);
					if (node != null) {

						nodeType = node.getPrimaryNodeType().getName();

						log.info("aemtrpsys =  nodeType = {}", nodeType);

						// jcrSession.save();
					} else {

						throw new WorkflowException(
								"aemtrpsys UNSUPPORTED CONTENT TYPE");
					}
				} catch (Exception e) {
					throw new WorkflowException(e);
				}
			} else {

				throw new WorkflowException(
						"aemtrpsys UNSUPPORTED WORKFLOW PAYLOAD, type = "
								+ item.getWorkflowData().getPayloadType());
			}
			
			// processing logic starts here:
			
			switch (actionType) {
		
			case ACTIVATE:

				switch (emailType) {

				case REQUEST:
					if (nodeType.equals(TYPE_PAGE_CONTENT)) {
						sendPageReplicationRequest(item, approver, environmentType, ReplicationActionType.ACTIVATE);
						return;
					} else if(nodeType.equals(TYPE_DAM_ASSET_CONTENT)) {
						sendAssetReplicationRequest(item, approver, environmentType, ReplicationActionType.ACTIVATE);
						return;
						
					}
					throw new Exception("aemtrpsys EXCEPTION, bad process args");					
				case COMPLETE:
					
					// assumes the previous step in the workflow was activation.
					// we only need this for complete emails. For request and reject, we assume the Pariticpant step
					// is the previous step and so in those cases the comment is carried forward in the current work item.
					String comment = getPreviousComment(item, wfsession);
										
					if (nodeType.equals(TYPE_PAGE_CONTENT)) {
						sendPageReplicationComplete(item, approver, ReplicationActionType.ACTIVATE, comment);
						return;
					} else if(nodeType.equals(TYPE_DAM_ASSET_CONTENT)) {
						sendAssetReplicationComplete(item, approver, ReplicationActionType.ACTIVATE, comment);						
						return;
						
					}	
					throw new Exception("aemtrpsys EXCEPTION, bad process args");
				case REJECT:
					if (nodeType.equals(TYPE_PAGE_CONTENT)) {
						sendPageReplicationRejection(item, approver, environmentType, ReplicationActionType.ACTIVATE);
						return;
					} else if(nodeType.equals(TYPE_DAM_ASSET_CONTENT)) {
						sendAssetReplicationRejection(item, approver, environmentType, ReplicationActionType.ACTIVATE);						
						return;
						
					}				
					throw new Exception("aemtrpsys EXCEPTION, bad process args");	

				default:
					throw new Exception("aemtrpsys EXCEPTION, bad process args");

				}
				
			case DEACTIVATE:
				
				switch (emailType) {

				case REQUEST:
					if (nodeType.equals(TYPE_PAGE_CONTENT)) {
						sendPageReplicationRequest(item, approver, environmentType, ReplicationActionType.DEACTIVATE);
						return;
					} else if(nodeType.equals(TYPE_DAM_ASSET_CONTENT)) {
						sendAssetReplicationRequest(item, approver, environmentType, ReplicationActionType.DEACTIVATE);
						return;
						
					}
					throw new Exception("aemtrpsys EXCEPTION, bad process args");					
				case COMPLETE:
					
					// assumes the previous step in the workflow was deactivation.
					// we only need this for complete emails. For request and reject, we assume the Pariticpant step
					// is the previous step and so in those cases the comment is carried forward in the current work item.
					String comment = getPreviousComment(item, wfsession);
					
					if (nodeType.equals(TYPE_PAGE_CONTENT)) {
						sendPageReplicationComplete(item, approver, ReplicationActionType.DEACTIVATE, comment);
						return;
					} else if(nodeType.equals(TYPE_DAM_ASSET_CONTENT)) {
						sendAssetReplicationComplete(item, approver, ReplicationActionType.DEACTIVATE, comment);						
						return;
						
					}	
					throw new Exception("aemtrpsys EXCEPTION, bad process args");
				case REJECT:
					if (nodeType.equals(TYPE_PAGE_CONTENT)) {
						sendPageReplicationRejection(item, approver, environmentType, ReplicationActionType.DEACTIVATE);
						return;
					} else if(nodeType.equals(TYPE_DAM_ASSET_CONTENT)) {
						sendAssetReplicationRejection(item, approver, environmentType, ReplicationActionType.DEACTIVATE);				
						return;
						
					}				
					throw new Exception("aemtrpsys EXCEPTION, bad process args");	

				default:
					throw new Exception("aemtrpsys EXCEPTION, bad process args");

				}
				//
			    //
				//{action: test, address:paul_myers@troweprice.com, approver: "" }
				//
				//
				//{action: test, principal : aem_author_test, approver: "" }
				// 
				// or
				//{action: test, approver: ""} to test by initiator address.
				//
				case TEST:
					if (StringUtils.isNotBlank(testAddress)) {

						log.info("aemtrpsys = email, TESTING BY ADDRESS, address = {}", testAddress);
						

						sendTestEmail(testAddress);
						
						
					} else if (StringUtils.isNotBlank(principal)) {
						
						log.info("aemtrpsys = email, TESTING BY PRINCIPAL, principal = {}", principal);

						Set<String> addressSet = EmailUtils.getEmailAddress(resourceResolver, AccessControlUtil.getUserManager(resourceResolver.adaptTo(Session.class)),principal);
						if (addressSet.isEmpty()) {
							throw new Exception(
									"aemtrpsys = email, workflow principal = "
											+ principal
											+ ", has no email addresses in profile, workflowId = "
											+ item.getWorkflow().getId());
						}
						sendTestEmail(addressSet);
					} else {

						// if no test address is given, use the initiator's email
						// address.
						log.info("aemtrpsys = email, workflow initiator = {}", initiator);

						Set<String> addressSet = EmailUtils.getEmailAddress(resourceResolver, AccessControlUtil.getUserManager(resourceResolver.adaptTo(Session.class)),initiator);
						if (addressSet.isEmpty()) {
							throw new Exception(
									"aemtrpsys = email, workflow initiator = "
											+ initiator
											+ ", has no email address in profile, workflowId = "
											+ item.getWorkflow().getId());
						}
						sendTestEmail(addressSet);
					}

					return;

					//
					//{action: test-advanced, address:appba59}
					//
				case TEST_ADVANCED:
					if (StringUtils.isBlank(testAddress) == false) {
						workflowNotificationService.testAdvancedService(item, testAddress);
					} else {
						throw new Exception("aemtrpsys EXCEPTION, bad process args");
					}

					return;


				
				
			default:
				throw new Exception("aemtrpsys EXCEPTION, bad process args");

			}

		}

		catch (Exception e) {
			throw new WorkflowException(e);
		} finally {
			if (resourceResolver != null) {
				resourceResolver.close();
			}
			
		}
	}
	
	private void sendTestEmail(Set<String> addressSet) {
		
		for(String address : addressSet) {
			sendTestEmail(address);
		}	
		
	}
	
	private void sendTestEmail(String address){
		workflowNotificationService.testBasicService(address);
	}
	

	/*
	 * send an email to the approver to request Page activation
	 * either to 'stage' or to 'live' (as specified by environmentType)
	 * 
	 */
	private void sendPageReplicationRequest(WorkItem item, String approver, EnvironmentType environmentType, ReplicationActionType replicationType) throws WorkflowException {		
		try {
			workflowNotificationService.sendPageReplicationRequest(item, approver, environmentType, replicationType);
		} catch (Exception e) {
			throw new WorkflowException(e);
		}

	}
			
	
	
	/*
	 * send an email to the initiator that Page activation request was rejected.
	 * either to 'stage' or to 'live' (as specified by environmentType)
	 * 
	 */
	private void sendPageReplicationRejection(WorkItem item, String approver, EnvironmentType environmentType, ReplicationActionType replicationType) throws WorkflowException {
		try {
			workflowNotificationService.sendPageReplicationRejection(item, approver, environmentType, replicationType);
		} catch (Exception e) {
			throw new WorkflowException(e);
		}

	}
		


	/*
	 * send an email to the initialor and the approver that the activation is complete
	 * (Note: 'stage' or 'live' is irrelevant since 'completion' assumes live. 
	 * 
	 */
	private void sendPageReplicationComplete(WorkItem item, String approver, ReplicationActionType replicationType, String comment) throws WorkflowException {		
		try {
			workflowNotificationService.sendPageReplicationComplete(item, approver, replicationType, comment);
		} catch (Exception e) {
			throw new WorkflowException(e);
		}

	}			
	/*
	 * send an email to the approver to request Asset activation
	 * either to 'stage' or to 'live' (as specified by environmentType)
	 * 
	 */
	private void sendAssetReplicationRequest(WorkItem item, String approver, EnvironmentType environmentType, ReplicationActionType replicationType) throws WorkflowException {		
		try {
			workflowNotificationService.sendAssetReplicationRequest(item, approver, environmentType, replicationType);
		} catch (Exception e) {
			throw new WorkflowException(e);
		}

	}
	/*
	 * send an email to the initiator that Asset activation request was rejected.
	 * either to 'stage' or to 'live' (as specified by environmentType)
	 * 
	 */
	private void sendAssetReplicationRejection(WorkItem item, String approver, EnvironmentType environmentType, ReplicationActionType replicationType) throws WorkflowException {
		try {
			workflowNotificationService.sendAssetReplicationRejection(item, approver, environmentType, replicationType);
		} catch (Exception e) {
			throw new WorkflowException(e);
		}
	}
	/*
	 * send an email to the initialor and the approver that the activation is complete
	 * (Note: 'stage' or 'live' is irrelevant since 'completion' assumes live.)
	 * 
	 */
	private void sendAssetReplicationComplete(WorkItem item, String approver, ReplicationActionType replicationType, String comment) throws WorkflowException {		
		try {
			workflowNotificationService.sendAssetReplicationComplete(item, approver, replicationType, comment);
		} catch (Exception e) {
			throw new WorkflowException(e);
		}

	}
			
	
	/*
	 * test, activate, or deactivate
	 * 
	 * action type is mandatory, if no action is specified an excpetion is thrown and the workflow fails.
	 */
	private String getActionType(String processArgs) throws WorkflowException {
		try {
			JSONObject jObject = new JSONObject(processArgs);
			final String actionType = jObject.getString(ACTION_TYPE_KEY);  
			return (actionType != null) ? actionType : "";
		} catch (JSONException e) {
			throw new WorkflowException("aemtrpsys = email, EXCEPTION, can't read process args", e);
		}
		
	}
	
	/*
	 * the id of a user or group that approves the workflow
	 * 
	 * approver is optional (note that the activate and deactivate actions require an approver)
	 * return String will be empty if no approver is specified.
	 */
	private String getApprover(String processArgs) throws WorkflowException {
		try {
			JSONObject jObject = new JSONObject(processArgs);
			final String approver = jObject.optString(APPROVER_KEY);  
			return (approver != null) ? approver : "";
		} catch (JSONException e) {
			throw new WorkflowException("aemtrpsys = email, EXCEPTION, can't read process args", e);
		}
		
	}
	
	
	/*
	 * request or reject
	 * 
	 * email type is optional
	 * return String will be empty if no email type is specified.
	 */
	private String getEmailType(String processArgs) throws WorkflowException {
		try {
			JSONObject jObject = new JSONObject(processArgs);
			final String emailType = jObject.optString(EMAIL_TYPE_KEY);  
			return (emailType != null) ? emailType : "";
		} catch (JSONException e) {
			throw new WorkflowException("aemtrpsys = email, EXCEPTION, can't read process args", e);
		}
		
	}

	/*
	 * environment type is stage or live.
	 * 
	 * environment type is optional
	 * return String will be empty if no environment type is specified.
	 */
	private String getEnvironmentType(String processArgs) throws WorkflowException {
		try {
			JSONObject jObject = new JSONObject(processArgs);
			final String environmentType = jObject.optString(ENVIRONMENT_TYPE_KEY);  
			return (environmentType != null) ? environmentType : "";
		} catch (JSONException e) {
			throw new WorkflowException("aemtrpsys = email, EXCEPTION, can't read process args", e);
		}
		
	}

	/*
	 * test address is used for basic email testing.
	 * 
	 * test address is optional
	 * return String will be empty if no test address is specified.
	 */
	private String getTestAddress(String processArgs) throws WorkflowException {
		try {
			JSONObject jObject = new JSONObject(processArgs);
			final String testAddress = jObject.optString(TEST_ADDRESS_KEY);  
			return (testAddress != null) ? testAddress : "";
		} catch (JSONException e) {
			throw new WorkflowException("aemtrpsys = email, EXCEPTION, can't read process args", e);
		}
		
	}
	

	/*
	 * principal is used for basic email testing.
	 * 
	 * principal is optional
	 * return String will be empty if principal is specified.
	 */
	private String getPrincipal(String processArgs) throws WorkflowException {
		try {
			JSONObject jObject = new JSONObject(processArgs);
			final String principal = jObject.optString(TEST_PRINCIPAL_KEY);  
			return (principal != null) ? principal : "";
		} catch (JSONException e) {
			throw new WorkflowException("aemtrpsys = email, EXCEPTION, can't read process args", e);
		}
		
	}	

	private String getPreviousComment(WorkItem item, WorkflowSession wfsession) throws WorkflowException {
		

		log.info("current workItem Id = {}", item.getId());
		
		
		
		try {
			List<HistoryItem> historyItems = wfsession.getHistory(item.getWorkflow());
			// wfsession.getHistory returns an ordered list, so we want the last item
			Collections.reverse(historyItems);
			return historyItems.get(0).getComment();
		} catch (Throwable th) {
			// catch anything and wrap it in a WorkflowException so the workflow will stop.
			throw new WorkflowException(th);
		}
		
	}
	

}













//			
//
//// send request email
//if (emailType != null && emailType.contains("request")) {
//	// get request email subject
//	if(emailType.contains("preview")) {	// request previeweews
//		if(emailType.contains("deactivate")) {
//			emailSubject = EmailUtils.REQUEST_DEACTIVATE_PREVIEW_EMAIL_SUBJECT;
//		} else {
//			emailSubject = EmailUtils.REQUEST_PREVIEW_EMAIL_SUBJECT;
//		}
//	} else {	// request publish
//		if(emailType.contains("deactivate")) {
//			emailSubject = EmailUtils.REQUEST_DEACTIVATE_PUBLISH_EMAIL_SUBJECT;
//		} else {
//			emailSubject = EmailUtils.REQUEST_PUBLISH_EMAIL_SUBJECT;
//		}
//	}
//	// get request email template path
//	emailTemplatePath = EmailUtils.REQUEST_EMAIL_TEMPLATE_PATH;
//} else if (emailType.contains("response")) {	// send response email
//	// get response email subject
//	if(emailType.contains("preview")) { 
//		if(emailType.contains("deactivate")) { //response preview deactivate
//			emailSubject = EmailUtils.RESPONSE_DEACTIVATE_PREVIEW_EMAIL_SUBJECT;
//			if(emailType.contains("approved")) {
//				decision = "DEACTIVATE PREVIEW APPROVED";
//			} else {
//				decision = "DEACTIVATE PREVIEW REJECTED";
//			}
//		} else {// response preview 
//			emailSubject = EmailUtils.RESPONSE_PREVIEW_EMAIL_SUBJECT;
//			if(emailType.contains("approved")) {
//				decision = "PREVIEW APPROVED";
//			} else {
//				decision = "PREVIEW REJECTED";
//			}
//		}
//	} else {	// publish
//		if(emailType.contains("deactivate")) {
//			emailSubject = EmailUtils.RESPONSE_DEACTIVATE_PUBLISH_EMAIL_SUBJECT;
//			if(emailType.contains("approved")) {
//				decision = "DEACTIVATE PUBLISH APPROVED";
//			} else {
//				decision = "DEACTIVATE PUBLISH REJECTED";
//			}
//		} else {
//			emailSubject = EmailUtils.RESPONSE_PUBLISH_EMAIL_SUBJECT;
//			if(emailType.contains("approved")) {
//				decision = "PUBLISH APPROVED";
//			} else {
//				decision = "PUBLISH REJECTED";
//			}
//		}
//	}
//	// get response email template path
//	emailTemplatePath = EmailUtils.RESPONSE_EMAIL_TEMPLATE_PATH;
//}
//try {
//	
//	// get approver
//	approver = StringUtils.trim(paramDiv[1]);
//	
//	// get approve/reject comment
//	comment = EmailUtils.getComment(item, wfsession);
//	
//
//	UserManager userManager = AccessControlUtil.getUserManager(resourceResolver.adaptTo(Session.class));
//	
//	// get initiator email from profile
//	initiatorEmailSet = EmailUtils.getEmailAddress(resourceResolver, userManager, initiator);
//	if(initiatorEmailSet.isEmpty()){
//		log.error("trpaemsys = email, workflow initiator = {}, has no email address in profile", initiator);
//		return;
//	}
//	log.info("trpaemsys = email, workflow initiator's email address = {}", initiatorEmailSet);
//	// get approver's email from profile
//	approverEmailSet = EmailUtils.getEmailAddress(resourceResolver, userManager, approver);
//	if(approverEmailSet.isEmpty()){
//		log.error("trpaemsys = email, workflow approver = {} has no email address in profile", approver);
//		return;
//	}				
//	log.info("trpaemsys = email, approver's email address = {}", approverEmailSet);
//	
//	
//	// email FROM address comes from configuration
//	emailFrom = ConfigUtil.getConfiguration(ca, EMAIL_FROM_CONFIG_PID, EMAIL_FROM_CONFIG_KEY);
//	log.info("trpaemsys = email, FROM address = {}", emailFrom);
//								
//	// get email message
//	Resource templateResource = resourceResolver.getResource(emailTemplatePath);
//	emailMessage = EmailUtils.getEmailTemplate(templateResource);
//	
//	// get page name/page title & version history
//	Resource pageResource = resourceResolver.getResource(path+"/jcr:content");
//	if(null != pageResource) {
//		Node pageNode = pageResource.adaptTo(Node.class);
//		pageName = pageNode.hasProperty("jcr:title") ? pageNode.getProperty("jcr:title").getString() : path;
////		versionLabel = EmailUtils.getVersionLabel(pageNode);
////		log.info("AEM TRP Sys versionLabel="+versionLabel);
//	}
//
//	// replace token in email subject
//	emailSubject = EmailUtils.replaceEmailValue(emailSubject, "($page_name)",  (path + pageName) );
//
//	// replace tokens in email template
//	emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($domain_author)", domainAuthorUrl);
//	emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($page_name)", (path + pageName));
//	emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($author_name)", initiator);
////	emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($version)", versionLabel);
////	emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($URL)", URL);
//	emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($decision)", decision);
////	emailMessage = EmailUtils.replaceEmailValue(emailMessage, "($comment)", comment);
//
//} catch (Throwable th) {
//	log.error("trpaemsys = email, ", th);
//} finally {
//	if (resourceResolver != null) {
//		resourceResolver.close();
//	}
//}
//
//// Declare a MessageGateway service
//MessageGateway<Email> messageGateway;
//// Set up email message
//Email email = new SimpleEmail();
//for(String approverEmail : approverEmailSet) {
//	email.addTo(approverEmail);
//}
//for(String initiatorEmail : initiatorEmailSet) {
//	email.addCc(initiatorEmail);
//}
//email.setSubject(emailSubject);
//email.setFrom(emailFrom);
//email.setMsg(emailMessage);
//
//// send email
//messageGateway = messageGatewayService.getGateway(Email.class);
//messageGateway.send((Email) email);