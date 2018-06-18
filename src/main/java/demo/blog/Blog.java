package demo.blog;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.*;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Qualifier("blog")
public @interface Blog {
}
