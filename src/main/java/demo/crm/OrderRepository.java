package demo.crm;

import org.springframework.data.jpa.repository.JpaRepository;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public interface OrderRepository extends JpaRepository<Order, Long> {
}
