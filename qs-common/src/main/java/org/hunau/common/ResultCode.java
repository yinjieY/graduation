package org.hunau.common;

public interface ResultCode {
    int SUCCESS = 200;
    int ERROR = 500;
    int PARAM_ERROR = 400;
    int NO_AUTH = 401;
    int NO_PERMISSION = 403;
    int BUSINESS_ERROR = 600;
}
