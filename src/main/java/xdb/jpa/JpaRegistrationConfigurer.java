package xdb.jpa;

import javax.sql.DataSource;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public interface JpaRegistrationConfigurer {

		DataSource getDataSourceFor(String prefix);
}
