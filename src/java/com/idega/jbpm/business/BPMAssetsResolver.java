package com.idega.jbpm.business;

import java.util.List;

import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.user.data.User;

public interface BPMAssetsResolver {

	public List<User> getUsersConectedToProcess(ProcessInstanceW piW);
	
}