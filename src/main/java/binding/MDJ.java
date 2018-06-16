package binding;

import jdbc.autoconfig.datasource.DataSourceRegistration;
import jdbc.com.example.rds.Blog;
import jdbc.com.example.rds.Crm;
import lombok.extern.java.Log;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.ApplicationArguments;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySources;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.annotation.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
public class MDJ {


		@Log
		@Component
		public static class DataSourceRegistrationRunner implements ApplicationRunner {


				private DataSourceRegistration crmDSR, blogDSR;

				private DataSource crmDS, blogDS;

				public DataSourceRegistrationRunner(@Crm DataSourceRegistration crmDSR, @Crm DataSource crmDS,
																																								@Blog DataSourceRegistration blogDSR, @Blog DataSource blogDS) {
						this.crmDSR = crmDSR;
						this.blogDSR = blogDSR;
						this.crmDS = crmDS;
						this.blogDS = blogDS;
				}

				@Override
				public void run(ApplicationArguments args) throws Exception {
						log("CRM", this.crmDS);
						log("BLOG", this.blogDS);

						log("CRM", this.crmDSR);
						log("BLOG", this.blogDSR);
				}


				private static void log(String p, Object o) {
						log.info("==============================================");
						log.info(p);
						log.info(ToStringBuilder.reflectionToString(o));
				}
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

@Log
class DynamicDataSourcePropertiesBindingPostProcessor implements ImportBeanDefinitionRegistrar {

		// todo dont duplicate this method!

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
						log.info("adding qualifier '" + label + "' for " + clzz.getName() + " instance.");
						log.info("registered bean " + newBeanName + ".");
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

				@Override
				public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
						this.applicationContext = applicationContext;
				}

				ApplicationContext applicationContext;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

				registry.registerBeanDefinition(ACH.class.getName(), BeanDefinitionBuilder.genericBeanDefinition(ACH.class).getBeanDefinition());

				Supplier<Binder> supplier = new Supplier<Binder>() {

						private Binder binder;

						@Override
						public Binder get() {
								if (null == this.binder) {
										DefaultListableBeanFactory dlbf = DefaultListableBeanFactory.class.cast(registry);
										ACH ach = dlbf.getBean(ACH.class.getName(), ACH.class);
										ApplicationContext applicationContext = ach.applicationContext;
										this.binder = buildBinderFor(applicationContext);
								}
								return this.binder;
						}
				};


				importingClassMetadata
					.getAllAnnotationAttributes(EnableMDJ.class.getName())
					.get("value")
					.stream()
					.map(o -> (String[]) o)
					.flatMap(Stream::of)
					.forEach(p -> registerForLabel(registry, supplier, p));

				//		value.forEach(x-> log.info(x.toString()));
				// todo source the labels here from the annotation itself !
//				Stream.of("crm", "blog").forEach(l -> this.registerForLabel(registry, supplier, l));
		}

		private void registerForLabel(BeanDefinitionRegistry registry,
																																Supplier<Binder> binderSupplier,
																																String label) {

				BoundDataSourceRegistrationSupplier dsrSupplier = new BoundDataSourceRegistrationSupplier(binderSupplier, label);
				this.register(label, label + DataSourceRegistration.class.getSimpleName(), DataSourceRegistration.class, registry, dsrSupplier);

				BoundDataSourceSupplier boundDataSourceSupplier = new BoundDataSourceSupplier(dsrSupplier);
				this.register(label, label + DataSource.class.getSimpleName(), DataSource.class, registry, boundDataSourceSupplier);

		}
}