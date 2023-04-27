package com.anf.core.services;
 

import com.adobe.granite.workflow.exec.WorkItem;
import com.anf.core.workflow.EmailWorkflow.EnvironmentType;
import com.day.cq.replication.ReplicationActionType;

public interface WorkflowNotificationService {

	public void testBasicService(String address);

	public void testAdvancedService(WorkItem workItem, String address)  throws Exception;
	
	
	// Page Replication
	public void sendPageReplicationRequest(WorkItem workItem, String approver, EnvironmentType environmentType, ReplicationActionType replicationType) throws Exception;
	public void sendPageReplicationComplete(WorkItem workItem, String approver, ReplicationActionType replicationType, String previousComment)  throws Exception;
	public void sendPageReplicationRejection(WorkItem workItem, String approver, EnvironmentType environmentType, ReplicationActionType replicationType)  throws Exception;
	

	// Asset Replication
	public void sendAssetReplicationRequest(WorkItem workItem, String approver, EnvironmentType environmentType, ReplicationActionType replicationType)  throws Exception;
	public void sendAssetReplicationComplete(WorkItem workItem, String approver, ReplicationActionType replicationType, String previousComment)  throws Exception;
	public void sendAssetReplicationRejection(WorkItem workItem, String approver, EnvironmentType environmentType, ReplicationActionType replicationType)  throws Exception;
		
	
}