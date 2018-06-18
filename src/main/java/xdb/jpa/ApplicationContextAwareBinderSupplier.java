package xdb.jpa;

import org.springframework.beans.PropertyEditorRegistry;
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

		private final ApplicationContext applicationContext;

		ApplicationContextAwareBinderSupplier(ApplicationContext ac) {
				this.applicationContext = ac;
		}

		@Override
		public Binder get() {
				if (null == this.binder) {
						this.binder = buildBinder();
				}
				return this.binder;
		}

		Binder buildBinder() {
				PropertySources propertySources = new VisiblePropertySourcesDeducer(this.applicationContext).getPropertySources();
				ConversionService bean = new VisibleConversionServiceDeducer(this.applicationContext).getConversionService();
				Iterable<ConfigurationPropertySource> from = ConfigurationPropertySources.from(propertySources);
				Consumer<PropertyEditorRegistry> editorRegistryConsumer =
					((ConfigurableApplicationContext) this.applicationContext).getBeanFactory()::copyRegisteredEditorsTo;

				return new Binder(from, new PropertySourcesPlaceholdersResolver(propertySources), bean, editorRegistryConsumer);
		}
}
