package org.dubbo.spring.boot.tigerz.gm.dao;

import org.dubbo.spring.boot.tigerz.api.entity.User;

import com.mongodb.BasicDBObject;

public interface UserDAO {

    String addUser(User user) throws IllegalArgumentException, IllegalAccessException;

    void updateUser(String userId, BasicDBObject update);
    
    void removeUser(User user);

    boolean isUserExist(String email) throws InstantiationException, IllegalAccessException;

    public User getUserByEmail(String email) throws InstantiationException, IllegalAccessException;
    
    public User getUserByThirdId(String thirdId) throws InstantiationException, IllegalAccessException;
}