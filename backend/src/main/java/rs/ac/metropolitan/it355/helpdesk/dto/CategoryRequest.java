package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Naziv kategorije je obavezan")
        @Size(max = 60, message = "Naziv kategorije sme imati najvise 60 karaktera")
        String name,

        @Size(max = 255, message = "Opis sme imati najvise 255 karaktera")
        String description,

        /** Kada nije poslato, podrazumeva se da je kategorija aktivna. */
        Boolean active) {
}
