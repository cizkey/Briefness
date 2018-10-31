package com.hacknife.briefness;

import com.hacknife.briefness.bean.Briefness;
import com.hacknife.briefness.bean.Field;
import com.hacknife.briefness.bean.Link;
import com.hacknife.briefness.bean.Method;
import com.hacknife.briefness.bean.View;
import com.hacknife.briefness.util.ClassUtil;
import com.hacknife.briefness.util.ClassValidator;
import com.hacknife.briefness.util.Logger;
import com.hacknife.briefness.util.StringUtil;
import com.hacknife.briefness.util.ViewCollection;

import java.util.List;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Created by Hacknife on 2018/10/31.
 */

public class Briefnessor {
    public static final String PROXY = "Briefnessor";
    private final TypeElement typeElement;
    private final String className;//MainActivity
    private final String proxyClassName;//MainActivityBriefness
    private final String packageName;//package com.hacknife.briefness
    private Briefness briefness;
    private String javaSource;
    private String host;

    public Briefnessor(Elements elementUtils, TypeElement classElement) {
        this.typeElement = classElement;
        PackageElement packageElement = elementUtils.getPackageOf(classElement);
        String packageName = packageElement.getQualifiedName().toString();
        className = ClassValidator.getClassName(classElement, packageName);
        this.packageName = packageName;
        this.proxyClassName = className + PROXY;
    }

    public String getProxyClassFullName() {
        return packageName + "." + proxyClassName;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public String generateJavaCode(String buidPath, String packages) {
        if (ClassUtil.instanceOfActivity(className))
            host = "host.";
        else
            host = "view.";
        javaSource = String.format(Constant.javaPath, buidPath, packages.replace(".", "/"), className);
        briefness = new Briefness();
        ClassParser.parser(javaSource, briefness);
        XmlParser.parser(buidPath, briefness.getLayout(), briefness);
        Logger.v(briefness.toString());
        return Constant.briefnessor
                .replaceAll(Constant.className, className)
                .replaceAll(Constant.iPackage, packageName)
                .replaceAll(Constant.proxy, proxyClassName)
                .replaceAll(Constant.packages, packages)
                .replaceAll(Constant.setContentView, generateContentView())
                .replaceAll(Constant.iJavabean, generateIJavabean())
                .replaceAll(Constant.iView, generateIView())
                .replaceAll(Constant.javabean, generateJavabean())
                .replaceAll(Constant.view, generateView())
                .replaceAll(Constant.findView, generateFindView())
                .replaceAll(Constant.click, generateClick())
                .replaceAll(Constant.longClick, generateLongClick())
                .replaceAll(Constant.set, generateSetBean())
                .replaceAll(Constant.clear, generateClear())
                .replaceAll(Constant.clearAll, generateClearAll());
    }

    private String generateLongClick() {
        return "";
    }

    private String generateClick() {
        StringBuilder builder = new StringBuilder();
        List<Method> methods = briefness.getMethods();
        for (Method method : methods) {
            String[] ids = method.getIds();
            for (String id : ids) {
                builder.append("        " + host + "findViewById(" + id + ").setOnClickListener(new View.OnClickListener() {\n" +
                        "            @Override\n" +
                        "            public void onClick(View view) {\n" +
                        "                host." + method.getMethodName() + "(view);\n" +
                        "            }\n" +
                        "        });\n");
            }
        }
        return builder.toString();
    }

    private String generateFindView() {
        StringBuilder builder = new StringBuilder();
        List<Field> fields = briefness.getFileds();
        for (Field field : fields) {
            if (field.getIds().length == 1)
                builder.append("        host." + field.getVariable() + " = (" + field.getClassType() + ") " + host + "findViewById(" + field.getIds()[0] + ");\n");
            else {
                builder.append("        host." + field.getVariable() + " = new " + field.getClassType() + "{\n");
                String[] ids = field.getIds();
                for (int i = 0; i < ids.length; i++) {
                    builder.append("                (" + field.getClassName() + ") " + host + "findViewById(" + ids[i] + ")");
                    if (i == ids.length - 1)
                        builder.append("\n");
                    else
                        builder.append(",\n");
                }
                builder.append("        };\n");
            }
        }
        if (briefness.getLayout() != null) {
            List<View> views = briefness.getLabel().getViews();
            for (View view : views) {
                builder.append("        " + view.getId() + " = (" + view.getClassName() + ") " + host + "findViewById(R.id." + view.getId() + ");\n");
            }
        }
        return builder.toString();
    }

    private String generateClearAll() {
        String clear = generateClear();
        if (briefness.getLayout() == null) return clear;
        List<View> views = briefness.getLabel().getViews();
        StringBuilder builder = new StringBuilder();
        builder.append(clear);
        for (View view : views) {
            builder.append("        this.").append(view.getId()).append(" = null;\n");
        }
        return builder.toString();

    }

    private String generateClear() {
        if (briefness.getLayout() == null) return "";
        List<Link> links = briefness.getLabel().getLinkes();
        StringBuilder builder = new StringBuilder();
        for (Link link : links) {
            builder.append("        this.").append(link.getAlisa()).append(" = null;\n");
        }
        return builder.toString();
    }

    private String generateSetBean() {
        return "";
    }

    private String generateView() {
        if (briefness.getLayout() == null) return "";
        List<View> views = briefness.getLabel().getViews();
        StringBuilder builder = new StringBuilder();
        for (View view : views) {
            builder.append("    ").append(view.getClassName()).append(" ").append(view.getId()).append(";\n");
        }
        return builder.toString();
    }

    private String generateJavabean() {
        if (briefness.getLayout() == null) return "";
        List<Link> links = briefness.getLabel().getLinkes();
        StringBuilder builder = new StringBuilder();
        for (Link link : links) {
            builder.append("    ").append(link.getClassName()).append(" ").append(link.getAlisa()).append(";\n");
        }
        return builder.toString();
    }

    private String generateIView() {
        StringBuilder builder = new StringBuilder();
        if (briefness.getLayout() == null) {
            List<Field> fields = briefness.getFileds();
            for (Field field : fields) {
                String fullClassName = ViewCollection.getFullNameByName(field.getClassName());
                if (fullClassName.length() != 0 && (!StringUtil.stringContainString(builder, fullClassName)))
                    builder.append("import ").append(fullClassName).append(";\n");
            }
        } else {
            List<View> views = briefness.getLabel().getViews();
            for (View view : views) {
                String fullClassName = view.getFullClassName();
                if (fullClassName.length() != 0 && (!StringUtil.stringContainString(builder, fullClassName)))
                    builder.append("import ").append(fullClassName).append(";\n");
            }
        }
        return builder.toString();
    }

    private String generateIJavabean() {
        if (briefness.getLayout() == null) return "";
        List<Link> links = briefness.getLabel().getLinkes();
        StringBuilder builder = new StringBuilder();
        for (Link link : links) {
            if (!StringUtil.stringContainString(builder, link.getFullClassName()))
                builder.append("import ").append(link.getFullClassName()).append(";\n");
        }
        return builder.toString();
    }

    private String generateContentView() {
        if (ClassUtil.instanceOfActivity(className))
            return "        if (!Utils.contentViewExist(host)) {\n" +
                    "            host.setContentView(R.layout." + briefness.getLayout() + ");\n" +
                    "        }\n";
        else
            return "        View view = (View) source;\n";
    }


}