# Multiple `DataSource` JPA configuration

This supports easily registering all the required bits for multiple-database JPA use. 

It works with Spring Boot's existing mechanisms for `DataSource` configuration. 

Let's assume you have a freshly generated Spring Boot project from [the Spring Initializr](http://start.spring.io) in 
which you've selected a JDBC database driver (in this case, `MySQL`, the example uses MySQL) and Lombok, a compile-time 
annotation processor.

Build this library and add this library (`com.joshlong`:`xdb-jpa`:`0.0.1-SNAPSHOT`) to your application's classpath.

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

Create the following JPA entities, Spring Data JPA repositories, and database schema. Be careful to put them in two _different_ packages!

## The CRM Schema 


`Order` (under `demo.crm`):

```
package demo.crm;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "orders")
@Data
public class Order {

 @Id
 @GeneratedValue
 private Long id;
 private String sku;
}
	
```

`Order` maps to a table with roughtly the following DDL:

```
REATE TABLE `orders` (
  `id` bigint(20) NOT NULL,
  `sku` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) 
```

Create the following Spring Data JPA repository:

```
package demo.crm;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

## The Blog Schema 
`Post` (under `demo.blog`):

```
package demo.blog;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "post")
public class Post {

 @Id
 @GeneratedValue
 private Long id;
 private String title;
};
```

`Post` maps to a table with roughtly the following DDL:

```
CREATE TABLE `post` (
  `id` bigint(20) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
);
```

Create the following Spring Data JPA repository:

```
package demo.blog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
```

## The Spring Boot Application 

Create the following Spring Boot main-class (in whatever package you like; I used `demo`). 

```
package demo;

import demo.blog.Post;
import demo.blog.PostRepository;
import demo.crm.Order;
import demo.crm.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
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
@Log4j2
public class DemoApplication {

		public static void main(String args[]) {
				SpringApplication.run(DemoApplication.class, args);
		}

		private static class LoggingRunner implements ApplicationRunner {

				private final DataSourceRegistration dsr;
				private final DataSource ds;
				private final JdbcTemplate jt;
				private final EntityManagerFactory emf;
				private final JpaTransactionManager jtaTxManager;
				private final Runnable runnable;
				private final String label;

				LoggingRunner(String label, DataSourceRegistration dsr, DataSource ds, JdbcTemplate jt, EntityManagerFactory emf,
																		JpaTransactionManager jtaTxManager, Runnable runnable) {
						this.dsr = dsr;
						this.ds = ds;
						this.label = label;
						this.jt = jt;
						this.emf = emf;
						this.jtaTxManager = jtaTxManager;
						this.runnable = runnable;
				}

				@Override
				public void run(ApplicationArguments args) throws Exception {
						log.info("======================================================================");
						log.info(this.label.toUpperCase() + ':');
						log.info(ToStringBuilder.reflectionToString(dsr));
						log.info(ToStringBuilder.reflectionToString(ds));
						log.info(ToStringBuilder.reflectionToString(jt));
						log.info(ToStringBuilder.reflectionToString(emf));
						log.info(ToStringBuilder.reflectionToString(jtaTxManager));
						runnable.run();
						log.info(System.lineSeparator());
				}
		}

		@Bean
		ApplicationRunner crmRunner(
			@Qualifier("crm") DataSourceRegistration crmDSR,
			@Qualifier("crm") DataSource crmDS,
			@Qualifier("crm") JdbcTemplate crmJT,
			@Qualifier("crm") EntityManagerFactory crmEMF,
			@Qualifier("crm") JpaTransactionManager crmTxManager,
			OrderRepository or) {
				return new LoggingRunner("crm", crmDSR, crmDS, crmJT, crmEMF, crmTxManager, () -> {
						crmEMF
							.createEntityManager()
							.createQuery("select o from " + Order.class.getName() + " o", Order.class)
							.getResultList()
							.forEach(p -> log.info("order: " + ToStringBuilder.reflectionToString(p)));
						or.findAll().forEach(o -> log.info("order (JPA): " + ToStringBuilder.reflectionToString(o)));
				});
		}

		@Bean
		ApplicationRunner blogRunner(
			@Qualifier("blog") DataSourceRegistration blogDSR,
			@Qualifier("blog") DataSource blogDS,
			@Qualifier("blog") JdbcTemplate blogJT,
			@Qualifier("blog") EntityManagerFactory blogEMF,
			@Qualifier("blog") JpaTransactionManager blogTxManager,
			PostRepository pr) {
				return new LoggingRunner("blog", blogDSR, blogDS, blogJT, blogEMF, blogTxManager, () -> {
						blogEMF
							.createEntityManager()
							.createQuery("select b from " + Post.class.getName() + " b", Post.class)
							.getResultList()
							.forEach(p -> log.info("post: " + ToStringBuilder.reflectionToString(p)));
						pr.findAll().forEach(p -> log.info("post (JPA): " + ToStringBuilder.reflectionToString(p)));
				});
		}
}
```

What demonstrates is that you now have multiple JPA repositories, databases, and more available for injection in your code. However, as this introduces the possibility of duplicate beans, we need a way to distinguish which instance we want. This library automatically gives every bean a _qualifier_. You can use the Spring `@Qualifier` annotation with the same string as you use for the configuration properties, `crm` and `blog`, in this case. 

Now, you might not want to duplicate that magic string everywhere in your code. And I don't blame you! So create meta annotations, like this:

```
package demo.blog;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Qualifier("blog")
public @interface Blog {
}
```

and

```
package demo.crm;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Qualifier("crm")
public @interface Crm {
}
```

Now, you can replace all uses of `@Qualifier("crm")` with `@Crm` and all uses of `@Qualifier("blog")` with `@Blog`. 

Enjoy!
