package rs.ac.metropolitan.it355.helpdesk.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stabilan oblik stranicenog odgovora.
 *
 * Spring-ov {@code Page} se serijalizuje sa dosta unutrasnjih polja ciji se oblik
 * menjao izmedju verzija, pa se ka klijentu salje ovaj minimalni omotac -
 * frontend tako zavisi samo od onoga sto mu stvarno treba.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
