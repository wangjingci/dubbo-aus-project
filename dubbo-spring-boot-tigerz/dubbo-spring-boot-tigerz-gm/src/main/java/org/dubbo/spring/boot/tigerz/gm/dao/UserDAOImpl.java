package org.dubbo.spring.boot.tigerz.gm.dao;

import java.util.Date;

import org.bson.types.ObjectId;
import org.dubbo.spring.boot.tigerz.api.entity.User;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.MongoService;

@Repository
public class UserDAOImpl implements UserDAO {
    
    MongoService mongoService = MongoService.getInstance();
    

    /* (non-Javadoc)
     * @see org.dubbo.spring.boot.tigerz.gm.dao.UserDAO#addUser(org.dubbo.spring.boot.tigerz.gm.entity.User)
     */
    @Override
    public String addUser(User user) throws IllegalArgumentException, IllegalAccessException {
        Date date = new Date();
        user.setCreateTime(date);
        user.setUpdateTime(date);
        ObjectId objectId = mongoService.insertById(user);
        return objectId.toString();
    }
    

    /* (non-Javadoc)
     * @see org.dubbo.spring.boot.tigerz.gm.dao.UserDAO#removeUser(org.dubbo.spring.boot.tigerz.gm.entity.User)
     */
    @Override
    public void removeUser(User user) {
        
    }

    /* (non-Javadoc)
     * @see org.dubbo.spring.boot.tigerz.gm.dao.UserDAO#isUserExist(java.lang.String)
     */
    @Override
    public boolean isUserExist(String email) throws InstantiationException, IllegalAccessException {
        BasicDBObject query = new BasicDBObject("email",email);
        User user = mongoService.findOne(query, User.class);
        if (user == null) {
            return false;
        } else {
            return true;
        }
    }
    
    /* (non-Javadoc)
     * @see org.dubbo.spring.boot.tigerz.gm.dao.UserDAO#isUserExist(java.lang.String)
     */
    @Override
    public User getUserByEmail(String email) throws InstantiationException, IllegalAccessException {
        BasicDBObject query = new BasicDBObject("email",email);
        User user = mongoService.findOne(query, User.class);
        return user;
    }


    @Override
    public User getUserByThirdId(String thirdId) throws InstantiationException, IllegalAccessException {
        BasicDBObject query = new BasicDBObject("third_id",thirdId);
        User user = mongoService.findOne(query, User.class);
        return user;
    }


    @Override
    public void updateUser(String userId, BasicDBObject update) {
        BasicDBObject query = new BasicDBObject("_id",new ObjectId(userId));
        Date date = new Date();
        update.append("update_time", date);
        
        mongoService.update(query, update, User.class);
    }
    
    
    
}
