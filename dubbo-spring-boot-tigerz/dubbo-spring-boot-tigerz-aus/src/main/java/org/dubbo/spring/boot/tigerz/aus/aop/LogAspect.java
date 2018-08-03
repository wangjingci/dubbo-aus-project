package org.dubbo.spring.boot.tigerz.aus.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import java.lang.reflect.Field;  

@Component
@Aspect
public class LogAspect {
    
    /**
     * 比较值得借鉴的是通过一个类，就可以获得这里的所有属性名及属性值
     */
    private static String[] types = { "java.lang.Integer", "java.lang.Double",  
            "java.lang.Float", "java.lang.Long", "java.lang.Short",  
            "java.lang.Byte", "java.lang.Boolean", "java.lang.Char",  
            "java.lang.String", "int", "double", "long", "short", "byte",  
            "boolean", "char", "float", "java.util.HashMap", "java.util.ArrayList" };  
    
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LogAspect.class);
    
    /**
     * 定义一个切入点. 
     * 解释下：
     *
     * ~ 第一个 * 代表任意修饰符及任意返回值.
     * ~ 第二个 * 定义在web包或者子包,类似basePackage，就是寻找要处理的类
     * ~ 第三个 * 任意方法
     * ~ .. 匹配任意数量的参数.
     */
     @Pointcut("execution(* org.dubbo.spring.boot.tigerz.aus.service..*.*(..))")
     
     public void logPointcut(){}
     
     @org.aspectj.lang.annotation.Around("logPointcut()")
     public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable{
        long startTime = System.currentTimeMillis();
        Object result = null;  
        
        try {  
            // 经过测试，proceed里面不需要加参数，估计代表所有参数
            result = joinPoint.proceed();  
        } catch (Throwable e) {  
            String classType = joinPoint.getTarget().getClass().getName();  
            Class<?> clazz = Class.forName(classType);  
            String clazzName = clazz.getName();  
            
            // 方法简写名称
            String methodName2 = joinPoint.getSignature().getName();  
            String[] paramNames = getFieldsName(this.getClass(), clazzName, methodName2);  
            String logContent = writeLogInfo(paramNames, joinPoint);  
            
            logger.error("AOP统计方法-"+ methodName2 + "param:" + logContent + "-耗时过程发生错误", e);  
            throw e;
        } 
        
        // 获取执行的方法名  
//        long endTime = System.currentTimeMillis();  
//        MethodSignature signature = (MethodSignature) joinPoint.getSignature();  
//        // 方法全名
//        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();  
//
//        long diffTime = endTime - startTime;  
//        logger.warn("===>" + methodName + " 方法执行耗时：" + diffTime + " ms");  
        
        String classType = joinPoint.getTarget().getClass().getName();  
        Class<?> clazz = Class.forName(classType);  
        String clazzName = clazz.getName();  
        
        // 方法简写名称
        String methodName2 = joinPoint.getSignature().getName();  
        String[] paramNames = getFieldsName(this.getClass(), clazzName, methodName2);  
        String logContent = writeLogInfo(paramNames, joinPoint);
        long endTime = System.currentTimeMillis();  
        long diffTime = endTime - startTime;  
        logger.info("执行方法=>clazzName: "+clazzName+", methodName:"+methodName2+", param:"+ logContent + ", 耗时:" + diffTime + "毫秒");  
        
        
        return result;
        
     }
     
     private static String writeLogInfo(String[] paramNames, JoinPoint joinPoint){  
         Object[] args = joinPoint.getArgs();  
         StringBuilder sb = new StringBuilder();  
         boolean clazzFlag = true;  
         for(int k=0; k<args.length; k++){  
             Object arg = args[k];  
             if (k < paramNames.length) {
                 sb.append(paramNames[k]+" ");  
             }
             // 获取对象类型  
             String typeName = "unknown";
             if (arg != null) {
                 typeName = arg.getClass().getTypeName(); 
             }
               
             for (String t : types) {  
                 if (t.equals(typeName)) {  
                     sb.append("=" + arg+"; ");  
                 }  
             }  
             if (clazzFlag) {  
                 sb.append(getFieldsValue(arg));  
             }  
         }  
         return sb.toString();  
     }  
     
     /** 
      * 得到方法参数的名称 
      * @param cls 
      * @param clazzName 
      * @param methodName 
      * @return 
      * @throws NotFoundException 
      */  
     private static String[] getFieldsName(Class<?> cls, String clazzName, String methodName) throws NotFoundException{  
         ClassPool pool = ClassPool.getDefault();  
         //ClassClassPath classPath = new ClassClassPath(this.getClass());  
         ClassClassPath classPath = new ClassClassPath(cls);  
         pool.insertClassPath(classPath);  
           
         CtClass cc = pool.get(clazzName);  
         CtMethod cm = cc.getDeclaredMethod(methodName);  
         MethodInfo methodInfo = cm.getMethodInfo();  
         CodeAttribute codeAttribute = methodInfo.getCodeAttribute();  
         LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);  
         if (attr == null) {  
             // exception  
         }  
         String[] paramNames = new String[cm.getParameterTypes().length];  
         int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;  
         for (int i = 0; i < paramNames.length; i++){  
             paramNames[i] = attr.variableName(i + pos); //paramNames即参数名  
         }  
         return paramNames;  
     }  
     
     /** 
      * 得到参数的值 
      * @param obj 
      */  
     public static String getFieldsValue(Object obj) {  
         if (obj == null) return "";
         Field[] fields = obj.getClass().getDeclaredFields();  
    
         String typeName = obj.getClass().getTypeName();  
         for (String t : types) {  
             if(t.equals(typeName))  
                 return "";  
         }  
         StringBuilder sb = new StringBuilder();  
         sb.append("【");  
         for (Field f : fields) {  
             f.setAccessible(true);  
             try {  
                 for (String str : types) {  
                     if (f.getType().getName().equals(str)){  
                         sb.append(f.getName() + " = " + f.get(obj)+"; ");  
                     }  
                 }  
             } catch (IllegalArgumentException e) {  
                 e.printStackTrace();  
             } catch (IllegalAccessException e) {  
                 e.printStackTrace();  
             }  
         }  
         sb.append("】");  
         return sb.toString();  
     }  
    
    
}
