package ru.itmo.rest.dto;

public class CoordinatesDTO {
    private final Long x;
    private final Double y;

    public CoordinatesDTO(Long x, Double y) {
        this.x = x;
        this.y = y;
    }

    public Long getX() { return x; }
    public Double getY() { return y; }
}
