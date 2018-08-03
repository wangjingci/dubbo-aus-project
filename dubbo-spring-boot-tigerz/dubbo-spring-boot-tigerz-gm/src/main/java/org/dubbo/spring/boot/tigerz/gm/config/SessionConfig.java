package org.dubbo.spring.boot.tigerz.gm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Spring Session 设置 跨域等 SessionConfig
 * 
 * @Desc:
 * @Company: TigerZ
 * @author Wang Jingci
 * @date 2018年5月25日 下午3:53:46
 */

@Configuration
public class SessionConfig {
    @Bean
    public DefaultCookieSerializer defaultCookieSerializer() {
        
        DefaultCookieSerializer defaultCookieSerializer = new DefaultCookieSerializer();
        defaultCookieSerializer.setCookieName("JSESSION"); // session
        defaultCookieSerializer.setCookiePath("/"); 
        defaultCookieSerializer.setDomainName("tigerz.nz"); // 指定顶级域名
        return defaultCookieSerializer;
    }

}
