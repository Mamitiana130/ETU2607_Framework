package servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import annotation.*;
// import model.*;
import javax.servlet.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.*;


import java.lang.reflect.Parameter;

import utils.*;
import annotation.*;

public class FrontController extends HttpServlet {

    HashMap<String, Mapping> mapp = new HashMap<>();

    public void init() throws ServletException{

        try{
            String packageToScan = getInitParameter("package_name");
            mapp = getListeClasses(packageToScan , ControllerAnnotation.class);
       
        }
        catch (Exception e) {
            e.printStackTrace(); 
            throw new ServletException(e.getMessage());
        }
    }
    

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processedRequest(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processedRequest(req, res);
    }

    protected void processedRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");

        PrintWriter out = res.getWriter();
       
        // String requestURI = req.getRequestURI();
        // String contextPath = req.getContextPath();
        String url = req.getRequestURI();

        // out.println("" + requestURI + " et " + ":" + contextPath);
        // out.println(url);

        // if (url.contains("?")) {
        //     int index = url.indexOf("?");
        //     String basePath = url.substring(0, index);
        //     url = basePath;
        // }  

        boolean urlExist = false;
        for(String cle : mapp.keySet()) {
            if(cle.equals(url)) {
                // out.println("Cle : "+cle+"\n");
                // out.println("Url : "+url +"\n");
                // out.println("Methode associe : "+ mapp.get(cle).getMethodeName());
                // out.println("avec la class : "+ mapp.get(cle).getClassName());
                Mapping mapping = mapp.get(cle);

                try{
                    
                    Class<?> clazz = Class.forName(mapp.get(cle).getClassName());
                    // Method[] methods = clazz.getDeclaredMethods();

                    // Method method = clazz.getMethod(mapp.get(cle).getMethodeName());
                    Method m = null;
                    for (VerbAction action : mapping.getVerbactions()) {
                        if (action.getVerb().equalsIgnoreCase(req.getMethod())) {
                            Class<?> c = Class.forName(mapping.getClassName());
                            Method[] methods = c.getDeclaredMethods();
                            for (Method method : methods) {
                                if (method.getName().equals(action.getMethodName())) {
                                    m = method;
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    if (m == null) {
                        throw new NoSuchMethodException("Method not found in class " + mapping.getClassName());
                    }
       
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    Object[] parameterValues = Util.getParameterValues(req, m, ParamAnnotation.class,
                            ParamObject.class);

                    Field sessionField = null;
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (field.getType().equals(MySession.class)) {
                            sessionField = field;
                            break;
                        }
                    }
                    if(sessionField != null) {
                        sessionField.setAccessible(true);
                        sessionField.set(instance , new MySession(req.getSession()));
                    }
                    
                    for (int i = 0; i < parameterValues.length; i++) {
                        if (parameterValues[i] == null && m.getParameterTypes()[i].equals(MySession.class)) {
                            MySession session = new MySession(req.getSession());
                            parameterValues[i] = session;
                        }
                    }

                    Object result = m.invoke(instance, parameterValues);

                    
                    if(m.isAnnotationPresent(RestAPI.class)){
                        if(result instanceof ModelView){
                            res.setContentType("application/json");
                            Gson gson = new Gson();

                            ModelView mv = (ModelView) result;
                            String targetUrl = mv.getUrl();
                            HashMap<String, Object> data = mv.getData();
                            String jsonModel = gson.toJson(data);
                            out.println(jsonModel);
                        }
                        else{
                            res.setContentType("application/json");
                            Gson gson = new Gson();
                            String jsonModel = gson.toJson(result);
                            out.println(jsonModel);
                        }
                    }else if(result instanceof ModelView){
                            ModelView mv = (ModelView) result;
                            String targetUrl = mv.getUrl();
                            ServletContext context = getServletContext();
                            String realPath = context.getRealPath(targetUrl);

                            // if (realPath == null || !new File(realPath).exists()) {
                            //     throw new ServletException("La page JSP " + targetUrl + " n'existe pas.");
                            // }
                        
                            // HashMap<String, Object> data = mv.getData();
                            // for (String keyData : data.keySet()) {
                            //     req.setAttribute(keyData, data.get(keyData));
                            // }
                                HashMap<String, Object> data = mv.getData();
                                for (Map.Entry<String, Object> entry : data.entrySet()) {
                                    req.setAttribute(entry.getKey(), entry.getValue());
                                }

                            RequestDispatcher dispatch = req.getRequestDispatcher(targetUrl);
                            dispatch.forward(req, res);
                        }
                        else if (result instanceof String) {
                            out.println(result.toString());
                        } else {
                            throw new ServletException("Type de retour inconnu : " + result.getClass().getName());
                        }
                        out.println("Resultat de l'execution: " + result.toString());

                } catch(Exception e){
                    e.printStackTrace();
                    req.setAttribute("error", e.getMessage());
                    RequestDispatcher dispatch = req.getRequestDispatcher("pages/error.jsp");
                    dispatch.forward(req, res);
                }
                urlExist = true;    
                break;
            }
            
        }
        if (!urlExist) {
            out.println("Error 404 - No method is associated with the URL: " + url);
        }
    }

    
    public HashMap getListeClasses(String packageName , Class<?> annotationClass) throws Exception {
        
        HashMap<String, Mapping> map = new HashMap<>();

    try{
        String path = Thread.currentThread().getContextClassLoader().getResource(packageName.replace('.', '/')).getPath();

        String decodedPath = URLDecoder.decode(path, "UTF-8");
        File packageDir = new File(decodedPath);
        
        // if (!packageDir.exists()) {
        //     throw new PackageNotFound("Le repertoire du package " + packageName + " n'existe pas.");
        // }

        File[] files = packageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> classe = Class.forName(className);
                    if (classe.isAnnotationPresent(annotationClass.asSubclass(java.lang.annotation.Annotation.class))) {
                        for (Method method : classe.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(Url.class)) {
                                Url annotation = method.getAnnotation(Url.class);

                                // for(String key : map.keySet()){
                                //     if(annotation.value().equals(key))
                                //     throw new Exception("Duplicate url : " +annotation.value());
                                // }

                                if(!map.containsKey(annotation.value())){
                                    map.put(annotation.value(), new Mapping(classe.getName()));
                                }

                                String nameClass = classe.getName();
                                String lien = annotation.value();
                                String methodeName = method.getName();

                                boolean isGet = method.isAnnotationPresent(Get.class);
                                boolean isPost = method.isAnnotationPresent(Post.class);

                                if (!isGet && !isPost) {
                                    isGet = true;
                                }
            
                                String verb = null;
                                if (isGet) {
                                    verb="GET";
                                }
                                else{
                                    verb="POST";
                                }
                                map.get(lien).addVerbAction(method.getName(), verb);

                                System.out.println("Méthode annotée : " + method.getName());
                                System.out.println("Valeur de l'annotation : " + annotation.value());
                            }
                        }
                    }
                }
            }
        }
    } catch (IOException e) {
        throw new Exception("Package introuvable");
    }
        return map;
    }
}
