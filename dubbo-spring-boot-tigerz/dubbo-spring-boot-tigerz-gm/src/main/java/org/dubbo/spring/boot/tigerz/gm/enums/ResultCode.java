package org.dubbo.spring.boot.tigerz.gm.enums;

public enum ResultCode {
    SUCCESS(0,"请求成功"),
    PARAMETER_ERROR(1001, "Wrong Parameter"),
    PASSWORD_ERROR(1002, "Wrong Password"),
    CREATE_USER_FAIL(1003, "Fail to create user"),
    USER_EXIST(1004, "The user has already existed"),
    USER_NOT_EXIST(1005, "User doesn't exist"),
    UPDATE_USER_FAIL(1006, "Fail to update user info");
    
    private int code;
    private String msg;
    private ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    
    public int getCode() {
        return this.code;
    }
    
    public String getMsg() {
        return this.msg;
    }

}
