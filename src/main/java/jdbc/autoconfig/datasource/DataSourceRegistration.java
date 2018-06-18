package jdbc.autoconfig.datasource;

import lombok.Data;
import lombok.extern.java.Log;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;

@Log
@Data
public class DataSourceRegistration implements BeanNameAware, BeanClassLoaderAware, InitializingBean {

		@NestedConfigurationProperty
		private final DataSourceProperties dataSource;

		private String beanName;

		public DataSourceRegistration() {
				this.dataSource = new DataSourceProperties();
		}

		@Override
		public void setBeanName(String name) {
				this.beanName = name;
		}

		public String toString() {
				return this.beanName;
		}

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
				this.dataSource.setBeanClassLoader(classLoader);
		}

		@Override
		public void afterPropertiesSet() throws Exception {
				this.dataSource.afterPropertiesSet();
		}
}
