package main.java;

public class Optionull<T> {
    private final T value;

    public Optionull(T value){
        this.value = value;
    }

    public T or(T value){
        return this.value == null ? value : this.value;
    }
}
