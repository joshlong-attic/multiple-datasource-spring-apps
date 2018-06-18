package binding;

import jdbc.autoconfig.datasource.DataSourceRegistration;
import jdbc.com.example.rds.Blog;
import jdbc.com.example.rds.Crm;
import jpa.blog.Post;
import jpa.crm.Order;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistry;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySources;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.lang.annotation.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
		ApplicationRunner runner(@Crm DataSourceRegistration crmDSR, @Crm DataSource crmDS, @Crm JdbcTemplate crmJT,
																											@Blog DataSourceRegistration blogDSR, @Blog DataSource blogDS, @Blog JdbcTemplate blogJT) {
				return args -> {
						log("CRM", crmDSR, crmDS, crmJT);
						log("BLOG", blogDSR, blogDS, blogJT);
				};
		}

		private static void log(String label, DataSourceRegistration dsr, DataSource ds, JdbcTemplate jt) {
				log.info("==============================================");
				log.info(label);
				log.info(ToStringBuilder.reflectionToString(dsr));
				log.info(ToStringBuilder.reflectionToString(ds));
				log.info(ToStringBuilder.reflectionToString(jt));
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
class DynamicDataSourcePropertiesBindingPostProcessor implements ImportBeanDefinitionRegistrar {

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
						return new JdbcTemplate(beanFactory.getBean(this.beanName, DataSource.class));
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

		private static class ACH implements ApplicationContextAware {

				ApplicationContext applicationContext;

				@Override
				public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
						this.applicationContext = applicationContext;
				}
		}

		private String registerAchBean(BeanDefinitionRegistry registry) {
				AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ACH.class)
					.setRole(BeanDefinition.ROLE_SUPPORT)
					.getBeanDefinition();
				String name = ACH.class.getName();
				registry.registerBeanDefinition(name, beanDefinition);
				return name;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {


				DefaultListableBeanFactory defaultListableBeanFactory = DefaultListableBeanFactory.class.cast(registry);

				String achBeanName = this.registerAchBean(defaultListableBeanFactory);

				Supplier<Binder> supplier = new Supplier<Binder>() {

						private Binder binder;

						@Override
						public Binder get() {
								if (null == this.binder) {
										ACH ach = defaultListableBeanFactory.getBean(achBeanName, ACH.class);
										ApplicationContext applicationContext = ach.applicationContext;
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
				registerMetaData.forEach((prefix, pkg) -> registerForLabel(defaultListableBeanFactory, supplier, prefix, pkg.getName()));


		}

		private static LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(DataSource ds, String puName, Package pkg) {
				return new EntityManagerFactoryBuilder(
					new HibernateJpaVendorAdapter(),
					Collections.emptyMap(), null)
					.dataSource(ds)
					.packages(pkg.getName())
					.persistenceUnit(puName)
					.build();
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
				this.register(label, label + JdbcTemplate.class.getSimpleName(), JdbcTemplate.class, registry, new JdbcTemplateSupplier(DefaultListableBeanFactory.class.cast(registry), dataSourceBeanName));

				// JPA EntityManager

				log.info("package name: " + packageName);
		}
}