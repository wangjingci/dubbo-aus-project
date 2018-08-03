/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dubbo.spring.boot.tigerz.gm.controller;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dubbo.spring.boot.tigerz.api.entity.User;
import org.dubbo.spring.boot.tigerz.api.service.IP2LocationService;
import org.dubbo.spring.boot.tigerz.api.service.IP2LocationService.IPInfoProperty;
import org.dubbo.spring.boot.tigerz.api.service.UserService;
import org.dubbo.spring.boot.tigerz.api.service.UserServiceResponse;
import org.dubbo.spring.boot.tigerz.gm.dto.StandardResponse;
import org.dubbo.spring.boot.tigerz.gm.service.FeedbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.tigerz.easymongo.util.Assert;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


/**
 * 
 * MainController
 * @Desc: 
 * @RestController相当于@Controller, @ResponseBody 确保返回json对象
 * 
 * @Company: TigerZ
 * @author Wang Jingci
 * @date 2018年4月2日 下午7:25:46
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)  //跨域
public class MainController {
    
    @Autowired
    private IP2LocationService ip2LocationService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private FeedbackService feedbackService;
    
    private Logger logger = LoggerFactory.getLogger(MainController.class);
    
    /**
     * 提取用户IP跳转到相应的域名
     * eg. 新西兰用户跳转到nz.tigerz.com
     * 
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public void IPTransfer(HttpServletRequest request,
            HttpServletResponse response) {
        
        logger.info("start tigerz.com");
        // Get IP address
        String IP = request.getRemoteAddr();
        EnumMap<IPInfoProperty,String> info = ip2LocationService.transfer(IP);
        // Set default value
        String code = "NZ";
        String region = "Auckland";
        if (info != null) {
            code = info.get(IPInfoProperty.CODE);
            region = info.get(IPInfoProperty.REGION);
        } 
        
        String targetUrl = "";
        switch (code) {
        case "AU":
            targetUrl = "http://aus.tigerz.com?region=" + region;
            break;
        case "NZ":
            targetUrl = "http://www.tigerz.nz?region=" + region;
            break;
        default:
            targetUrl = "http://www.tigerz.nz?region=" + region;
            break;
        }
        
        try {
            response.sendRedirect(targetUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * 用户登录
     * @throws Exception 
     */
    @ApiOperation(value="用户登录", notes="Email地址加密码登录")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public StandardResponse login(HttpServletResponse response,
            HttpServletRequest request,
            @RequestBody @ApiParam(value = "{\"email\":\"wangjingci@126.com\",\"passwd\":\"123\"}") Map<String,Object> acount) throws Exception {
        Assert.notNull(acount, "Parameter can't be null");
        Assert.notNull(acount.get("email"), "Email can't be null");
        Assert.notNull(acount.get("passwd"), "");
        String email = (String)acount.get("email");
        String passwd = (String)acount.get("passwd");
        
        StandardResponse sr = new StandardResponse();
        
        UserServiceResponse resp = userService.login(email, passwd);
        
        if (resp.getErrorCode() != 0) {
            sr.setIsOK(false);
            sr.setErrorMessage(resp.getErrorMessage());
            logger.warn(resp.getErrorMessage()+":" + email);
            return sr;
        }
        
        HttpSession httpSession = request.getSession();
        httpSession.setAttribute("user", resp.getUser());
        Map<String, Object> data = new HashMap<>();
        data.put("userName", resp.getUser().getEmail());
        data.put("token", httpSession.getId());
        data.put("msg","Login Successfully");
        sr.setData(data);
        
        return sr;
    }
    
    /**
     * 第三方用户登录
     * @throws Exception 
     */
    @ApiOperation(value="第三方登录", notes="Facebook，Wecat等")
    @RequestMapping(value = "/loginThird", method = RequestMethod.POST)
    public StandardResponse loginThird(HttpServletResponse response,
            HttpServletRequest requset,
            @RequestBody @ApiParam(value = "{\"thirdId\":\"xxxxx\",\"email\":\"wangjingci@126.com\",\"fromSite\":\"facebook\",\"location\":\"nz\"}") Map<String,Object> acount) throws Exception {
        Assert.notNull(acount, "参数不能是空");
        Assert.notNull(acount.get("thirdId"), "第三方ID不能是空");
        Assert.notNull(acount.get("fromSite"), "第三方来源不能是空");
        Assert.notNull(acount.get("location"), "来自区域不能是空");
        
        String thirdId = (String)acount.get("thirdId");
        String fromSite = (String)acount.get("fromSite");
        String location = (String)acount.get("location");

        String ip = requset.getRemoteAddr();
        
        StandardResponse sr = new StandardResponse();
        User user = new User();
        user.setThirdId(thirdId);
        user.setNickName((String)acount.get("nickName"));
        user.setEmail((String)acount.get("email"));
        user.setFromSite(fromSite);
        user.setLocation(location);
        user.setIp(ip);
        
        UserServiceResponse resp = userService.loginThird(user);
        
        if (resp.getErrorCode() != 0) {
            sr.setIsOK(false);
            sr.setErrorMessage(resp.getErrorMessage());
            logger.warn(resp.getErrorMessage()+":" + acount.get("email"));
            return sr;
        }
        
        HttpSession httpSession = requset.getSession();
        httpSession.setAttribute("user", resp.getUser());
        Map<String, Object> data = new HashMap<>();
        data.put("userName", resp.getUser().getEmail());
        data.put("token", httpSession.getId());
        data.put("msg","Login Successfully");
        sr.setData(data);
        return sr;
    }

    
    /**
     * 用户注册
     */
    @ApiOperation(value="用户注册", notes="Email地址加密码即可注册")
    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public StandardResponse signup(HttpServletResponse response,
            HttpServletRequest requset,
            @ApiParam(value = "{\"email\":\"wangjingci@126.com\",\"passwd\":\"123\",\"role\":0,\"location\":\"nz\"}") @RequestBody Map<String,Object> acount) throws Exception {
        Assert.notNull(acount, "参数不能是空");
        String email = (acount.get("email") != null) ? (String)acount.get("email"):null;
        Assert.notNull(email, "邮箱不能是空");
        String passwd = (acount.get("passwd") != null) ? (String)acount.get("passwd"):null;
        Assert.notNull(passwd, "密码不能是空");
        Integer role = (acount.get("role") != null) ? (Integer)acount.get("role"):null;
        Assert.notNull(role, "角色不能是空");
        String location = (acount.get("location") != null) ? (String)acount.get("location"):null;
        Assert.notNull(location, "来自区域不能是空");
        
        StandardResponse sr = new StandardResponse();
        String ip = requset.getRemoteAddr();
        User user = new User();
        user.setEmail(email);
        user.setPasswd(passwd);
        user.setRole(role);
        user.setLocation(location);
        user.setIp(ip);
        UserServiceResponse resp = userService.signUp(user);
        
        if (resp.getErrorCode() != 0) {
            sr.setIsOK(false);
            sr.setErrorMessage(resp.getErrorMessage());
            logger.warn(resp.getErrorMessage()+":" + email);
            return sr;
        }
        
        HttpSession httpSession = requset.getSession();
        httpSession.setAttribute("user", resp.getUser());
        Map<String, Object> data = new HashMap<>();
        data.put("userName", resp.getUser().getEmail());
        data.put("token", httpSession.getId());
        data.put("msg","Signup Successfully");
        sr.setData(data);
        
        return sr;
    }
    
    
    /**
     * 获取用户基础信息
     */
    @ApiOperation(value="获取用户基础信息", notes="返回用户名等基础信息")
    @RequestMapping(value = "/getUserInfo", method = RequestMethod.GET)
    public StandardResponse getUserInfo(HttpServletRequest request, HttpSession session) throws Exception {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            System.out.println("Cookie name:" + cookie.getName() + ", Value:" + cookie.getValue() );
        }
        
        
        StandardResponse sr = new StandardResponse();
        
        User user = (User)session.getAttribute("user");
        
        if (user == null) {
            sr.setIsOK(false);
            sr.setErrorMessage("Please login first!");
        } else {
            sr.setIsOK(true);
            sr.setData(user.getSimpleUser());
        }
        
        return sr;
    }
    
    @ApiOperation(value="用户登出", notes="用户登出")
    @RequestMapping(value = "/logOff", method = RequestMethod.GET)
    public StandardResponse logOff(HttpServletResponse response,
            HttpServletRequest requset) {
        HttpSession session = requset.getSession();
        session.removeAttribute("user");
        StandardResponse sr = new StandardResponse();
        return sr;
    }
    
    
    @ApiOperation(value="修改密码", notes="修改用户密码")
    @RequestMapping(value = "/changePassword", method = RequestMethod.POST)
    public StandardResponse changePassword(
            @RequestBody @ApiParam(value = "{\"email\":\"wjc@126.com\",\"oldPassword\":\"123\",\"456\"}") Map<String,Object> userInfo) throws Exception {

        String email = (String)userInfo.get("email");
        String oldPasswd = (String)userInfo.get("oldPassword");
        String newPasswd = (String)userInfo.get("newPassword");
        
        UserServiceResponse resp = userService.changePassword(email, oldPasswd, newPasswd);
        StandardResponse sr = new StandardResponse();
        
        if (resp.getErrorCode() != 0) {
            sr.setIsOK(false);
            sr.setErrorMessage(resp.getErrorMessage());
            logger.warn(resp.getErrorMessage()+":" + email);
            return sr;
        }
        
        sr.setData("Password modification success");
        
        return sr;
    }
   
   
    
   /**
    * 用户反馈
    * 目前用户反馈可分为如下几个类别
    * 1. 数据纠正
    * 2. 版权申诉
    * 3. 问中介房子
    * 4. 用户问题反馈
    * @param countryCode
    * @param type  -> "data", "infringement" "feedback" "house"
    * @param content
    */
    @ApiOperation(value="用户反馈", notes="包括如下类型的反馈->data,infringement,feedback,house")
    @RequestMapping(value = "/userFeedback", method = RequestMethod.POST)
    public StandardResponse userFeedback(
            @ApiParam(value = "{\"type\":\"house\",\"countryCode\":\"au\",\"fromPropId\":\"5aa04e70aebec17dbf8f0508\",\"content\":\"feedback content\",\"email\":\"user@gmail.com\",\"phone\":\"1243333333\",\"agents\":[{\"agentName\":\"tommy\",\"agentMail\":\"86441350@qq.com\"}]}") @RequestBody Map<String,Object> feedbackContent) throws Exception {
        feedbackService.feedback(feedbackContent);
        return new StandardResponse();
    }

    
    
    
    /**
     * Test service available
     * @return
     */
    @ApiOperation(value="测试", notes="测试")
    @RequestMapping(value = "/test/{name}/{age}", method = RequestMethod.GET)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "name", value = "用户名", defaultValue = "wjc",required = true,paramType = "path"),
        @ApiImplicitParam(name = "age", value = "年龄", defaultValue = "6",required = true,paramType = "path")
    })
    public String test(@PathVariable String name, @PathVariable int age) {
        System.out.println("===》启动sayHello " + name);
        return "hello " + name + " "+ age;
    }
    
    @ApiOperation(value="测试Session", notes="测试Session")
    @RequestMapping(value = "/testSession", method = RequestMethod.GET)
    public String testSession(HttpServletRequest request) {
        // 没有就创建，有就获取
        HttpSession session = request.getSession();

        if (session.isNew()) {
            session.setMaxInactiveInterval(5);
            User user = new User();
            user.setEmail("wjc@126.com");
            user.setPasswd("123");
            session.setAttribute("user", user);
            System.out.println("==========》 the session is new");
        } else {
            System.out.println("==========》 the session is old");
        }
        
        
        User user2 = (User) session.getAttribute("user");  
        return "SessionId:" + session.getId() + "\n content:" + user2.toString();
    }
    
 

 
}
