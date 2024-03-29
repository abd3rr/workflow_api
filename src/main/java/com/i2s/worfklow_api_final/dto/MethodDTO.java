package com.i2s.worfklow_api_final.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class MethodDTO {

    private long id;
    private String methodName;
    private List<ParameterDTO> parameters;



    public MethodDTO() {
    }



}
