package org.acme;

import jakarta.persistence.Embeddable;

@Embeddable
public class Phone {

    public String type;
    public String number;
}
