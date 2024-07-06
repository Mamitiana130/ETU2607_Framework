package utils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.*;

import annotation.*;
import java.lang.Exception;

public class Util {

    public static Object[] getParameterValues(HttpServletRequest request, Method method,
            Class<ParamAnnotation> paramAnnotationClass, Class<ParamObject> paramObjectAnnotationClass)
            throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(paramAnnotationClass)) {
                ParamAnnotation param = parameters[i].getAnnotation(paramAnnotationClass);
                String paramName = param.value();
                String paramValue = request.getParameter(paramName);
                parameterValues[i] = convertParameterValue(paramValue, parameters[i].getType());
            } else if (parameters[i].isAnnotationPresent(paramObjectAnnotationClass)) {
                ParamObject paramObject = parameters[i].getAnnotation(paramObjectAnnotationClass);
                String objName = paramObject.objName();
                try {
                    Object paramObjectInstance = parameters[i].getType().getDeclaredConstructor().newInstance();
                    Field[] fields = parameters[i].getType().getDeclaredFields();
                    for (Field field : fields) {
                        String fieldName = field.getName();
                        String paramValue = request.getParameter(objName + "." + fieldName);
                        field.setAccessible(true);
                        field.set(paramObjectInstance, convertParameterValue(paramValue, field.getType()));
                    }
                    parameterValues[i] = paramObjectInstance;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to create and populate parameter object: " + e.getMessage());
                }
            } else {
                // throw new Exception("ETU 2607");
                String paramName = parameters[i].getName();
                String paramValue = request.getParameter(paramName);
                parameterValues[i] = convertParameterValue(paramValue,
                        parameters[i].getType());
            }
        }
        return parameterValues;
    }

    public static Object convertParameterValue(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        } else if (type == char.class || type == Character.class) {
            if (value.length() != 1) {
                throw new IllegalArgumentException("Invalid character value: " + value);
            }
            return value.charAt(0);
        }
        return null;
    }
}
