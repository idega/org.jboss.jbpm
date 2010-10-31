package com.idega.jbpm.presentation.beans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Scope("request")
@Service("bpmSandbox")
public class BpmSandbox {
	
	private Long tokenId;
	
	@Autowired(required = false)
	private SandboxExecutable executable;
	
	public void executeLicenseFeeClaimReceived() {
		
		if (tokenId != null) {
			executable.execute(tokenId);
		} else {
			throw new IllegalArgumentException("No token id");
		}
	}
	
	public Long getTokenId() {
		return tokenId;
	}
	
	public void setTokenId(Long tokenId) {
		this.tokenId = tokenId;
	}
	
	public interface SandboxExecutable {
		
		public abstract void execute(long tokenId);
	}
}