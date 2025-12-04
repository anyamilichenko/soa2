package ru.itmo.rest.dto;

class PageInfoDTO {
    private final int page;
    private final long totalElements;
    private final int totalPages;

    public PageInfoDTO(int page, long totalElements, int totalPages) {
        this.page = page;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public int getPage() { return page; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
