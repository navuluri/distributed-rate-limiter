package com.distributed.rate.limiter.models;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ApiResponse implements Serializable
{
    @Serial
    private static final long serialVersionUID = -3883165426158324248L;

    private String user;
    private String message;
    private int totalRequestsThisMinute;
    private int totalRequestsThisHour;
    private int totalRequestsToday;

}
