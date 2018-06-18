package xdb.jpa;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.VisibleConversionServiceDeducer;
import org.springframework.boot.context.properties.VisiblePropertySourcesDeducer;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySources;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
class ApplicationContextAwareBinderSupplier implements Supplier<Binder> {

		private Binder binder;

		private final DefaultListableBeanFactory defaultListableBeanFactory;
		private final String applicationContextHolderBeanName;

		ApplicationContextAwareBinderSupplier(DefaultListableBeanFactory defaultListableBeanFactory, String applicationContextHolderBeanName) {
				this.defaultListableBeanFactory = defaultListableBeanFactory;
				this.applicationContextHolderBeanName = applicationContextHolderBeanName;
		}

		@Override
		public Binder get() {
				if (null == this.binder) {
						MultipleJpaRegistrationImportBeanDefinitionRegistrar.ApplicationContextHolder applicationContextHolder = defaultListableBeanFactory
							.getBean(applicationContextHolderBeanName, MultipleJpaRegistrationImportBeanDefinitionRegistrar.ApplicationContextHolder.class);
						ApplicationContext applicationContext = applicationContextHolder.applicationContext;
						this.binder = buildBinderFor(applicationContext);
				}
				return this.binder;
		}

		Binder buildBinderFor(ApplicationContext applicationContext) {
				PropertySources propertySources = new VisiblePropertySourcesDeducer(applicationContext).getPropertySources();
				ConversionService bean = new VisibleConversionServiceDeducer(applicationContext).getConversionService();
				Iterable<ConfigurationPropertySource> from = ConfigurationPropertySources.from(propertySources);
				Consumer<PropertyEditorRegistry> editorRegistryConsumer = ((ConfigurableApplicationContext) applicationContext).getBeanFactory()::copyRegisteredEditorsTo;
				return new Binder(from, new PropertySourcesPlaceholdersResolver(propertySources), bean, editorRegistryConsumer);
		}
}
