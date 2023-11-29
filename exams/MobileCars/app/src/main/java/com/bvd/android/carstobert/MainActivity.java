package com.bvd.android.carstobert;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bvd.android.carstobert.customer.CustomerActivity;
import com.bvd.android.carstobert.employee.EmployeeActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }
    @OnClick(R.id.mainCustomerBtn)
    void redirectCustomer() {
        Intent intent = new Intent(this, CustomerActivity.class);
        startActivity(intent);
    }
    @OnClick(R.id.mainEmployeBtn)
    void  redirectEmployee() {
        Intent intent = new Intent(this, EmployeeActivity.class);
        startActivity(intent);
    }
}
