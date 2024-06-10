package servlet;

import java.io.File;
import java.io.IOException;
import java.lang.Exception;
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

public class FrontController extends HttpServlet {
    HashMap<String, Mapping> mapp = new HashMap<>();

    public void init() throws ServletException {
        try {
            String packageScan = getInitParameter("package_name");
            mapp = getAllClasses(packageScan, ControllerAnnotation.class);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e.getMessage());
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

        boolean urlExist = false;
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
                        ServletContext context = getServletContext();
                        String realPath = context.getRealPath(urlTarget);

                        if (realPath == null || !new File(realPath).exists()) {
                            throw new ServletException("La page JSP " + urlTarget + " n'existe pas.");
                        }

                        HashMap<String, Object> data = modelView.getData();
                        for (String keyData : data.keySet()) {
                            requette.setAttribute(keyData, data.get(keyData));
                        }
                        RequestDispatcher requestDispatcher = requette.getRequestDispatcher("pages/" + urlTarget);
                        requestDispatcher.forward(requette, response);

                    } else if (result instanceof String) {
                        out.println(result.toString());
                    } else {
                        throw new ServletException("Type de retour inconnu : " + result.getClass().getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace(out);
                }
                urlExist = true;
                break;
            }
        }
        if (!urlExist) {
            out.println("Aucune methode n\\'est associee a l\\'url : " + url);
        }
    }

    public HashMap getAllClasses(String packageName, Class<?> annotationClass) throws Exception {
        HashMap<String, Mapping> map = new HashMap<>();
        try {
            String path = Thread.currentThread().getContextClassLoader().getResource(packageName.replace('.', '/'))
                    .getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            File packageDir = new File(decodedPath);

            if (!packageDir.exists()) {
                throw new Exception("Le repertoire du package " + packageName + " n'existe pas.");
            }

            File[] files = packageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = packageName + "." + file.getName().replace(".class", "");
                        Class<?> classe = Class.forName(className);
                        if (classe.isAnnotationPresent(
                                annotationClass.asSubclass(java.lang.annotation.Annotation.class))) {
                            for (Method method : classe.getDeclaredMethods()) {
                                if (method.isAnnotationPresent(Get.class)) {
                                    Get annotation = method.getAnnotation(Get.class);

                                    for (String key : map.keySet()) {
                                        if (annotation.value().equals(key))
                                            throw new Exception("Duplicate url : " + annotation.value());
                                    }

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
            }
        } catch (IOException e) {
            throw new Exception("Package introuvable");
        }
        return map;

    }
}
