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

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
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

		@Bean
		ApplicationRunner runner(
			@Crm DataSourceRegistration crmDSR, @Crm DataSource crmDS, @Crm JdbcTemplate crmJT, @Crm EntityManagerFactory crmEMF, @Crm JpaTransactionManager crmTxManager, PostRepository pr,
			@Blog DataSourceRegistration blogDSR, @Blog DataSource blogDS, @Blog JdbcTemplate blogJT, @Blog EntityManagerFactory blogEMF, @Blog JpaTransactionManager blogTxManager, OrderRepository or) {

				return args -> {
						Runnable crmRunnable = () -> {
								crmEMF
									.createEntityManager()
									.createQuery("select o from " + Order.class.getName() + " o", Order.class)
									.getResultList()
									.forEach(p -> log.info("order: " + ToStringBuilder.reflectionToString(p)));
								or.findAll().forEach(o -> log.info("order (JPA): " + ToStringBuilder.reflectionToString(o)));
						};
						log("CRM", crmDSR, crmDS, crmJT, crmEMF, crmTxManager, crmRunnable);

						Runnable blogRunnable = () -> {
								blogEMF
									.createEntityManager()
									.createQuery("select b from " + Post.class.getName() + " b", Post.class)
									.getResultList()
									.forEach(p -> log.info("post: " + ToStringBuilder.reflectionToString(p)));
								pr.findAll().forEach(p -> log.info("post (JPA): " + ToStringBuilder.reflectionToString(p)));
						};
						log("BLOG", blogDSR, blogDS, blogJT, blogEMF, blogTxManager, blogRunnable);
				};
		}

		private static void log(String label, DataSourceRegistration dsr, DataSource ds, JdbcTemplate jt, EntityManagerFactory emf, JpaTransactionManager txManager, Runnable r) {
				log.info("======================================================================");
				log.info(label);
				log.info(ToStringBuilder.reflectionToString(dsr));
				log.info(ToStringBuilder.reflectionToString(ds));
				log.info(ToStringBuilder.reflectionToString(jt));
				log.info(ToStringBuilder.reflectionToString(emf));
				log.info(ToStringBuilder.reflectionToString(txManager));
				r.run();
				log.info(System.lineSeparator());
		}
}

