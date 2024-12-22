package com.scitequest.martin.export;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Circle.class, name = "CIRCLE"),
        @JsonSubTypes.Type(value = Polygon.class, name = "POLYGON"),
})
public interface Geometry {

    Geometry moveTo(double d, double e);
}
