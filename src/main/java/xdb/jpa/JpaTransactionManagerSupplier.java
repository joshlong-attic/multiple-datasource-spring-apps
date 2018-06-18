package xdb.jpa;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.persistence.EntityManagerFactory;
import java.util.function.Supplier;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
class JpaTransactionManagerSupplier implements Supplier<JpaTransactionManager> {

		private final DefaultListableBeanFactory registry;
		private final String jpaEntityManagerFactoryBeanName;

		JpaTransactionManagerSupplier(DefaultListableBeanFactory registry, String jpaEntityManagerFactoryBeanName) {
				this.jpaEntityManagerFactoryBeanName = jpaEntityManagerFactoryBeanName;
				this.registry = registry;
		}

		@Override
		public JpaTransactionManager get() {
				EntityManagerFactory emf = registry.getBean(this.jpaEntityManagerFactoryBeanName, EntityManagerFactory.class);
				return new JpaTransactionManager(emf);
		}
}
