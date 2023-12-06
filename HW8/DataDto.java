package ru.otus.redisprofiler.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Random;



@Data
public class DataDto implements Serializable {
    private final static Random random = new Random();


    private  String data1;
    private  String data2;

    public DataDto() {
        data1 = Double.toString(random.nextExponential());
        data2 = Double.toString(random.nextExponential());
    }

}
