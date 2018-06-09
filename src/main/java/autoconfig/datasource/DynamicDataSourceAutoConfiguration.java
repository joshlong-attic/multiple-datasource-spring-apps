package autoconfig.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.function.Supplier;


/***
	* Here's how this works, roughly:
	*
	* 1.) the code looks for all beans of {@link DataSourceRegistration}
	* 2.) it uses their bean name as a way of isolating related beans from others of a similar type using {@link Qualifier}
	* 3.) it manually registers a DataSource and a JdbcTemplate
	* 4.) if somebody wants to, they can create a typed `@Qualifier` annotation and meta-annotate it with the same String as used for the bean name.
	*
	* @author <a href="mailto:josh@joshlong.com"> Josh  Long </a>
	*
	*/
@Configuration

@Import(DynamicDataSourceAutoConfiguration.BeanRegistrar.class)
class DynamicDataSourceAutoConfiguration {

		abstract static class BaseSupplier<T> implements Supplier<T> {

				private final String dataSourceRegistrationBeanName, suppliedBeanName;
				private final BeanDefinitionRegistry registry;

				protected String getSuppliedBeanName() {
						return suppliedBeanName;
				}

				public BaseSupplier(String beanName, String sbn, BeanDefinitionRegistry registry) {
						this.dataSourceRegistrationBeanName = beanName;
						this.suppliedBeanName = sbn;
						this.registry = registry;
				}

				protected DefaultListableBeanFactory getDefaultListableBeanFactory() {
						Assert.isTrue(this.registry instanceof DefaultListableBeanFactory, "the registry must be a " + DefaultListableBeanFactory.class.getName() + "!");
						return DefaultListableBeanFactory.class.cast(this.registry);
				}

				protected String getDataSourceRegistrationBeanName() {
						return this.dataSourceRegistrationBeanName;
				}

				@Override
				public abstract T get();
		}

		static class DataSourceSupplier extends DynamicDataSourceAutoConfiguration.BaseSupplier<HikariDataSource> {

				public DataSourceSupplier(String beanName, String sbn, BeanDefinitionRegistry registry) {
						super(beanName, sbn, registry);
				}

				@SuppressWarnings("unchecked")
				private static <T> T createDataSource(DataSourceProperties properties, Class<? extends DataSource> type) {
						return (T) properties.initializeDataSourceBuilder().type(type).build();
				}

				private static HikariDataSource createHikariDataSource(DataSourceProperties dsp) {
						HikariDataSource dataSource = createDataSource(dsp, HikariDataSource.class);
						if (StringUtils.hasText(dsp.getName())) {
								dataSource.setPoolName(dsp.getName());
						}
						return dataSource;
				}

				@Override
				public HikariDataSource get() {
						DataSourceRegistration bean = getDefaultListableBeanFactory().getBean(getDataSourceRegistrationBeanName(), DataSourceRegistration.class);
						return createHikariDataSource(bean.getDataSource());
				}
		}

		static class JdbcTemplateSupplier extends DynamicDataSourceAutoConfiguration.BaseSupplier<JdbcTemplate> {

				public JdbcTemplateSupplier(String beanName, String sbn, BeanDefinitionRegistry registry) {
						super(beanName, sbn, registry);
				}

				@Override
				public JdbcTemplate get() {
						HikariDataSource bean = getDefaultListableBeanFactory().getBean(getDataSourceRegistrationBeanName() + "DataSource", HikariDataSource.class);
						return new JdbcTemplate(bean);
				}
		}

		static class BeanRegistrar implements ImportBeanDefinitionRegistrar {

				private Log log = LogFactory.getLog(getClass());

				private <T> void register(String dataSourceRegistrationBeanName,
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
								gdb.addQualifier(new AutowireCandidateQualifier(Qualifier.class, dataSourceRegistrationBeanName));
								beanDefinitionRegistry.registerBeanDefinition(newBeanName, gdb);
								log.info("adding qualifier '" + dataSourceRegistrationBeanName + "' for " + clzz.getName() + " instance.");
								log.info("registered bean " + newBeanName + ".");
						}
				}

				@Override
				public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
						Assert.isTrue(beanDefinitionRegistry instanceof DefaultListableBeanFactory, "must be an instance of " + DefaultListableBeanFactory.class.getName());
						DefaultListableBeanFactory context = DefaultListableBeanFactory.class.cast(beanDefinitionRegistry);
						String[] beanNamesForType = context.getBeanNamesForType(DataSourceRegistration.class, false, false);
						for (String beanName : beanNamesForType) {
								String dsName = beanName + "DataSource";
								String jdbcTpl = beanName + "JdbcTemplate";
								register(beanName, dsName, HikariDataSource.class, beanDefinitionRegistry, new DynamicDataSourceAutoConfiguration.DataSourceSupplier(beanName, dsName, beanDefinitionRegistry));
								register(beanName, jdbcTpl, JdbcTemplate.class, beanDefinitionRegistry, new DynamicDataSourceAutoConfiguration.JdbcTemplateSupplier(beanName, jdbcTpl, beanDefinitionRegistry));
						}
				}
		}
}