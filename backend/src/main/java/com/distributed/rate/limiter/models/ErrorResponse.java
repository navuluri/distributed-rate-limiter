package com.distributed.rate.limiter.models;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ErrorResponse implements Serializable
{
    @Serial
    private static final long serialVersionUID = 1869258911633181489L;
    private String message;
    private int status;

}
