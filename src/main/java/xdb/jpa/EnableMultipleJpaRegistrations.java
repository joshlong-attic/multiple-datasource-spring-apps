package xdb.jpa;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(MultipleJpaRegistrationImportBeanDefinitionRegistrar.class)
public @interface EnableMultipleJpaRegistrations {
		String[] value() default {};
}
