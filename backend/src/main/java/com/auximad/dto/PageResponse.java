package com.auximad.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO générique pour les réponses paginées.
 * Encapsule le contenu, les métadonnées de pagination et les informations de tri.
 *
 * @param <T> Le type d'entité contenu dans la page
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    /**
     * Construit un PageResponse depuis un objet Spring Page.
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> springPage) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(springPage.getContent());
        response.setPage(springPage.getNumber());
        response.setSize(springPage.getSize());
        response.setTotalElements(springPage.getTotalElements());
        response.setTotalPages(springPage.getTotalPages());
        response.setFirst(springPage.isFirst());
        response.setLast(springPage.isLast());
        return response;
    }
}
