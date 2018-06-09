package com.example.rds;

import autoconfig.datasource.DataSourceRegistration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
@SpringBootApplication
public class RdsApplication {

		public static void main(String[] args) {
				SpringApplication.run(RdsApplication.class, args);
		}

		@Bean("blog")
		@ConfigurationProperties("blog")
		DataSourceRegistration blog() {
				return new DataSourceRegistration();
		}

		@Bean("crm")
		@ConfigurationProperties("crm")
		DataSourceRegistration crm() {
				return new DataSourceRegistration();
		}
}
