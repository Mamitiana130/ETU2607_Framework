package utils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import javax.servlet.ServletException;
import annotation.*;
import java.lang.Exception;
import javax.servlet.http.Part;
import javax.servlet.http.*;
import javax.servlet.*;

public class Util {

    public static Object[] getParameterValues(HttpServletRequest request,HttpServletResponse response, Method method,
            Class<ParamAnnotation> paramAnnotationClass, Class<ParamObject> paramObjectAnnotationClass)
            throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];
        Map<String, String[]> params = request.getParameterMap();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(paramAnnotationClass)) {
               if (request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart/")) {
                    Part filePart = request.getPart("file");
                    if (filePart != null) {
                        Fichier fichier = new Fichier(filePart);
                        parameterValues[i] = fichier;
                    } else {
                        throw new ServletException("File part is missing.");
                    }
                }
                else{
                    ParamAnnotation param = parameters[i].getAnnotation(paramAnnotationClass);
                    String paramName = param.value();
                    String paramValue = request.getParameter(paramName);
                    parameterValues[i] = convertParameterValue(paramValue, parameters[i].getType());
                }
               
            } else if (parameters[i].isAnnotationPresent(paramObjectAnnotationClass)) {
                ParamObject paramObject = parameters[i].getAnnotation(paramObjectAnnotationClass);
                String objName = paramObject.objName();
                try {
                    Object paramObjectInstance = parameters[i].getType().getDeclaredConstructor().newInstance();
                    Field[] fields = parameters[i].getType().getDeclaredFields();
                    
                   Map<String, String> fieldsValues = new HashMap<>();
                    Map<String, String> validationErrors = new HashMap<>();

                    for (Field field : fields) {
                        String fieldName = field.getName();
                        String paramValue = request.getParameter(objName + "." + fieldName);
                        
                        fieldsValues.put(fieldName, paramValue);
                        if (paramValue != null) {
                            field.setAccessible(true);
                            field.set(paramObjectInstance, convertParameterValue(paramValue, field.getType()));
                        }

                        String key = objName + "." + fieldName;
                    }
                    validationErrors = validateField(paramObjectInstance, params, objName);
                    
                    if (!validationErrors.isEmpty()) {
                        request.setAttribute("errors", validationErrors);
                        request.setAttribute("values", fieldsValues);
                        
                        isError iserr = method.getAnnotation(isError.class);
                        if(iserr != null) { 
                            String errUrl = iserr.url();
                            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {

                                @Override
                                public String getMethod() {
                                    return "GET";
                                }
                            };
                            RequestDispatcher dispatcher = request.getRequestDispatcher(errUrl);
                            dispatcher.forward(wrappedRequest, response);
                        }
                        System.out.println("Validation errors for object: " + objName);
                        throw new Exception("Validation errors: " + validationErrors);
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

    public static Map<String, String> validateField(Object obj , Map<String, String[]> params, String objName) throws Exception {
           
           Map<String, String> errors =  new HashMap<>();
           Field[] fields = obj.getClass().getDeclaredFields();
   
           for(Field field : fields) {
   
               field.setAccessible(true); 
               String fieldName = field.getName();
               String key = objName + "." + fieldName; 
       
               if (field.isAnnotationPresent(Required.class)) {
                   if (params.get(key) == null || params.get(key)[0].isEmpty()) {
                       errors.put(fieldName, "Ce parametre est obligatoire.");   
                       continue;  
                   }
               }
              
       
               if (field.isAnnotationPresent(Length.class)) {
                   if (params.get(key) != null ) {                 
                           String valeur = params.get(key)[0].replace(" ", "");
                           Length length = field.getAnnotation(Length.class);
   
                           if (valeur.length() > length.value()) {
                               errors.put(fieldName,"Ce paramètre ne doit pas dépasser " + length.value() + " caractères.");
                               continue;
                           }          
                   }
               }
               
               if (field.isAnnotationPresent(Numeric.class)) {
                   if (params.get(key)!= null && !params.get(key)[0].isEmpty()) {                 
                       try {                    
                               Double.parseDouble(params.get(key)[0]);
                       } catch (Exception e) {
                           System.out.println(e.getMessage());
                           errors.put(fieldName, "Ce parametre doit etre un nombre.");
                           continue;
                       }
                   }
               }
               if (field.isAnnotationPresent(Range.class)) {
                   if (params.get(key) != null) {
                       try {
                           double valeur = Double.parseDouble(params.get(key)[0]);
                           Range range = field.getAnnotation(Range.class);
                           if (valeur < range.min() || valeur > range.max()) {
                               // System.out.println("Ambany na ambony");
                               errors.put(fieldName, "Ce parametre doit etre entre [" + range.min() + " et " + range.max() + "].");
                               continue;
                               // throw new Exception("The parameter " + key + " must be within the range [" + range.min() + ", " + range.max() + "].");
                           }
                       } catch (NumberFormatException e) {
                           errors.put(fieldName, "Ce parametre doit etre un nombre.");
                           continue;
                           // throw new Exception("The parameter " + key + " must be a number.");
                       }
               
                   }
               }
   
           }
           return errors;
       }

    public static Object convertParameterValue(String value, Class<?> type) {
        if (value == null || value.isEmpty()) {
            if (type.isPrimitive()) {
                // Retourner la valeur par défaut pour les types primitifs
                if (type == int.class) return 0;
                if (type == boolean.class) return false;
                if (type == long.class) return 0L;
                if (type == double.class) return 0.0;
                if (type == float.class) return 0.0f;
                if (type == short.class) return (short) 0;
                if (type == byte.class) return (byte) 0;
                if (type == char.class) return '\u0000';
            }
            // Retourner null pour les types objets
            return null;
        }
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
