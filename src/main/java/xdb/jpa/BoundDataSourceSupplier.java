package xdb.jpa;

import javax.sql.DataSource;
import java.util.function.Supplier;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
class BoundDataSourceSupplier implements Supplier<DataSource> {

		private final Supplier<DataSourceRegistration> dataSourceRegistration;

		BoundDataSourceSupplier(Supplier<DataSourceRegistration> dataSourceRegistration) {
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
