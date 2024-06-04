package servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.lang.reflect.Method;
import annotation.*;
import utils.*;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import annotation.ControllerAnnotation;

public class FrontController extends HttpServlet {
    HashMap<String, Mapping> mapp = new HashMap<>();

    public void init() throws ServletException {
        try {
            String packageScan = getInitParameter("package_name");
            mapp = getAllClasses(packageScan);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doPost(HttpServletRequest requette, HttpServletResponse response) throws ServletException, IOException {
        this.processRequest(requette, response);
    }

    public void doGet(HttpServletRequest requette, HttpServletResponse response) throws ServletException, IOException {
        this.processRequest(requette, response);
    }

    public void processRequest(HttpServletRequest requette, HttpServletResponse response)
            throws ServletException, IOException {
        String url = requette.getRequestURL().toString();
        PrintWriter out = response.getWriter();
        for (String key : mapp.keySet()) {
            if (key.equals(url)) {
                out.println("URL: " + url + "\n");
                out.println("Function associate: " + mapp.get(key).getMethodeName());
                out.println("with the class: " + mapp.get(key).getClassName());
                try {
                    Class<?> clazz = Class.forName(mapp.get(key).getClassName());
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    Method method = clazz.getMethod(mapp.get(key).getMethodeName());

                    Object result = method.invoke(instance);
                    out.println("Result of the execution: " + result.toString());

                    if (result instanceof ModelView) {
                        ModelView modelView = (ModelView) result;
                        String urlTarget = modelView.getUrl();
                        RequestDispatcher requestDispatcher = requette.getRequestDispatcher("pages/"+urlTarget);

                        HashMap<String, Object> data = modelView.getData();
                        for (String keyData : data.keySet()) {
                            requette.setAttribute(keyData, data.get(keyData));
                        }
                        requestDispatcher.forward(requette, response);
                    }
                } catch (Exception e) {
                    e.printStackTrace(out);
                }
            }
        }
    }

    public HashMap getAllClasses(String packageName) throws Exception {
        HashMap<String, Mapping> map = new HashMap<>();
        String path = Thread.currentThread().getContextClassLoader().getResource(packageName.replace('.', '/'))
                .getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        File packageDir = new File(decodedPath);

        File[] files = packageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> classe = Class.forName(className);
                    for (Method method : classe.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Get.class)) {
                            Get annotation = method.getAnnotation(Get.class);
                            String nameClass = classe.getName();
                            String annotationName = annotation.value();
                            String methodName = method.getName();

                            map.put(annotationName, new Mapping(nameClass, methodName));

                            System.out.println("Method annotation :" + method.getName());
                            System.out.println("Valeur de l annotation: " + annotation.value());
                        }
                    }
                }
            }
        }
        return map;




