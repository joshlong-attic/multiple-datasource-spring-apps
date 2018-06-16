package org.springframework.boot.context.properties;

import org.springframework.context.ApplicationContext;

/**
	* YUCK!
	*
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public class VisibleConfigurationPropertiesBinder extends ConfigurationPropertiesBinder {

		public VisibleConfigurationPropertiesBinder(ApplicationContext applicationContext, String validatorBeanName) {
				super(applicationContext, validatorBeanName);
		}
}
