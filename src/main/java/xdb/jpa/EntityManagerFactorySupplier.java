package xdb.jpa;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.function.Supplier;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
class EntityManagerFactorySupplier implements Supplier<EntityManagerFactory> {

		private final DefaultListableBeanFactory registry;
		private final ResourceLoader resourceLoader;
		private final String dataSourceBeanName;
		private final String packageName;

		EntityManagerFactorySupplier(DefaultListableBeanFactory registry, ResourceLoader resourceLoader, String dataSourceBeanName, String packageName) {
				this.registry = registry;
				this.resourceLoader = resourceLoader;
				this.dataSourceBeanName = dataSourceBeanName;
				this.packageName = packageName;
		}

		LocalContainerEntityManagerFactoryBean createEntityManagerFactoryBean(
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

		@Override
		public EntityManagerFactory get() {
				return createEntityManagerFactoryBean(
					this.registry,
					this.resourceLoader,
					this.registry.getBean(dataSourceBeanName, DataSource.class),
					this.dataSourceBeanName + "PU",
					Package.getPackage(this.packageName))
					.getObject();
		}

}
