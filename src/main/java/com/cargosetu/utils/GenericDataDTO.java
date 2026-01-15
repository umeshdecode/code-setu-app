package com.cargosetu.utils;

import lombok.Data;

import java.util.List;


@Data
public class GenericDataDTO {
    private String responseMessege;
    private Integer responseCode;
    private List dataList;
    private Object date;
}
