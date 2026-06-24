package com.product_office_setup.product_office_setup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = {
		CassandraAutoConfiguration.class,
		CassandraDataAutoConfiguration.class,
		CassandraRepositoriesAutoConfiguration.class
})
public class ProductOfficeSetupApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductOfficeSetupApplication.class, args);
	}
}
