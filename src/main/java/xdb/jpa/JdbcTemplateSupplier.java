package xdb.jpa;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.function.Supplier;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
class JdbcTemplateSupplier implements Supplier<JdbcTemplate> {

		private final DefaultListableBeanFactory beanFactory;
		private final String beanName;

		JdbcTemplateSupplier(DefaultListableBeanFactory df, String bn) {
				this.beanFactory = df;
				this.beanName = bn;
		}

		@Override
		public JdbcTemplate get() {
				return new JdbcTemplate(this.beanFactory.getBean(this.beanName, DataSource.class));
		}
}
