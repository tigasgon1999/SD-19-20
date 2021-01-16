package pt.tecnico.sauron.silo.domain.exceptions;

public enum ErrorMessage {
    CAM_NOT_FOUND("Camera not found with name %s."), PERSON_ID_FORMAT("Id %s is not a valid id format for a person."),
    INVALID_TYPE("Type %s is not a valid type."), CAR_ID_FORMAT("Id %s is not a valid id format for a car."),
    CAM_EXISTS("Camera with name %s already exists and has different coords"),
    INVALID_CAM_NAME("%s is not a valid name format for a camera."), INVALID_CAM_LAT("Invalid latitude format."),
    INVALID_CAM_LONG("Ivalid longitude format."), NULL_ID("Identifier cannot be null or empty");

    public final String label;

    ErrorMessage(String label) {
        this.label = label;
    }
}