package xdb.jpa;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.data.repository.config.RepositoryConfigurationSourceSupport;
import org.springframework.data.util.Streamable;

import java.util.Optional;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
class DynamicRepositoryConfigurationSourceSupport extends RepositoryConfigurationSourceSupport {

		private final String pkg;
		private final String transactionManagerRef, entityManagerFactoryRef;

		DynamicRepositoryConfigurationSourceSupport(
			Environment environment,
			ClassLoader classLoader,
			BeanDefinitionRegistry registry,
			String pkg,
			String transactionManagerBeanName,
			String entityManagerFactoryBeanName) {
				super(environment, classLoader, registry);
				this.transactionManagerRef = transactionManagerBeanName;
				this.entityManagerFactoryRef = entityManagerFactoryBeanName;
				this.pkg = pkg;
		}

		@Override
		public Object getSource() {
				return null;
		}

		@Override
		public Streamable<String> getBasePackages() {
				return Streamable.of(this.pkg);
		}

		@Override
		public Optional<Object> getQueryLookupStrategyKey() {
				return Optional.empty();
		}

		@Override
		public Optional<String> getRepositoryImplementationPostfix() {
				return Optional.empty();
		}

		@Override
		public Optional<String> getNamedQueryLocation() {
				return Optional.empty();
		}

		@Override
		public Optional<String> getRepositoryBaseClassName() {
				return Optional.empty();
		}

		@Override
		public Optional<String> getRepositoryFactoryBeanClassName() {
				return Optional.empty();
		}

		@Override
		public Optional<String> getAttribute(String name) {

				if (name.equalsIgnoreCase("transactionManagerRef")) {
						return Optional.of(transactionManagerRef);
				}

				if (name.equalsIgnoreCase("entityManagerFactoryRef")) {
						return Optional.of(this.entityManagerFactoryRef);
				}

				return Optional.empty();
		}

		@Override
		public boolean usesExplicitFilters() {
				return false;
		}
}
