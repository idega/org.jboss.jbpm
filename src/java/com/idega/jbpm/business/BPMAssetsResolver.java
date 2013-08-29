package com.idega.jbpm.business;

import java.util.List;
import java.util.Map;

import com.idega.user.data.User;

public interface BPMAssetsResolver {

	public List<User> getUsersConectedToProcess(Long piId, String procDefName, Map<String, Object> variables);

}