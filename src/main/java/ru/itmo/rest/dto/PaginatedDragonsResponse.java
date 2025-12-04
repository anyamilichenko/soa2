package ru.itmo.rest.dto;

public class PaginatedDragonsResponse {
    private final java.util.List<DragonResponseDTO> items;
    private final PageInfoDTO page;

    public PaginatedDragonsResponse(java.util.List<DragonResponseDTO> items, PageInfoDTO page) {
        this.items = items;
        this.page = page;
    }

    public java.util.List<DragonResponseDTO> getItems() { return items; }
    public PageInfoDTO getPage() { return page; }
}
