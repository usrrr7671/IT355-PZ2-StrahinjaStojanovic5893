package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PriorityRequest(
        @NotBlank(message = "Naziv prioriteta je obavezan")
        @Size(max = 40)
        String name,

        @NotNull(message = "Nivo prioriteta je obavezan")
        @Min(value = 1, message = "Nivo mora biti izmedju 1 i 10")
        @Max(value = 10, message = "Nivo mora biti izmedju 1 i 10")
        Integer level,

        @NotNull(message = "SLA rok je obavezan")
        @Min(value = 1, message = "SLA rok mora biti bar 1 sat")
        @Max(value = 720, message = "SLA rok sme biti najvise 720 sati")
        Integer slaHours) {
}
