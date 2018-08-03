package org.dubbo.spring.boot.tigerz.api.service;

import org.dubbo.spring.boot.tigerz.api.entity.User;

public class UserServiceResponse {
    
    private int errorCode;
    private String errorMessage;
    private User user;
    
    public int getErrorCode() {
        return errorCode;
    }
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    
    
    
    
    
    
    

    
    
    

}
