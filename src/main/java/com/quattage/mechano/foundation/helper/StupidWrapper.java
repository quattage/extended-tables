package com.quattage.mechano.foundation.helper;

public class StupidWrapper<T extends Object> {

    T data;

    public void set(T data) {
        this.data = data;
    }

    public T get() {
        return this.data;
    }
}
