package binding;

import jdbc.autoconfig.datasource.DataSourceRegistration;
import jdbc.com.example.rds.Blog;
import jdbc.com.example.rds.Crm;
import jpa.blog.Post;
import jpa.blog.PostRepository;
import jpa.crm.Order;
import jpa.crm.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.VisibleConversionServiceDeducer;
import org.springframework.boot.context.properties.VisiblePropertySourcesDeducer;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.*;
import org.springframework.data.util.Streamable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.lang.annotation.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@EnableAutoConfiguration(exclude = {
	DataSourceAutoConfiguration.class,
	JpaRepositoriesAutoConfiguration.class,
	HibernateJpaAutoConfiguration.class
})
@SpringBootApplication
@EnableMDJ({"blog", "crm"})
@Log4j2
public class MDJ {

		@Bean
		ApplicationRunner runner(@Crm DataSourceRegistration crmDSR, @Crm DataSource crmDS, @Crm JdbcTemplate crmJT, @Crm EntityManagerFactory crmEMF, @Crm JpaTransactionManager crmTxManager, PostRepository pr,
																											@Blog DataSourceRegistration blogDSR, @Blog DataSource blogDS, @Blog JdbcTemplate blogJT, @Blog EntityManagerFactory blogEMF, @Blog JpaTransactionManager blogTxManager, OrderRepository or) {
				return args -> {

						Runnable crmRunnable = () -> {

								crmEMF
									.createEntityManager()
									.createQuery("select o from " + Order.class.getName() + " o", Order.class)
									.getResultList()
									.forEach(p -> log.info("order: " + ToStringBuilder.reflectionToString(p)));

								or.findAll().forEach(o -> log.info("order (JPA): " + ToStringBuilder.reflectionToString(o)));

						};
						log("CRM", crmDSR, crmDS, crmJT, crmEMF, crmTxManager, crmRunnable);

						Runnable blogRunnable = () -> {

								blogEMF
									.createEntityManager()
									.createQuery("select b from " + Post.class.getName() + " b", Post.class)
									.getResultList()
									.forEach(p -> log.info("post: " + ToStringBuilder.reflectionToString(p)));

								pr.findAll().forEach(p -> log.info("post (JPA): " + ToStringBuilder.reflectionToString(p)));

						};

						log("BLOG", blogDSR, blogDS, blogJT, blogEMF, blogTxManager, blogRunnable);

				};
		}

		private static void log(String label, DataSourceRegistration dsr, DataSource ds, JdbcTemplate jt, EntityManagerFactory emf, JpaTransactionManager txManager, Runnable r) {
				log.info("==============================================");
				log.info(label);
				log.info(ToStringBuilder.reflectionToString(dsr));
				log.info(ToStringBuilder.reflectionToString(ds));
				log.info(ToStringBuilder.reflectionToString(jt));
				log.info(ToStringBuilder.reflectionToString(emf));
				log.info(ToStringBuilder.reflectionToString(txManager));
				r.run();
				log.info(System.lineSeparator());
		}


		public static void main(String args[]) {
				SpringApplication.run(MDJ.class, args);
		}

}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(DynamicDataSourcePropertiesBindingPostProcessor.class)
@interface EnableMDJ {
		String[] value() default {};
}

