package com.cargosetu.utils;

import lombok.Data;
import org.hibernate.mapping.List;

@Data
public class GenericDataDTO {
    private String responseMessege;
    private Integer responseCode;
    private List dataList;
    private Object date;
}
