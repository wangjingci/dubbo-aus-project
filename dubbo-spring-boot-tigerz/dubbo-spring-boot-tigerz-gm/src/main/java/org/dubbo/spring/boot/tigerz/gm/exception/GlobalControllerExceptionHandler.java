package org.dubbo.spring.boot.tigerz.gm.exception;

import org.dubbo.spring.boot.tigerz.gm.dto.StandardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
* 定义全局异常处理
* 所有Controller抛出来的异常都会在这里被处理
* @author 
* @RestControllerAdvice 是@controlleradvice 与@ResponseBody 的组合注解
*/
@ControllerAdvice
@ResponseBody
public class GlobalControllerExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);
    
    @ExceptionHandler(value = { IllegalArgumentException.class })
    public StandardResponse handleIllegalArgumenException(IllegalArgumentException e) {
        logger.warn("总异常处理器(警告类)：",e);
        StandardResponse sr = new StandardResponse();
        sr.setIsOK(false);
        sr.setErrorMessage(e.getMessage());
        return sr;
    }
    
    @ExceptionHandler(value = { Exception.class })
    public StandardResponse handleException(Exception e) {
        logger.error("总异常处理器(警告类)：",e);
        StandardResponse sr = new StandardResponse();
        sr.setIsOK(false);
        sr.setErrorMessage(e.getMessage());
        return sr;
    }
    
    
    
    
}
