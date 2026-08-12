package rs.ac.metropolitan.it355.helpdesk.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Sifarnik prioriteta (npr. "Nizak", "Srednji", "Visok", "Kriticno").
 *
 * Polje {@code level} sluzi za sortiranje (veci broj = hitnije), a {@code slaHours}
 * govori za koliko sati tiket tog prioriteta treba da bude resen.
 */
@Entity
@Table(name = "priorities")
public class Priority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "sla_hours", nullable = false)
    private int slaHours;

    protected Priority() {
    }

    public Priority(String name, int level, int slaHours) {
        this.name = name;
        this.level = level;
        this.slaHours = slaHours;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(int slaHours) {
        this.slaHours = slaHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Priority other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
