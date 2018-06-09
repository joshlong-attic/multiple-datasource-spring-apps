package com.example.rds;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
class RegistrationRunner {

		private final Log log = LogFactory.getLog(getClass());

		RegistrationRunner(@Blog DataSource blog, @Crm DataSource player,
																					@Blog JdbcTemplate blogTemplate, @Crm JdbcTemplate crmTemplate) {

				this.log.info("inside the " + RegistrationRunner.class.getName());

				blogTemplate.query("select * from post", rs -> {
						log.info("blogTemplate: the title is '" + rs.getString("title") + "'");
				});

				crmTemplate.query("select * from customers", rs -> {
						log.info("crmTemplate: the first_name is '" + rs.getString("first_name") + "'");
				});
		}
}
