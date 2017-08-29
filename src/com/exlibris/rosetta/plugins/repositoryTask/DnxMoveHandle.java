package com.exlibris.rosetta.plugins.repositoryTask;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.exlibris.core.infra.common.exceptions.logging.ExLogger;
import com.exlibris.core.sdk.repository.TaskResults;
import com.exlibris.digitool.common.dnx.DnxDocumentHelper;
import com.exlibris.digitool.common.dnx.DnxDocumentHelper.InternalIdentifier;
import com.exlibris.digitool.common.dnx.DnxDocumentHelper.ObjectIdentifier;
import com.exlibris.digitool.repository.api.IEEditor;
import com.exlibris.digitool.repository.api.RepositoryTaskPlugin;

public class DnxMoveHandle implements RepositoryTaskPlugin {

	ExLogger log = ExLogger.getExLogger(DnxMoveHandle.class);

	private final String FAIL_FIND_VALUE = "No Handle value found";
	private final String FAIL_GET_DNX = "Failed to get Dnx for IE";

	public DnxMoveHandle() {
		super();
	}

	public TaskResults execute(IEEditor ieEditor, Map<String, String> initParams, TaskResults taskResults) {

		log.info("Executing DnxMoveHandle for " + ieEditor.getIEPid());

		init(initParams);
		DnxDocumentHelper ieDnxH = null;

		//get Dnx from IE
		try {
			ieDnxH = ieEditor.getDnxHelperForIE();
		} catch (Exception e) {
			taskResults.addResult(ieEditor.getIEPid(), null, false, FAIL_GET_DNX);
			return taskResults;
		}

		//extract Handle
		String handleValue = getHandleValue(ieDnxH);

		//update Dnx
		if (handleValue == null) {
			taskResults.addResult(ieEditor.getIEPid(), null, false, FAIL_FIND_VALUE);
		} else {
			log.info("Handle for " + ieEditor.getIEPid() + " is " + handleValue);
			log.info("Starting DNX update task...");
			try {
				updateDnx(ieDnxH, ieEditor, handleValue);
			} catch (Exception e) {
				taskResults.addResult(ieEditor.getIEPid(), null, false, e.getMessage());
			}
		}
		
		return taskResults;
		
	}

	private String getHandleValue(DnxDocumentHelper ieDnxH) {
		
		String handleValue = null;
		
		List<InternalIdentifier> intIdList = ieDnxH.getInternalIdentifiers();
		for (InternalIdentifier intId : intIdList) {
			if (intId.getInternalIdentifierType().matches("[Hh]andle")) {
				handleValue = intId.getInternalIdentifierValue();
			}
		}
		
		return handleValue;
		
	}

	private void updateDnx(DnxDocumentHelper ieDnxH, IEEditor ieEditor, String handleValue) throws Exception {
		
		boolean existHandle = false;
		
		List<ObjectIdentifier> objIdList = ieDnxH.getObjectIdentifiers();
		for (ObjectIdentifier objId : objIdList) {
			if (objId.getObjectIdentifierType().equals("HANDLE")) {
				existHandle = true;
			}
		}
		
		if (existHandle == false) {
			//remove InternalIdentifier of type handle or Handle from DnxDocumentHelper
			List<InternalIdentifier> intIdList = ieDnxH.getInternalIdentifiers();
			List<InternalIdentifier> updatedIntIdList = new ArrayList<InternalIdentifier>();
			for (InternalIdentifier intId : intIdList) {
				if (!intId.getInternalIdentifierType().matches("[Hh]andle")) {
					updatedIntIdList.add(intId);
				}
			}
			ieDnxH.setInternalIdentifiers(updatedIntIdList);
			log.info("Successfully deleted the handle from InternalIdentifier section");
			
			//add ObjectIdentifier of type HANDLE to DnxDocumentHelper
			ObjectIdentifier newObjId = ieDnxH.new ObjectIdentifier("HANDLE",handleValue);
			objIdList.add(newObjId);
			ieDnxH.setObjectIdentifiers(objIdList);
			
			//update DnxDocumentHelper in IEEditor
			ieEditor.setDnxForIE(ieDnxH);
			log.info("Successfully copied the Handle to ObjectIdentifier section");
		} else {
			throw new Exception("Handle for " + ieEditor.getIEPid() + " already exists as ObjectIdentifier");
		}
	}

	private void init(Map<String, String> initParams) {
		
	}

	public boolean isReadOnly() {
		return false;
	}

	public static void main(String[] args) {
		System.out.println("Hello");
	}

}