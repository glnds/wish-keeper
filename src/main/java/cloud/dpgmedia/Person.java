package cloud.dpgmedia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class Person {
    public Optional<Integer> id;
    public String firstName;
    public String lastName;
    public LocalDate dateOfBirth;
    public LocalDateTime timeOfRegistration;
    public Location addressLocation;
    public Behavior behavior;
    public int version;

    //constructor
    public Person(Optional<Integer> id, String firstName, String lastName, LocalDate dateOfBirth, LocalDateTime timeOfRegistration, Location addressLocation, Behavior behavior, int version) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.timeOfRegistration = timeOfRegistration;
        this.addressLocation = addressLocation;
        this.behavior = behavior;
        this.version = version;
    }

    public void increaseVersion() {
        this.version++;
    }

}
