package org.springframework.boot.context.properties;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public class VisibleConversionServiceDeducer extends	ConversionServiceDeducer {
		public VisibleConversionServiceDeducer(ApplicationContext applicationContext) {
				super(applicationContext);
		}

}
