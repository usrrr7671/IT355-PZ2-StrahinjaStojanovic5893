package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.Category;

public record CategoryResponse(Long id, String name, String description, boolean active) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive());
    }
}
