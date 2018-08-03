package org.dubbo.spring.boot.tigerz.gm.service;

import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.dubbo.spring.boot.tigerz.gm.dao.UserDAO;
import org.dubbo.spring.boot.tigerz.api.entity.User;
import org.dubbo.spring.boot.tigerz.api.service.UserService;
import org.dubbo.spring.boot.tigerz.api.service.UserServiceResponse;
import org.dubbo.spring.boot.tigerz.gm.enums.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.ExpiringSession;
import org.springframework.session.SessionRepository;

import com.alibaba.dubbo.config.annotation.Service;
import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.util.Assert;

@Service(
        version = "1.0.0",
        application = "${dubbo.application.id}",
        protocol = "${dubbo.protocol.id}",
        registry = "${dubbo.registry.id}"
)
public class UserServiceImpl implements UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Resource(name="sessionRepository")  
    private SessionRepository<ExpiringSession> sessionRepository;
    
    @Autowired
    UserDAO userDAO;

    /**
     * 用户名密码登录模式
     * Email做key
     */
    
    @Override
    public UserServiceResponse login(String email, String passwd) throws InstantiationException, IllegalAccessException {
        Assert.notNull(email, "email must not null");
        Assert.notNull(passwd, "passwd must not null");

        UserServiceResponse respones = new UserServiceResponse();
        // 首选查询是否有这个用户,如果有这个用户则报错
        User user = userDAO.getUserByEmail(email);
        if (user == null) {
            respones.setErrorCode(ResultCode.USER_NOT_EXIST.getCode());
            respones.setErrorMessage(ResultCode.USER_NOT_EXIST.getMsg());
            return respones;
        }
        
        String realPasswd = user.getPasswd();
        if (StringUtils.equals(realPasswd,passwd)) {
            respones.setUser(user);
            respones.setErrorCode(0);
            return respones;
        } else {
            respones.setErrorCode(ResultCode.PASSWORD_ERROR.getCode());
            respones.setErrorMessage(ResultCode.PASSWORD_ERROR.getMsg());
            return respones;
        }
        
    }
    
    /**
     * 第三方登录模式
     * 以第三方ID作为key
     */
    @Override
    public UserServiceResponse loginThird(User user) throws InstantiationException, IllegalAccessException {
        Assert.notNull(user, "the user must not null");
        Assert.notNull(user.getThirdId(), "the user must not null");
        
        // 判断用户是否存在，如果不存在则注册，如果存在则登录
        User currentUser = userDAO.getUserByThirdId(user.getThirdId());
        if (currentUser == null) {
            return doSignUp(user);
        }
        
        // 如果用户存在，不在添加数据直接生产token
        UserServiceResponse respones = new UserServiceResponse();
        respones.setUser(currentUser);
        respones.setErrorCode(0);
        return respones;
    }

    @Override
    public UserServiceResponse signUp(User user) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
        // String email, String passwd, int role, String location, String IP
        Assert.notNull(user, "user must not null");
        Assert.notNull(user.getEmail(), "email 不能是空");
        Assert.notNull(user.getPasswd(), "password 不能是空");
        
        if (userDAO.isUserExist(user.getEmail())) {
            UserServiceResponse respones = new UserServiceResponse();
            respones.setErrorCode(ResultCode.USER_EXIST.getCode());
            respones.setErrorMessage(ResultCode.USER_EXIST.getMsg());
            return respones;
        }
        
        return doSignUp(user);
    }
    
    // 可能因为这个类不是简单的服务，是dubbo服务的一部分，所以必须要在接口里注册一下
//    public void getBackPassword() {
//        /**
//         * 1. 提供邮箱，我们判断是否有这个用户
//         * 2. 给用户发送一封邮件，让他去设置密码
//         * 3. 用户点击链接，重新设置密码
//         */
//    }
    
    
    @Override
    public UserServiceResponse changePassword(String email, String oldPasswd, String newPasswd) throws InstantiationException, IllegalAccessException {
        User user = userDAO.getUserByEmail(email);
        UserServiceResponse respones = new UserServiceResponse();
        if (user == null) {
            respones.setErrorCode(ResultCode.USER_NOT_EXIST.getCode());
            respones.setErrorMessage(ResultCode.USER_NOT_EXIST.getMsg());
            return respones;
        }
        
        if (!user.getPasswd().equals(oldPasswd)) {
            respones.setErrorCode(ResultCode.PASSWORD_ERROR.getCode());
            respones.setErrorMessage(ResultCode.PASSWORD_ERROR.getMsg());
            return respones;
        } 
        
        BasicDBObject update = new BasicDBObject("passwd",newPasswd);
        try {
            userDAO.updateUser(user.get_id(), update);
            user.setPasswd(newPasswd);
            
            respones.setUser(user);
            respones.setErrorCode(0);
            
        } catch (Exception e) {
            e.printStackTrace();
            respones.setErrorCode(ResultCode.UPDATE_USER_FAIL.getCode());
            respones.setErrorMessage(ResultCode.UPDATE_USER_FAIL.getMsg());
        }
        
        
        
        
        return respones;
    }
    
    
    private UserServiceResponse doSignUp(User user) throws InstantiationException, IllegalAccessException {
        UserServiceResponse respones = new UserServiceResponse();

        // 创建用户并返回token
        createUser(user);
        String id = user.get_id();
        if (StringUtils.isEmpty(id)) {
            respones.setErrorCode(ResultCode.CREATE_USER_FAIL.getCode());
            respones.setErrorMessage(ResultCode.CREATE_USER_FAIL.getMsg());
            return respones;
        }
        respones.setUser(user);
        logger.info("成功注册用户:"+user.getEmail());
        
        return respones;
    }
    
    
    private User createUser(User user) throws IllegalArgumentException, IllegalAccessException {     
        String id = userDAO.addUser(user);
        user.set_id(id);
        return user;
    }


    
}
