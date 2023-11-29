package com.bvd.android.carstobert.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.IntRange;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

/**
 * Created by bara on 1/28/2018.
 */

//@Entity
public class Car implements Serializable {
    //@PrimaryKey(autoGenerate = true)
    @Expose
    private Integer id;
    @Expose
    private String name;
    @Expose
    private String type;
    @Expose
    private String status;
    @Expose
    private Integer quantity;


    public Car(Integer id, String name, String type, String status, Integer quantity) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.quantity = quantity;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Car(String name, String type, Integer quantity, String status) {

        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.status = status;
    }

    @Override
    public String toString() {
        return "Car{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
