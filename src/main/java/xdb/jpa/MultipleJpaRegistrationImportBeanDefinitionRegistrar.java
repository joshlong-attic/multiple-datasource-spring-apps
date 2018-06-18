package xdb.jpa;

import demo.blog.Post;
import demo.crm.Order;
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
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Log4j2
@Configuration
class MultipleJpaRegistrationImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

		private Environment environment;
		private ResourceLoader resourceLoader;

		private <T> void register(String label,
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

		// yuck. i do this only to have a lazily resolved pointer to the ApplicationContext. There has to be an easier way.
		private static class ApplicationContextHolder implements ApplicationContextAware {

				ApplicationContext applicationContext;

				@Override
				public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
						this.applicationContext = applicationContext;
				}
		}

		private String registerApplicationContextHolderBean(BeanDefinitionRegistry registry) {

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

		private void registerJpaRepositories(
			String pkg, String transactionManagerBeanName, String entityManagerBeanName,
			BeanDefinitionRegistry beanDefinitionRegistry) {

				RepositoryConfigurationSourceSupport configurationSource = new DynamicRepositoryConfigurationSourceSupport(
					this.environment, getClass().getClassLoader(), beanDefinitionRegistry, pkg, transactionManagerBeanName, entityManagerBeanName);

				RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
				RepositoryConfigurationUtils.exposeRegistration(extension, beanDefinitionRegistry, configurationSource);

				extension.registerBeansForRoot(beanDefinitionRegistry, configurationSource);

				VisibleRepositoryBeanDefinitionBuilder builder = new VisibleRepositoryBeanDefinitionBuilder(beanDefinitionRegistry, extension, this.resourceLoader, this.environment);

//				List<BeanComponentDefinition> definitions = new ArrayList<>();

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
//						definitions.add(new BeanComponentDefinition(beanDefinition, beanName));
				}
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

				DefaultListableBeanFactory defaultListableBeanFactory = DefaultListableBeanFactory.class.cast(registry);
				String achBeanName = this.registerApplicationContextHolderBean(defaultListableBeanFactory);
				Supplier<Binder> supplier = new Supplier<Binder>() {

						private Binder binder;

						@Override
						public Binder get() {
								if (null == this.binder) {
										MultipleJpaRegistrationImportBeanDefinitionRegistrar.ApplicationContextHolder applicationContextHolder = defaultListableBeanFactory.getBean(achBeanName, MultipleJpaRegistrationImportBeanDefinitionRegistrar.ApplicationContextHolder.class);
										ApplicationContext applicationContext = applicationContextHolder.applicationContext;
										this.binder = buildBinderFor(applicationContext);
								}
								return this.binder;
						}
				};

				/*importingClassMetadata
					.getAllAnnotationAttributes(EnableMultipleJpaRegistrations.class.getName())
					.get("value")
					.stream()
					.map(o -> (String[]) o)
					.flatMap(Stream::of)*/
//					.forEach(p -> registerForLabel(defaultListableBeanFactory, supplier, p, Package.getPackage()));

				// todo factor this out into a nested annotation set
				/*
				 @EnableMultipleJpaRegistrations (
				 	{
				 		 @MDJ("crm", pkg.for.crm.Order.class)},
				 	 	@MDJ("blog", pkg.for.blog.Post.class)
						}
					)
				 */
				Map<String, Package> registerMetaData = new HashMap<>();
				registerMetaData.put("blog", Post.class.getPackage());
				registerMetaData.put("crm", Order.class.getPackage());
				registerMetaData.forEach((prefix, pkg) -> this.registerForLabel(defaultListableBeanFactory, supplier, prefix, pkg.getName()));
		}

		private void registerForLabel(DefaultListableBeanFactory registry,
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
