package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="users")
public class User extends PanacheEntity {
	
    public String first_name;
    public String last_name;
	public String street;
	public String zip_code;
	public String city;
	public String country;
	public String phone;
	public String birthday;
}
