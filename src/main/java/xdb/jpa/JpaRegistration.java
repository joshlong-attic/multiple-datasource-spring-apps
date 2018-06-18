package xdb.jpa;

/**
	* Specific information for a given JPA-centric configuration.
	*
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public @interface JpaRegistration {

		/**
			* what Environment configuration property prefix should we apply in looking for the properties to apply
			* to a {@link DataSourceRegistration}.
			* <p>
			* If you used {@code foo}, then this would imply the presence of {@code foo.datasource.url=..}, {@code foo.datasource.password=..}, etc.,
			* in the Spring Framework {@link org.springframework.core.env.Environment}.
			*/
		String prefix();

		/**
			* Use to specify the root package against which this JPA configuration should search
			*/
		String rootPackage() default "";

		/**
			* Use this to specify a type whose package will be used to the exclusion of {@code rootPackage}
			*/
		Class<?> rootPackageClass() ;
}
