package org.dubbo.spring.boot.tigerz.gm.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.dubbo.spring.boot.tigerz.gm.entity.SellingHouse;
import org.dubbo.spring.boot.tigerz.gm.mail.EmailTemplet;
import org.dubbo.spring.boot.tigerz.gm.mail.MailSendSuccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class FeedbackService {
    
    @Autowired
    SellingHouseService sellingHouseService;
    
    private ExecutorService executorService = java.util.concurrent.Executors.newFixedThreadPool(2);

    /**
     * 汇报数据错误问题，前端必须发来是哪个房源
     * 直接发送给产品经理，曹亮等人
     * 不需要任何数据格式
     * 
     */
    public void feedback(Map<String, Object> data) {
        executorService.submit(() -> feedbackReport(data));
    }
    
    private void feedbackReport(Map<String, Object> data) {
        String type = (String)data.get("type");
        if (org.apache.commons.lang3.StringUtils.equals(type, "house")) {
            consultPropReport(data);
        } else {
            sendFeedback(data);
        }
    }
    
    /**
     * 转发用户的咨询邮件，既要发给中介，用户，也要发给我们自己，关键是要有清晰的格式
     * @param data
     */
    private void consultPropReport(Map<String, Object> data) {
        //String type = (String)data.get("type");
        String content = (String)data.get("content");
        String email = (String)data.get("email");
        String phone = (String)data.get("phone");
        String userName = (String)data.get("name");
        String countryCode = (String)data.get("countryCode");
        String fromPropId = (String)data.get("fromPropId");
        
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> agents = (List<Map<String,Object>>)data.get("agents");
        
        
        com.tigerz.easymongo.util.Assert.notNull(fromPropId, "property id should not be null");
        if (agents == null || agents.size() == 0) {
            throw new IllegalArgumentException("中介数量要在1个及以上");
        }
        
        EmailTemplet emailTemplet = new EmailTemplet();
        String detail = emailTemplet.readUserEmailTemplet();
        
        SellingHouse house = sellingHouseService.getAusSellingHouse(fromPropId, "en");
        com.tigerz.easymongo.util.Assert.notNull(house, "sellinghouse should not be null");
        String href =  "http://www.tigerz.com/detail-" + fromPropId;
        detail = emailTemplet.ModifyUserEmailTemplet(detail, userName, email, href, house.getAddress(), content,house);
        
        // 给用户发送邮件  
        boolean isUserSend = MailSendSuccess.sendEmail("Your enquiring mail has been sent to agent", detail, email);
        if (isUserSend) {
            System.out.println("成功发送邮件给用户->" + email);
        } else {
            System.out.println("给用户发送邮件失败->" + email);
        }
        
        // 给中介发送邮件
        String agentDesc = emailTemplet.readAgentEmailTemplet();
        for (Map<String,Object> agent : agents) {
            String agentMail = (String)agent.get("agentMail");
            String agentName = (String)agent.get("agentName");
            String templet = emailTemplet.ModifyAgentEmailTemplet(agentDesc, agentName, agentMail,email,userName,phone, href, house.getAddress(), content, house);
            boolean isAgentSend = MailSendSuccess.sendEmail("A customer from TigerZ.com send you a mail to enquire property", templet, agentMail);
            if (isAgentSend) {
                // TODO 在数据库里记录所有的邮件发送事件
                System.out.println("成功发送邮件给中介->" + agentMail);
            } else {
                System.out.println("给中介发送邮件失败->" + agentMail);
            }
        }


    }
    
    /**
     * 接收用户反馈，有的需要立刻回，有的知道就好
     * @param data
     */
    private void sendFeedback(Map<String, Object> data) {
         String type = (String)data.get("type");
         String content = (String)data.get("content");
         String email = (String)data.get("email");
         String phone = (String)data.get("phone");
         String countryCode = (String)data.get("countryCode");
         String fromPropId = (String)data.get("fromPropId");
         
         StringBuffer sb = new StringBuffer();
         sb.append("<br>反馈类型：" + type + "</br>");
         sb.append("<br>用户手机：" + phone + "</br>");
         sb.append("<br>用户邮箱：" + email + "</br>");
         sb.append("<br>房源：" + fromPropId + "</br>");
         sb.append("<br>内容：" + content + "</br>");
         String title = MailSendSuccess.refFormatNowDateForMin(System.currentTimeMillis())+" 用户反馈信息";
         title += "——类型:" + type + "| 国家:" + countryCode;
         MailSendSuccess.sendFeedBackSucess(title, sb.toString());
    }
}
