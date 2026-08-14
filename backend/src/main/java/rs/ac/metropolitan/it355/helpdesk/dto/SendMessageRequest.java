package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Poruka ne sme biti prazna")
        @Size(max = 2000, message = "Poruka sme imati najvise 2000 karaktera")
        String content) {
}
