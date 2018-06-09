package com.example.rds;

import autoconfig.datasource.DataSourceRegistration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
@SpringBootApplication
public class RdsApplication {

		public static void main(String[] args) {
				SpringApplication.run(RdsApplication.class, args);
		}

		public static final String CRM = "crm";
		public static final String BLOG = "blog";

		@Bean(BLOG)
		@ConfigurationProperties(BLOG)
		DataSourceRegistration blog() {
				return new DataSourceRegistration();
		}

		@Bean(CRM)
		@ConfigurationProperties(CRM)
		DataSourceRegistration crm() {
				return new DataSourceRegistration();
		}
}


@Component
class RegistrationRunner {

		private final Log log = LogFactory.getLog(getClass());

		RegistrationRunner(@Blog DataSource blog, @Player DataSource player,
																					@Blog JdbcTemplate blogTemplate, @Player JdbcTemplate crmTemplate) {

				this.log.info("inside the " + RegistrationRunner.class.getName());

				blogTemplate.query("select * from post", rs -> {
						log.info("blogTemplate: the title is '" + rs.getString("title") +"'");
				});

				crmTemplate.query("select * from customers", rs -> {
						log.info("crmTemplate: the first_name is '" + rs.getString("first_name")+"'");
				});

		}
}