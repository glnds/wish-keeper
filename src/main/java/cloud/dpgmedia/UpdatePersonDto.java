package cloud.dpgmedia;

import java.time.LocalDate;

public record UpdatePersonDto(int id, String firstName, String lastName, LocalDate dateOfBirth, LocationDto addressLocation,
                              Behavior behavior, int version
) {}

