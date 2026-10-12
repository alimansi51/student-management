package com.sms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

/** A student managed by the admin. Each student gets a login (username + password) when created. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    /** Generated automatically as firstname.lastname */
    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt hash, never sent to the client */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    /** True while the student still has the one-time password the admin gave them. */
    private Boolean mustChangePassword;

    /** The plain password, only filled in right after it is generated so the admin can hand it out. */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String initialPassword;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return firstName + " " + lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public String getInitialPassword() { return initialPassword; }
    public void setInitialPassword(String initialPassword) { this.initialPassword = initialPassword; }
}
