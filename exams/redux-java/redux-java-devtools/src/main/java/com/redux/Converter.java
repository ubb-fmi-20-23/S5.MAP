package com.redux;

public interface Converter<E> {

    E fromJson(String json);

    String toJson(E element);
}
