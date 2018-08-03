package org.dubbo.spring.boot.tigerz.gm.mail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.dubbo.spring.boot.tigerz.gm.entity.SellingHouse;

public class EmailTemplet {
    public static String userEmail = null;
    public static String agentEmail = null;

    // 读取用户模版
    public String readUserEmailTemplet() {
        if (userEmail == null) {
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("user_email.html");
            System.out.println(inputStream == null);
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                String detail = "";
                while ((detail = br.readLine()) != null) {
                    sb.append(detail + "\n");
                }
                br.close();
                userEmail = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return userEmail;
    }

    // 中介模版
    public String readAgentEmailTemplet() {
        if (agentEmail == null) {
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("agent_email.html");
            System.out.println(inputStream == null);
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                String detail = "";
                while ((detail = br.readLine()) != null) {
                    sb.append(detail + "\n");
                }
                br.close();
                agentEmail = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return agentEmail;
    }

    // 修改发送给用户邮件的模版
    public String ModifyUserEmailTemplet(String detail, String userName, String email, String href, String title,
            String message, SellingHouse house) {
        if (detail != null) {
            if (userName != null) {
                if (userName.trim().contains(" ")) {
                    userName = captureName(userName.split(" ")[0]);
                } else {
                    userName = captureName(userName);
                }
                detail = detail.replace("@userName", userName);
            } else {
                detail = detail.replace("@userName", email);
            }
            if (house != null) {
                detail = detail.replace("@mainPhoto", house.getHousePicMain());
                detail = detail.replace("@price", house.getPrice());
                detail = detail.replace("@address", house.getAddress());
                detail = detail.replace("@bed", house.getBeds() + "");
                detail = detail.replace("@bath", house.getBaths() + "");
                detail = detail.replace("@parking", house.getParking() + "");
            }
            if (title != null) {
                detail = detail.replace("@title", title);
            } else {
                detail = detail.replace("@title", "TigerZ");
            }
            if (href != null) {
                detail = detail.replace("@href", href);
            }
            if (message != null) {
                detail = detail.replace("@message", message);
            }
        }
        return detail;
    }

    // 修改发送给中介邮件的模版
    public String ModifyAgentEmailTemplet(String detail, String agentName, String agentEmail, 
            String userEmail, String userName, String userPhone,
            String href, String title, String message, SellingHouse house) {
        if (detail != null) {
            if (agentName != null) {
                if (agentName.trim().contains(" ")) {
                    agentName = captureName(agentName.split(" ")[0]);
                } else {
                    agentName = captureName(agentName);
                }
                detail = detail.replace("@agentName", agentName);
            } else {
                detail = detail.replace("@agentName", agentEmail);
            }
            if (title != null) {
                detail = detail.replace("@title", title);
            } else {
                detail = detail.replace("@title", "老虎买房");
            }
            if (house != null) {
                detail = detail.replace("@mainPhoto", house.getHousePicMain());
                detail = detail.replace("@price", house.getPrice());
                detail = detail.replace("@address", house.getAddress());
                detail = detail.replace("@bed", house.getBeds() + "");
                detail = detail.replace("@bath", house.getBaths() + "");
                detail = detail.replace("@parking", house.getParking() + "");
            }
            if (href != null) {
                detail = detail.replace("@href", href);
            }
            if (message != null) {
                detail = detail.replace("@message", message);
            }
            if (userEmail != null) {
                detail = detail.replace("@userEmail", userEmail);
            }
            if (userName != null) {
                detail = detail.replace("@userName", userName);
            }
            if (userPhone != null) {
                detail = detail.replace("@phone", userPhone);
            }
        }
        return detail;
    }

    public static String captureName(String name) {
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        return name;

    }
}
