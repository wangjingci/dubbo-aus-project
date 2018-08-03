package org.dubbo.spring.boot.tigerz.aus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SwaggerConfig {
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("org.dubbo.spring.boot.tigerz.aus.controller"))
                .paths(PathSelectors.any())
                .build();
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("澳大利亚房产数据接口文档")
                .description("Restfun风格接口，www.tigerz.com\n注意:0-Country 1-State 2-Region 3-Area 4-Suburb 5-House")
                .termsOfServiceUrl("http://www.tigerz.com")
                .version("1.0")
                .build();
    }
}
