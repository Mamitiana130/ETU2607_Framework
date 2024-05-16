package servele;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class FrontController extends HttpServlet {

    private List<String> controllerNames;

    public void init() throws ServletException {
        String packageToScan = this.getInitParameter("package_name");

        controllerNames = new ArrayList<>();

        try {
            String path = getClass().getClassLoader().getResource(packageToScan.replace('.', '/')).getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            File packageDir = new File(decodedPath);

            File[] files = packageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = packageToScan + "." + file.getName().replace(".class", "");
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(ControllerAnnotation.class)) {
                            controllerNames.add(clazz.getSimpleName());
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processedRequest(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processedRequest(req, res);
    }

    protected void processedRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("text/plain");
        PrintWriter out = res.getWriter();
        out.println("Tongasoa ato am FrontController");
        out.println("Votre url : " + req.getRequestURL().toString());

        out.println("Liste des contrôleurs : ");
        for (String controllerName : controllerNames) {
            out.println(controllerName);
        }
    }
}
