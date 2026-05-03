package com.concert.ticketing.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Controller 层 AOP 日志切面
 * 统一记录请求参数、响应结果和执行耗时
 *
 * @author Concert Ticketing Team
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 定义切点：所有 Controller 层的方法
     */
    @Pointcut("execution(* com.concert.ticketing..*Controller.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录请求和响应
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 异常
     */
    @Around("controllerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String requestURI = request != null ? request.getRequestURI() : "N/A";
        String httpMethod = request != null ? request.getMethod() : "N/A";

        // 记录请求参数
        String argsString = Arrays.stream(joinPoint.getArgs())
                .map(arg -> {
                    try {
                        if (arg == null) {
                            return "null";
                        }
                        // 过滤掉 HttpServletRequest/Response 等对象
                        if (arg instanceof HttpServletRequest) {
                            return "HttpServletRequest";
                        }
                        return objectMapper.writeValueAsString(arg);
                    } catch (Exception e) {
                        return arg.toString();
                    }
                })
                .collect(Collectors.joining(", "));

        long startTime = System.currentTimeMillis();
        log.info("[请求] {} {} - {}.{}, 参数: [{}]", httpMethod, requestURI, className, methodName, argsString);

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();

            // 记录响应
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String resultString;
            try {
                resultString = objectMapper.writeValueAsString(result);
                // 截断过长的响应
                if (resultString.length() > 500) {
                    resultString = resultString.substring(0, 500) + "... (truncated)";
                }
            } catch (Exception e) {
                resultString = result != null ? result.toString() : "null";
            }

            log.info("[响应] {} {} - {}.{}, 耗时: {}ms, 结果: {}",
                    httpMethod, requestURI, className, methodName, duration, resultString);

            return result;
        } catch (Throwable throwable) {
            // 记录异常
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.error("[异常] {} {} - {}.{}, 耗时: {}ms, 异常: {}",
                    httpMethod, requestURI, className, methodName, duration, throwable.getMessage(), throwable);
            throw throwable;
        }
    }
}
