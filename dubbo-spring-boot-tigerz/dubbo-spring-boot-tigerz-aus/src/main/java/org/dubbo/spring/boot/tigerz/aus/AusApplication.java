package org.dubbo.spring.boot.tigerz.aus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3*365*24*60*60)
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class})
public class AusApplication {
    public static void main(String[] args) {
        SpringApplication.run(AusApplication.class,args);
    }
}
