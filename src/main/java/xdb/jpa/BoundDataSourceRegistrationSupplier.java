package xdb.jpa;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.ResolvableType;

import java.util.function.Supplier;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
class BoundDataSourceRegistrationSupplier implements Supplier<DataSourceRegistration> {

		private final ThreadLocal<DataSourceRegistration> dsr = new ThreadLocal<>();
		private final String label;
		private final Supplier<Binder> binder;

		BoundDataSourceRegistrationSupplier(Supplier<Binder> binder, String label) {
				this.label = label;
				this.binder = binder;
		}

		@Override
		public DataSourceRegistration get() {
				if (this.dsr.get() == null) {
						try {
								DataSourceRegistration dsr = new DataSourceRegistration();
								dsr.setBeanClassLoader(ClassLoader.getSystemClassLoader());
								dsr.setBeanName(this.label);
								this.doBinding(dsr, this.label);
								dsr.afterPropertiesSet();
								this.dsr.set(dsr);
						}
						catch (Exception e) {
								throw new RuntimeException(e);
						}
				}
				return this.dsr.get();
		}

		private void doBinding(DataSourceRegistration registration, String prefix) {
				ResolvableType resolvableType = ResolvableType.forClass(registration.getClass());
				Bindable<Object> target = Bindable
					.of(resolvableType)
					.withExistingValue(registration);
				binder.get().bind(prefix, target, null);
		}
}
