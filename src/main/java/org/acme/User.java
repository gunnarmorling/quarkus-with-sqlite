package org.acme;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="users")
public class User extends PanacheEntity {

    public String first_name;
    public String last_name;
    public String email;
    public String birthday;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
    public List<Address> addresses = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name ="phone_numbers", joinColumns = @JoinColumn(name="user_id"))
    public List<Phone> phone_numbers = new ArrayList<>();
}
