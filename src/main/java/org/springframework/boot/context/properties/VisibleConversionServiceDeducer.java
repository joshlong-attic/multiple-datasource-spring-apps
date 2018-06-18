package org.springframework.boot.context.properties;

import org.springframework.context.ApplicationContext;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public class VisibleConversionServiceDeducer extends ConversionServiceDeducer {

		public VisibleConversionServiceDeducer(ApplicationContext applicationContext) {
				super(applicationContext);
		}
}
