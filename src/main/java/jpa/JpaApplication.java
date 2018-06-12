package jpa;


import com.mysql.jdbc.Driver;
import jpa.blog.Post;
import jpa.blog.PostRepository;
import jpa.crm.Order;
import jpa.crm.OrderRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.*;
import org.springframework.data.util.Streamable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Import(JpaRegistrar.class)
@Configuration
public class JpaApplication {

		@Configuration
		public static class CrmConfig {

				private final static String QUALIFIER = "crm";

				@Bean
				@Qualifier(QUALIFIER)
				JpaTransactionManager crmTM(@Qualifier("crm") EntityManagerFactory emf) {
						return new JpaTransactionManager(emf);
				}

				@Bean
				@Qualifier(QUALIFIER)
				JdbcTemplate crmJT() {
						return new JdbcTemplate(crmDS());
				}

				@Bean
				@Qualifier(QUALIFIER)
				DataSource crmDS() {
						return ds(Driver.class, "jdbc:mysql://localhost:3306/crm?useSSL=false", "root", "root");
				}

				@Bean
				@Qualifier(QUALIFIER)
				LocalContainerEntityManagerFactoryBean crmEMF() {
						return emf(crmDS(), "crm", Order.class.getPackage());
				}
		}

		@Configuration
		public static class BlogConfig {

				private final static String QUALIFIER = "blog";

				@Bean
				@Qualifier(QUALIFIER)
				JpaTransactionManager blogTM(@Qualifier(QUALIFIER) EntityManagerFactory emf) {
						return new JpaTransactionManager(emf);
				}

				@Bean
				@Qualifier(QUALIFIER)
				JdbcTemplate blogJT() {
						return new JdbcTemplate(blogDS());
				}

				@Bean
				@Qualifier(QUALIFIER)
				DataSource blogDS() {
						return ds(Driver.class, "jdbc:mysql://localhost:3306/blog?useSSL=false", "root", "root");
				}

				@Bean
				@Qualifier(QUALIFIER)
				LocalContainerEntityManagerFactoryBean blogEMF() {
						return emf(blogDS(), "blog", Post.class.getPackage());
				}

		}

		private static LocalContainerEntityManagerFactoryBean emf(DataSource ds, String puName, Package pkg) {
				LocalContainerEntityManagerFactoryBean build = new EntityManagerFactoryBuilder(
					new HibernateJpaVendorAdapter(),
					Collections.emptyMap(), null)
					.dataSource(ds)
					.packages(pkg.getName())
					.persistenceUnit(puName)
					.build();
				return build;
		}

		private static DataSource ds(Class<? extends java.sql.Driver> d, String url, String u, String p) {
				return DataSourceBuilder
					.create()
					.driverClassName(d.getName())
					.password(u)
					.username(p)
					.url(url)
					.build();
		}

		private <T, I> void run(JpaRepository<T, I> r) {
				r.findAll().forEach(System.out::println);
		}

		@Bean
		ApplicationRunner runner(PostRepository pr, OrderRepository or) {
				return args -> {
						run(pr);
						run(or);
				};
		}

		public static void main(String[] args) {
				SpringApplication.run(JpaApplication.class, args);
		}

}

class DynamicRepositoryConfigurationSourceSupport extends RepositoryConfigurationSourceSupport {

		private final Package pkg;
		private final String transactionManagerRef, entityManagerFactoryRef;

		public DynamicRepositoryConfigurationSourceSupport(Environment environment,
																																																					ClassLoader classLoader, BeanDefinitionRegistry registry, Package pkg,
																																																					String transactionManagerRef, String emf) {
				super(environment, classLoader, registry);
				this.transactionManagerRef = transactionManagerRef;
				this.pkg = pkg;
				this.entityManagerFactoryRef = emf;
		}

		@Override
		public Object getSource() {
				return null;
		}

		@Override
		public Streamable<String> getBasePackages() {
				return Streamable.of(this.pkg.getName());
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

class JpaRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {


		private void register(Package pkg, String t, String e, AnnotationMetadata am, BeanDefinitionRegistry beanDefinitionRegistry) {

				RepositoryConfigurationSourceSupport configurationSource = new DynamicRepositoryConfigurationSourceSupport(
					this.environment, getClass().getClassLoader(), beanDefinitionRegistry, pkg, t, e);

				RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
				RepositoryConfigurationUtils.exposeRegistration(extension, beanDefinitionRegistry, configurationSource);

				extension.registerBeansForRoot(beanDefinitionRegistry, configurationSource);

				VisibleRepositoryBeanDefinitionBuilder builder = new VisibleRepositoryBeanDefinitionBuilder(beanDefinitionRegistry, extension, this.resourceLoader, this.environment);

				List<BeanComponentDefinition> definitions = new ArrayList<>();

				for (RepositoryConfiguration<? extends RepositoryConfigurationSource> configuration : extension.getRepositoryConfigurations(configurationSource, resourceLoader, true)) {

						BeanDefinitionBuilder definitionBuilder = builder.build(configuration);
						extension.postProcess(definitionBuilder, configurationSource);
						definitionBuilder.addPropertyValue("enableDefaultTransactions", false);

						AbstractBeanDefinition beanDefinition = definitionBuilder.getBeanDefinition();
						String beanName = configurationSource.generateBeanName(beanDefinition);

						LoggerFactory.getLogger(getClass()).debug(
							"Spring Data {} - Registering repository: {} - Interface: {} - Factory: {}",
							extension.getModuleName(), beanName,
							configuration.getRepositoryInterface(), configuration.getRepositoryFactoryBeanClassName());

						beanDefinition.setAttribute("factoryBeanObjectType", configuration.getRepositoryInterface());

						beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
						definitions.add(new BeanComponentDefinition(beanDefinition, beanName));
				}
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
				// todo we know that this works. how can we drive it from a @ConfigurationProperties instance?
				register(Order.class.getPackage(), "crmTM", "crmEMF", annotationMetadata, beanDefinitionRegistry);
				register(Post.class.getPackage(), "blogTM", "blogEMF", annotationMetadata, beanDefinitionRegistry);
		}

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
}