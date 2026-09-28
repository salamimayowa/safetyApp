package com.nigeria.health.shared.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wraps paginated results from Spring Data Page objects.
 * Used by all list endpoints that support pagination.
 *
 * Usage in service:
 *   Page<Hospital> page = hospitalRepository.findAll(pageable);
 *   return PagedResponse.from(page.map(mapper::toResponse));
 */
@Data
@Builder
public class PagedResponse<T> {

    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean first;
    private boolean last;

    public static <T> PagedResponse<T> from(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
