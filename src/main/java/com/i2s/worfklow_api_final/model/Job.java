package com.i2s.worfklow_api_final.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode

@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false, name = "title")
    private String title;


    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    private List<User> users;

    public Job() {
    }


}
