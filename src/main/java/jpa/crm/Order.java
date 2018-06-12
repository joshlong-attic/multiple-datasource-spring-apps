package jpa.crm;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Entity
@Table(name = "orders")
@Data
public class Order {

		@Id
		@GeneratedValue
		private Long id;

		private String sku;
}
