package org.dubbo.spring.boot.tigerz.api.service;


import org.dubbo.spring.boot.tigerz.api.entity.User;

public interface UserService {
    

    UserServiceResponse login(String email, String passwd) throws InstantiationException, IllegalAccessException;

    UserServiceResponse signUp(User user)
            throws IllegalArgumentException, IllegalAccessException, InstantiationException;
    
    
    UserServiceResponse loginThird(User user) throws InstantiationException, IllegalAccessException;

    UserServiceResponse changePassword(String email, String oldPasswd, String newPasswd) throws InstantiationException, IllegalAccessException;

}
