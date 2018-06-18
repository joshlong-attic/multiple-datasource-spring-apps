package xdb.jpa;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.context.properties.VisibleConversionServiceDeducer;
import org.springframework.boot.context.properties.VisiblePropertySourcesDeducer;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Log4j2
@Configuration
class MultipleJpaRegistrationImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

		private Environment environment;
		private ResourceLoader resourceLoader;

		@Override
		public void setEnvironment(Environment environment) {
				this.environment = environment;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
				this.resourceLoader = resourceLoader;
		}

		private Binder buildBinderFor(ApplicationContext applicationContext) {
				PropertySources propertySources = new VisiblePropertySourcesDeducer(applicationContext).getPropertySources();
				ConversionService bean = new VisibleConversionServiceDeducer(applicationContext).getConversionService();
				Iterable<ConfigurationPropertySource> from = ConfigurationPropertySources.from(propertySources);
				Consumer<PropertyEditorRegistry> editorRegistryConsumer = ((ConfigurableApplicationContext) applicationContext).getBeanFactory()::copyRegisteredEditorsTo;
				return new Binder(from, new PropertySourcesPlaceholdersResolver(propertySources), bean, editorRegistryConsumer);
		}

		protected <T> void register(String label,
																														String newBeanName,
																														Class<T> clzz,
																														BeanDefinitionRegistry beanDefinitionRegistry,
																														Supplier<T> supplier) {

				if (!beanDefinitionRegistry.containsBeanDefinition(newBeanName)) {
						GenericBeanDefinition gdb = new GenericBeanDefinition();
						gdb.setBeanClass(clzz);
						gdb.setSynthetic(true);
						gdb.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
						gdb.setInstanceSupplier(supplier);
						gdb.setLazyInit(true);
						gdb.addQualifier(new AutowireCandidateQualifier(Qualifier.class, label));
						beanDefinitionRegistry.registerBeanDefinition(newBeanName, gdb);
						log.debug("adding qualifier '" + label + "' for " + clzz.getName() + " instance.");
						log.debug("registered bean " + newBeanName + ".");
				}
		}

		// yuck. i do this only to have a lazily resolved pointer to the ApplicationContext. There has to be an easier way.
		private static class ApplicationContextHolder implements ApplicationContextAware {

				ApplicationContext applicationContext;

				@Override
				public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
						this.applicationContext = applicationContext;
				}
		}

		protected String registerApplicationContextHolderBean(BeanDefinitionRegistry registry) {

				String name = MultipleJpaRegistrationImportBeanDefinitionRegistrar.ApplicationContextHolder.class.getName();
				if (registry.containsBeanDefinition(name)) {
						return name;
				}

				AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
					.genericBeanDefinition(MultipleJpaRegistrationImportBeanDefinitionRegistrar.ApplicationContextHolder.class)
					.setRole(BeanDefinition.ROLE_SUPPORT)
					.getBeanDefinition();

				registry.registerBeanDefinition(name, beanDefinition);
				return name;
		}

		protected void registerJpaRepositories(
			String pkg, String transactionManagerBeanName, String entityManagerBeanName,
			BeanDefinitionRegistry beanDefinitionRegistry) {

				RepositoryConfigurationSourceSupport configurationSource = new DynamicRepositoryConfigurationSourceSupport(
					this.environment, getClass().getClassLoader(), beanDefinitionRegistry, pkg, transactionManagerBeanName, entityManagerBeanName);

				RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
				RepositoryConfigurationUtils.exposeRegistration(extension, beanDefinitionRegistry, configurationSource);

				extension.registerBeansForRoot(beanDefinitionRegistry, configurationSource);

				VisibleRepositoryBeanDefinitionBuilder builder = new VisibleRepositoryBeanDefinitionBuilder(beanDefinitionRegistry, extension, this.resourceLoader, this.environment);

				for (RepositoryConfiguration<? extends RepositoryConfigurationSource> configuration : extension.getRepositoryConfigurations(configurationSource, resourceLoader, true)) {

						BeanDefinitionBuilder definitionBuilder = builder.build(configuration);
						extension.postProcess(definitionBuilder, configurationSource);
						definitionBuilder.addPropertyValue("enableDefaultTransactions", false);

						AbstractBeanDefinition beanDefinition = definitionBuilder.getBeanDefinition();
						String beanName = configurationSource.generateBeanName(beanDefinition);

						log
							.debug("Spring Data {} - Registering repository: {} - Interface: {} - Factory: {}",
								extension.getModuleName(), beanName, configuration.getRepositoryInterface(), configuration.getRepositoryFactoryBeanClassName());

						beanDefinition.setAttribute("factoryBeanObjectType", configuration.getRepositoryInterface());
						beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
				}
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassAnnotationMetadata, BeanDefinitionRegistry registry) {

				DefaultListableBeanFactory defaultListableBeanFactory = DefaultListableBeanFactory.class.cast(registry);

				String applicationContextHolderBeanName = this.registerApplicationContextHolderBean(defaultListableBeanFactory);

				Supplier<Binder> supplier = new Supplier<Binder>() {

						private Binder binder;

						@Override
						public Binder get() {
								if (null == this.binder) {
										ApplicationContextHolder applicationContextHolder = defaultListableBeanFactory
											.getBean(applicationContextHolderBeanName, ApplicationContextHolder.class);
										ApplicationContext applicationContext = applicationContextHolder.applicationContext;
										this.binder = buildBinderFor(applicationContext);
								}
								return this.binder;
						}
				};

				String enableMultipleJpaRegistrationsAnnotationName = EnableMultipleJpaRegistrations.class.getName();

				importingClassAnnotationMetadata
					.getAllAnnotationAttributes(enableMultipleJpaRegistrationsAnnotationName)
					.get("value")
					.stream()
					.map(o -> (AnnotationAttributes[]) o)
					.flatMap(Stream::of)
					.forEach(jpaRegistration -> {

							String prefix = jpaRegistration.getString("prefix");
							Class<?> rootPackageClass = jpaRegistration.getClass("rootPackageClass");
							String rootPackage = jpaRegistration.getString("rootPackage");

							String resolvedPackage = (StringUtils.hasText(rootPackage)) ? rootPackage : rootPackageClass.getPackage().getName();

							Assert.hasText(resolvedPackage, "you must specify a root package when using " + enableMultipleJpaRegistrationsAnnotationName);
							this
								.registerJpaRegistrationsForPrefixAndPackage(
									defaultListableBeanFactory,
									supplier,
									prefix,
									resolvedPackage
								);
					});
		}

		protected void registerJpaRegistrationsForPrefixAndPackage(
			DefaultListableBeanFactory registry,
			Supplier<Binder> binderSupplier,
			String label,
			String packageName) {

				// DataSourceRegistration
				BoundDataSourceRegistrationSupplier dsrSupplier = new BoundDataSourceRegistrationSupplier(binderSupplier, label);
				this.register(label, label + DataSourceRegistration.class.getSimpleName(), DataSourceRegistration.class, registry, dsrSupplier);

				// DataSource
				String dataSourceBeanName = label + DataSource.class.getSimpleName();
				BoundDataSourceSupplier boundDataSourceSupplier = new BoundDataSourceSupplier(dsrSupplier);
				this.register(label, dataSourceBeanName, DataSource.class, registry, boundDataSourceSupplier);

				// JdbcTemplate
				this.register(label, label + JdbcTemplate.class.getSimpleName(), JdbcTemplate.class, registry, new JdbcTemplateSupplier(registry, dataSourceBeanName));

				// JPA EntityManagerFactory
				EntityManagerFactorySupplier emfSupplier = new EntityManagerFactorySupplier(registry, this.resourceLoader, dataSourceBeanName, packageName);
				Class<EntityManagerFactory> entityManagerFactoryClass = EntityManagerFactory.class;
				String jpaEntityManagerFactoryBeanName = label + entityManagerFactoryClass.getSimpleName();
				this.register(label, jpaEntityManagerFactoryBeanName, entityManagerFactoryClass, registry, emfSupplier);

				// JPA TransactionManager
				JpaTransactionManagerSupplier jpaTransactionManagerSupplier = new JpaTransactionManagerSupplier(registry, jpaEntityManagerFactoryBeanName);
				String jpaTransactionManagerBeanName = label + JpaTransactionManager.class.getName();
				this.register(label, jpaTransactionManagerBeanName, JpaTransactionManager.class, registry, jpaTransactionManagerSupplier);

				// Spring Data JPA
				this.registerJpaRepositories(packageName, jpaTransactionManagerBeanName, jpaEntityManagerFactoryBeanName, registry);
		}


}
