package ru.itmo.rest.dto;

class DragonHeadDTO {
    private Integer eyesCount;


    public DragonHeadDTO() {
    }

    public DragonHeadDTO(Integer eyesCount) {
        this.eyesCount = eyesCount;

    }

    public Integer getEyesCount() {
        return eyesCount;
    }

    public void setEyesCount(Integer eyesCount) {
        this.eyesCount = eyesCount;
    }


}
