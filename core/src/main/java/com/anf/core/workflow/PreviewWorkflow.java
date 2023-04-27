package com.anf.core.workflow;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import com.anf.core.services.WorkflowNotificationService;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.replication.*;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;


/**
 * Agent Filter Replication workflow process that sets an <code>agent</code>
 * property to the payload based on the process argument value.
 */
@Component(service = WorkflowProcess.class,
		   property = {Constants.SERVICE_DESCRIPTION + "= Workflow PreviewWorkflow process.",
				Constants.SERVICE_VENDOR + "=adobe"})
public class PreviewWorkflow implements WorkflowProcess {

	// following constants are used in json configuration used for the proccess args.
	private static final String AGENTS_KEY = "agents";
	private static final String APPROVER_KEY = "approver";

	
	
	
	/**
	 * the logger
	 */
	private static final Logger log = LoggerFactory.getLogger(PreviewWorkflow.class);
	
	// payload types, see https://docs.adobe.com/docs/en/cq/5-6-1/workflows/wf-ref.html
	public static final String TYPE_JCR_PATH = "JCR_PATH";
	public static final String TYPE_JCR_UUID = "JCR_UUID";
	
	@Reference
	protected Replicator replicator;
	@Reference
    protected AgentManager agentManager;
	@Reference
	private WorkflowNotificationService workflowNotificationService;

	
	
	/*
	 * 
	 * This Workflow process replicates content using the specified replication agents.
	 * 
	 * Uses JSON for process args
	 * {agents : [{name : publish_internal_13}] }
	 * 
	 * 
	 * 
	 */
	
	public void execute(WorkItem item, WorkflowSession workflowSession,
			MetaDataMap args) throws WorkflowException {
		
		Map<String, Agent> agentsMap = agentManager.getAgents();
		WorkflowData workflowData = item.getWorkflowData();
		String processArgs = args.get("PROCESS_ARGS", "false");

		log.info("aemtrpsys activate-workflow, processArgs = {}", processArgs);

		List<String> agents = getAgents(processArgs);
		
		log.info("aemtrpsys activate-workflow, agents = {}", Arrays.toString(agents.toArray()));
		
		String approver = getApprover(processArgs);
		
		log.info("aemtrpsys activate-workflow, approver = {}", approver);
		
        if (workflowData.getPayloadType().equals(TYPE_JCR_PATH)) {
            String path = workflowData.getPayload().toString() + "/jcr:content";
            
            try {
            
            	// replicate 
              for(final String agentName : agents){
    			ReplicationOptions opts = new ReplicationOptions();
                opts.setFilter(new AgentFilter(){
                    public boolean isIncluded(final Agent agent) {
                    	return agentName.equals(agent.getId());
                    }
                });	
                Session jcrSession = workflowSession.adaptTo(Session.class);
                replicator.replicate(jcrSession, ReplicationActionType.ACTIVATE, path, opts);
                /*
                 * the following code optionally saves the agent name as a property on the content node.
                 * not really needed.
                 */
//                Node node = (Node) jcrSession.getItem(path);
//                if (node != null) {
//                  node.setProperty(agentName, true );
//                  jcrSession.save();
//                }
        		log.info("aemtrpsys activate-workflow COMPLETE, agent = {}, path = {}", agentName, path);
                
              }               
            } catch (Exception e) {
              throw new WorkflowException("aemtrpsys activate-workflow EXCEPTION, ", e);
            } 
            
        }
	}
	

	/*
	 * agents is mandatory, if no agents are specified an excpetion is thrown and the workflow fails.
	 * 
	 */
	private List getAgents(String processArgs) throws WorkflowException {
		ArrayList<String> arrayList = new ArrayList<String>();
		try {
			JSONObject jObject = new JSONObject(processArgs);
			 final JSONArray agents = jObject.getJSONArray(AGENTS_KEY);
			 for (int i = 0; i < agents.length(); ++i) {
			      final JSONObject agent = agents.getJSONObject(i);
			      String name = agent.getString("name");  
			      arrayList.add(name);
			 }
		} catch (JSONException e) {
			throw new WorkflowException("aemtrpsys activate-workflow, EXCEPTION, can't read process args", e);
		}
		
		return arrayList;
	}
	
	/*
	 * approver is optional
	 * return String will be empty if no approver is specified.
	 */
	private String getApprover(String processArgs) throws WorkflowException {
		try {
			JSONObject jObject = new JSONObject(processArgs);
			final String approver = jObject.optString(APPROVER_KEY);  
			return (approver != null) ? approver : "";
		} catch (JSONException e) {
			throw new WorkflowException("aemtrpsys activate-workflow, EXCEPTION, can't read process args", e);
		}
		
	}
}