package com.concert.ticketing.common.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * Knife4j / Swagger 配置类
 * 提供 API 文档和在线调试界面
 *
 * @author Concert Ticketing Team
 */
@Configuration
@EnableSwagger2WebMvc
@EnableKnife4j
public class SwaggerConfig {

    /**
     * 创建 API 文档配置
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 扫描 Controller 包
                .apis(RequestHandlerSelectors.basePackage("com.concert.ticketing"))
                // 所有路径
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * API 文档基本信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("演唱会票务预订与VIP服务平台 API 文档")
                .description("大型演唱会票务预订与VIP服务平台 - 后端接口文档")
                .contact(new Contact("Concert Ticketing Team", "", ""))
                .version("1.0.0")
                .build();
    }
}
