package org.springframework.boot.context.properties;

import org.springframework.context.ApplicationContext;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public class VisiblePropertySourcesDeducer extends	PropertySourcesDeducer {

		public VisiblePropertySourcesDeducer(ApplicationContext applicationContext) {
				super(applicationContext);
		}
}
