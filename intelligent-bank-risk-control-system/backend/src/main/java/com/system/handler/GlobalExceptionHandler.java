package com.system.handler;

import com.system.common.Result;
import com.system.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public Result<Void> handleApiException(ApiException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(400, message);
    }

    /** MyBatis/JDBC 访问数据库失败时给出具体原因（缺表、缺列等），便于对照 sql 脚本修复 */
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccess(DataAccessException e) {
        log.error("Data access error", e);
        Throwable root = e.getMostSpecificCause();
        String detail = root != null && root.getMessage() != null ? root.getMessage() : e.getMessage();
        String hint = "数据库访问失败。请确认已执行 bank-risk-controller.sql，库名与 backend/application.yml 一致。";
        return Result.error(500, detail != null ? detail : hint);
    }

    /** MyBatis 包装异常默认不进 DataAccessException，单独透传根因（如 Unknown column） */
    @ExceptionHandler(PersistenceException.class)
    public Result<Void> handlePersistence(PersistenceException e) {
        log.error("MyBatis persistence error", e);
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String detail = t.getMessage() != null ? t.getMessage() : e.getMessage();
        String msg =
                "数据库映射失败（常见原因：未执行 sql/bot_rag_patch.sql / bot_intelligent_service_v2.sql）。"
                        + (detail != null ? " " + detail : "");
        return Result.error(500, msg.length() > 800 ? msg.substring(0, 800) + "…" : msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return Result.error(500, "服务器内部错误");
    }
}
