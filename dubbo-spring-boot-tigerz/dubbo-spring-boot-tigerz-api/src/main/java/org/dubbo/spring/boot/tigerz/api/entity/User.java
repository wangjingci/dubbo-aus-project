package org.dubbo.spring.boot.tigerz.api.entity;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnore;

public class User implements java.io.Serializable {
    
    private static final long serialVersionUID = 2L;
    private String _id;
    private String email;
    private String passwd;
    private Integer role = 0;
    private String location;
    private String ip;
    private Boolean isFormalUser = true;
    private String nickName;
    private String thirdId;
    private String fromSite;
    private Boolean fromThirdPart = false;
    private Date createTime;
    private Date updateTime;
    
    public User() {
        
    }
    


    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore   
    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isFormalUser() {
        return isFormalUser;
    }

    public String getNickName() {
        return nickName;
    }
    

    public String getFromSite() {
        return fromSite;
    }

    public void setFromSite(String fromSite) {
        this.fromSite = fromSite;
    }
    
    public Boolean getFromThirdPart() {
        return fromThirdPart;
    }

    public void setFromThirdPart(Boolean fromThirdPart) {
        this.fromThirdPart = fromThirdPart;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getThirdId() {
        return thirdId;
    }

    public void setThirdId(String thirdId) {
        this.thirdId = thirdId;
    }

    public void setFormalUser(Boolean isFormalUser) {
        this.isFormalUser = isFormalUser;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
    
    @Override
    public java.lang.String toString() {
        return "{email:" + email + ",passwd:" + passwd + "}";
    }
    
    public SimpleUser getSimpleUser() {
        SimpleUser simpleUser = new SimpleUser();
        simpleUser.setEmail(email);
        simpleUser.setNickName(nickName);
        simpleUser.setRole(role);
        return simpleUser;
    }
    
    private static class SimpleUser {
        private String email;
        private Integer role = 0;
        private String nickName;
        
        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;
        }
        public Integer getRole() {
            return role;
        }
        public void setRole(Integer role) {
            this.role = role;
        }
        public String getNickName() {
            return nickName;
        }
        public void setNickName(String nickName) {
            this.nickName = nickName;
        }
        
        
        
    }
    

}