@Log4j2
class DynamicDataSourcePropertiesBindingPostProcessor implements
	ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

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

		private static class BoundDataSourceSupplier implements Supplier<DataSource> {

				private final Supplier<DataSourceRegistration> dataSourceRegistration;

				private BoundDataSourceSupplier(Supplier<DataSourceRegistration> dataSourceRegistration) {
						this.dataSourceRegistration = dataSourceRegistration;
				}

				@Override
				public DataSource get() {
						return this.dataSourceRegistration
							.get()
							.getDataSource()
							.initializeDataSourceBuilder()
							.build();
				}
		}

		private static class JdbcTemplateSupplier implements Supplier<JdbcTemplate> {

				private final DefaultListableBeanFactory beanFactory;
				private final String beanName;

				JdbcTemplateSupplier(DefaultListableBeanFactory df, String bn) {
						this.beanFactory = df;
						this.beanName = bn;
				}

				@Override
				public JdbcTemplate get() {
						return new JdbcTemplate(this.beanFactory.getBean(this.beanName, DataSource.class));
				}
		}

		private static class BoundDataSourceRegistrationSupplier implements Supplier<DataSourceRegistration> {

				private final String label;
				private final Supplier<Binder> binder;

				private DataSourceRegistration dsr;

				private BoundDataSourceRegistrationSupplier(Supplier<Binder> binder, String label) {
						this.label = label;
						this.binder = binder;
				}

				@Override
				public DataSourceRegistration get() {
						if (this.dsr == null) {
								try {
										DataSourceRegistration dsr = new DataSourceRegistration();
										dsr.setBeanClassLoader(ClassLoader.getSystemClassLoader());
										dsr.setBeanName(this.label);
										this.doBinding(dsr, this.label);
										dsr.afterPropertiesSet();
										this.dsr = dsr;
								}
								catch (Exception e) {
										throw new RuntimeException(e);
								}
						}
						return this.dsr;
				}

				private void doBinding(DataSourceRegistration registration, String prefix) {
						ResolvableType resolvableType = ResolvableType.forClass(registration.getClass());
						Bindable<Object> target = Bindable
							.of(resolvableType)
							.withExistingValue(registration);
						binder.get().bind(prefix, target, null);
				}
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
				AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ApplicationContextHolder.class)
					.setRole(BeanDefinition.ROLE_SUPPORT)
					.getBeanDefinition();
				String name = ApplicationContextHolder.class.getName();
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
										ApplicationContextHolder applicationContextHolder = defaultListableBeanFactory.getBean(achBeanName, ApplicationContextHolder.class);
										ApplicationContext applicationContext = applicationContextHolder.applicationContext;
										this.binder = buildBinderFor(applicationContext);
								}
								return this.binder;
						}
				};

				/*importingClassMetadata
					.getAllAnnotationAttributes(EnableMDJ.class.getName())
					.get("value")
					.stream()
					.map(o -> (String[]) o)
					.flatMap(Stream::of)*/
//					.forEach(p -> registerForLabel(defaultListableBeanFactory, supplier, p, Package.getPackage()));

				// todo factor this out into a nested annotation set
				/*
				 @EnableMDJ (
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

		private static LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(
			BeanFactory registry,
			ResourceLoader resourceLoader,
			DataSource ds,
			String puName,
			Package pkg) {

				LocalContainerEntityManagerFactoryBean build = new EntityManagerFactoryBuilder(
					new HibernateJpaVendorAdapter(),
					Collections.emptyMap(), null)
					.dataSource(ds)
					.packages(pkg.getName())
					.persistenceUnit(puName)
					.build();
				build.setResourceLoader(resourceLoader);
				build.setBeanFactory(registry);
				build.setBeanClassLoader(ClassLoader.getSystemClassLoader());
				build.afterPropertiesSet();
				return build;
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

		private static class JpaTransactionManagerSupplier implements Supplier<JpaTransactionManager> {

				private final DefaultListableBeanFactory registry;
				private final String jpaEntityManagerFactoryBeanName;

				private JpaTransactionManagerSupplier(DefaultListableBeanFactory registry, String jpaEntityManagerFactoryBeanName) {
						this.jpaEntityManagerFactoryBeanName = jpaEntityManagerFactoryBeanName;
						this.registry = registry;
				}

				@Override
				public JpaTransactionManager get() {
						EntityManagerFactory emf = registry.getBean(jpaEntityManagerFactoryBeanName, EntityManagerFactory.class);
						return new JpaTransactionManager(emf);
				}
		}

		private static class EntityManagerFactorySupplier implements Supplier<EntityManagerFactory> {

				private final DefaultListableBeanFactory registry;
				private final ResourceLoader resourceLoader;
				private final String dataSourceBeanName;
				private final String packageName;

				private EntityManagerFactorySupplier(DefaultListableBeanFactory registry, ResourceLoader resourceLoader, String dataSourceBeanName, String packageName) {
						this.registry = registry;
						this.resourceLoader = resourceLoader;
						this.dataSourceBeanName = dataSourceBeanName;
						this.packageName = packageName;
				}

				@Override
				public EntityManagerFactory get() {
						return entityManagerFactoryBean(
							this.registry,
							this.resourceLoader,
							this.registry.getBean(dataSourceBeanName, DataSource.class),
							this.dataSourceBeanName + "PU",
							Package.getPackage(this.packageName))
							.getObject();
				}
		}


		private static class DynamicRepositoryConfigurationSourceSupport extends RepositoryConfigurationSourceSupport {

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
}
