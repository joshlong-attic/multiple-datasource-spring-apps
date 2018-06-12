package org.springframework.data.repository.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;


public class VisibleRepositoryBeanDefinitionBuilder extends RepositoryBeanDefinitionBuilder {

		public VisibleRepositoryBeanDefinitionBuilder(BeanDefinitionRegistry registry,
																																																RepositoryConfigurationExtension extension,
																																																ResourceLoader resourceLoader,
																																																Environment environment) {
				super(registry, extension, resourceLoader, environment);
		}
}
