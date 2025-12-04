package ru.itmo.rest.dto;


public class DragonResponseDTO {
    private final Long id; //Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически
    private final String name; //Поле не может быть null, Строка не может быть пустой
    private final CoordinatesDTO coordinates; //Поле не может быть null
    private final String creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически
    private final int age; //Значение поля должно быть больше 0
    private final String description; //Поле может быть null
    private final String color; //Поле не может быть null
    private final String type; //Поле может быть null
    private final DragonHeadDTO head;

    public DragonResponseDTO(Long id, String name, CoordinatesDTO coordinates,
                             String creationDate, int age, String description,
                             String color, String type, DragonHeadDTO head) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.age = age;
        this.description = description;
        this.color = color;
        this.type = type;
        this.head = head;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public CoordinatesDTO getCoordinates() { return coordinates; }
    public String getCreationDate() { return creationDate; }
    public int getAge() { return age; }
    public String getDescription() { return description; }
    public String getColor() { return color; }
    public String getType() { return type; }
    public DragonHeadDTO getHead() { return head; }
}
