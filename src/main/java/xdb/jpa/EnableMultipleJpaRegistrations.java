package xdb.jpa;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
	*
	* Registers a {@link javax.sql.DataSource}, {@link org.springframework.jdbc.core.JdbcTemplate}, {@link DataSourceRegistration},
	* {@link org.springframework.orm.jpa.JpaTransactionManager}, {@link javax.persistence.EntityManagerFactory},
	* and Spring Data JPA repository support for the specific package.
	*
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(MultipleJpaRegistrationImportBeanDefinitionRegistrar.class)
public @interface EnableMultipleJpaRegistrations {
		JpaRegistration[] value();
}
