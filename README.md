# Multiple `DataSource` JPA configuration

This supports easily registering all the required bits for multiple-database JPA use. 

It works with Spring Boot's existing mechanisms for `DataSource` configuration. 

Let's assume you have a freshly generated Spring Boot project from [the Spring Initializr](http://start.spring.io) in 
which you've selected a JDBC database driver (in this case, `MySQL`, the example uses MySQL) and Lombok, a compile-time 
annotation processor.

Add this library (`com.joshlong`:`xdb-jpa`:`0.0.1-SNAPSHOT`) to your application's classpath.

Let's suppose you have the followng configuration in your Spring Boot `src/main/resources/application.properties`. 
These point to real databases on my machine. Configure appropriately and as necessary for your machine.


```
#
# CRM
crm.datasource.url=jdbc:mysql://localhost:3306/crm?useSSL=false
crm.datasource.username=root
crm.datasource.password=root
#
# BLOG
blog.datasource.url=jdbc:mysql://localhost:3306/blog?useSSL=false
blog.datasource.username=root
blog.datasource.password=root

```

Create the following JPA entities, `Order` (under `demo.crm`) and `Blog` (under `demo.blog`):



Create the following Spring Boot main-class (in whatever package you like; I used `demo`). 

```
package demo;

import demo.blog.Blog;
import demo.blog.Post;
import demo.blog.PostRepository;
import demo.crm.Crm;
import demo.crm.Order;
import demo.crm.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import xdb.jpa.DataSourceRegistration;
import xdb.jpa.EnableMultipleJpaRegistrations;
import xdb.jpa.JpaRegistration;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@SpringBootApplication
@EnableMultipleJpaRegistrations({
	@JpaRegistration(prefix = "crm", rootPackageClass = Order.class),
	@JpaRegistration(prefix = "blog", rootPackageClass = Post.class),
})
public class DemoApplication {

		public static void main(String args[]) {
				SpringApplication.run(DemoApplication.class, args);
		}

		@Bean
		ApplicationRunner runner(
			@Crm DataSourceRegistration crmDSR, @Crm DataSource crmDS, @Crm JdbcTemplate crmJT, @Crm EntityManagerFactory crmEMF, @Crm JpaTransactionManager crmTxManager, PostRepository pr,
			@Blog DataSourceRegistration blogDSR, @Blog DataSource blogDS, @Blog JdbcTemplate blogJT, @Blog EntityManagerFactory blogEMF, @Blog JpaTransactionManager blogTxManager, OrderRepository or) {

				return args -> {
				        // ... 
				};
		}
}
```
