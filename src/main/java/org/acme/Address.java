package org.acme;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="addresses")
public class Address extends PanacheEntity {

    @ManyToOne
    @JsonIgnore
    public User user;
    public String street;
    public String zip_code;
    public String city;
    public String country;
}
