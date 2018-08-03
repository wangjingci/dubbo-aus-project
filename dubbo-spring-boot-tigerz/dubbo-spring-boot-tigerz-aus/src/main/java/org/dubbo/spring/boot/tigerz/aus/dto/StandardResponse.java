package org.dubbo.spring.boot.tigerz.aus.dto;


public class StandardResponse {
	private Boolean isOK = true;
	private String errorMessage = "";
	private Object data = null;
	
	public StandardResponse(){
		
	}
	
	public Boolean getIsOK() {
		return isOK;
	}

	public void setIsOK(Boolean isOK) {
		this.isOK = isOK;
	}

	public void setData(Object obj){
		this.data = obj;
	}

	public Object getData() {
		return data;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	

	
	
}
