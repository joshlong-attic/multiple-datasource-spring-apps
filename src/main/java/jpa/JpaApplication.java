package jpa;


import com.mysql.jdbc.Driver;
import jpa.blog.Post;
import jpa.blog.PostRepository;
import jpa.crm.Order;
import jpa.crm.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
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


@Log4j2
class JpaRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {



		@Override
		public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {

				// todo we know that this works. how can we drive it from a
				// todo @ConfigurationProperties instance?

				// it would be nice to be able to start with crm.datasource.username, password, driver = ... and blog.datasource.username, password, driver,
				// and arrive automatically at: auto-registered JpaRepositories, JdbcTemplate, DataSource, JpaTransactionManagers, etc.

//				this.register(Order.class.getPackage(), "crmTM", "crmEMF", annotationMetadata, beanDefinitionRegistry);
//				this.register(Post.class.getPackage(), "blogTM", "blogEMF", annotationMetadata, beanDefinitionRegistry);
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