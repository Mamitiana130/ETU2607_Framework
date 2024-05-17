package servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import annotation.*;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import annotation.ControllerAnnotation;

public class FrontController extends HttpServlet {
    private List<Class<?>> Listecontroller;
    private boolean isChecked = false;

    public void doPost(HttpServletRequest requette, HttpServletResponse response) throws ServletException, IOException {
        this.processRequest(requette, response);
    }

    public void doGet(HttpServletRequest requette, HttpServletResponse response) throws ServletException, IOException {
        this.processRequest(requette, response);
    }

    public void processRequest(HttpServletRequest requette, HttpServletResponse response)
            throws ServletException, IOException {
        StringBuffer url = requette.getRequestURL();
        PrintWriter out = response.getWriter();
        out.println("Mamt was here" + url);
        if (!this.isChecked) {
            String packageScan = this.getInitParameter("package_name");
            try {
                this.Listecontroller = this.getListeControllers(packageScan);
                this.isChecked = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Class<?> classs : Listecontroller) {
            out.println(classs.getName());
        }
    }

    boolean isController(Class<?> c) {
        return c.isAnnotationPresent(ControllerAnnotation.class);
    }

    List<Class<?>> getListeControllers(String packageName) throws Exception {
        List<Class<?>> res = new ArrayList<Class<?>>();
        String path = this.getClass().getClassLoader().getResource(packageName.replace('.', '/')).getPath();
        String decodedPath = URLDecoder.decode(path, "UTF-8");
        File packageDir = new File(decodedPath);

        File[] files = packageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> classe = Class.forName(className);
                    if (this.isController(classe)) {
                        res.add(classe);
                    }
                }
            }
        }
        return res;

    }
}
