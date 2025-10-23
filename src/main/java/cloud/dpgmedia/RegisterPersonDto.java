package cloud.dpgmedia;

import java.time.LocalDate;

public record RegisterPersonDto(String firstName, String lastName, LocalDate dateOfBirth, LocationDto addressLocation,
                                Behavior behavior) {}
