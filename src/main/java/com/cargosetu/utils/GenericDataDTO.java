package com.cargosetu.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericDataDTO {
    private String responseMessege;
    private Integer responseCode;
    private List dataList;
    private Object data;


}
