package com.bvd.android.carstobert.controllers;

import com.bvd.android.carstobert.model.Car;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by bara on 1/28/2018.
 */

public interface CarController {
    @GET("cars")
    Call<List<Car>> getAvailableCars();
}
