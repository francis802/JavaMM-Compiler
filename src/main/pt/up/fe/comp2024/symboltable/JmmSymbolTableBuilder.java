package pt.up.fe.comp2024.symboltable;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {

    public static String arrayType(JmmNode type) {
        if (!type.getAttributes().contains("name"))
            return arrayType(type.getChild(0));
        else return type.get("name");
    }

    public static JmmSymbolTable build(JmmNode root) {
        var classDecl = root.getJmmChild(root.getNumChildren()-1);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("classname");
        String superclass = "";
        List<String> attributes = classDecl.getAttributes().stream().toList();
        if(attributes.contains("superclass")) {
            superclass = classDecl.get("superclass");
        }

        var imports = buildImports(root);
        var methods = buildMethods(classDecl);
        var fields = buildFields(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, superclass, imports, fields, methods, returnTypes, params, locals);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).forEach(method -> {

            Type returnType;

            // If the method has no children, it's a void method
            if(method.getChildren().isEmpty())
                returnType = new Type("void", false);

            else {
                if (!method.getJmmChild(0).getAttributes().contains("name")) {
                    returnType = new Type(arrayType(method.getJmmChild(0).getChild(0)), true);
                } else {
                    returnType = new Type(method.getJmmChild(0).get("name"), false);
                }
            }

            map.put(method.get("name"), returnType);
        });

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).forEach(method -> {
            List<Symbol> symbols = new ArrayList<>();

            if(!method.getChildren(PARAM).isEmpty()){
                String varName = method.getChildren().get(1).get("name");
                symbols = getParamsList(method.getChildren().get(1), symbols, varName);
            }

            map.put(method.get("name"), symbols);
        });

        return map;
    }

    private static List<Symbol> getParamsList(JmmNode paramDecl, List<Symbol> lst, String varName) {
        if(!paramDecl.getChild(0).getAttributes().contains("name"))
            lst.add(new Symbol(new Type(arrayType(paramDecl.getChild(0).getChild(0)), true), varName));
        else
            lst.add(new Symbol(new Type(paramDecl.getChild(0).get("name"), false), varName));

        if (paramDecl.getNumChildren() == 1){
            return lst;
        }
        else {
            String varName2 = paramDecl.getChildren().get(1).get("name");
            return getParamsList(paramDecl.getChildren().get(1), lst, varName2);
        }
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> {
                    boolean isArray = false;
                    String type;
                    if(!varDecl.getChild(0).getAttributes().contains("name")){
                        isArray = true;
                        type = arrayType(varDecl.getChild(0).getChild(0));
                    }
                    else
                        type = varDecl.getChild(0).get("name");

                    return new Symbol(new Type(type, isArray), varDecl.get("name"));
                }).toList();
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> list = new ArrayList<>();

        classDecl.getChildren(VAR_DECL).forEach(var -> {
                    if(!var.getChild(0).getAttributes().contains("name"))
                        list.add(new Symbol(new Type(arrayType(var.getChild(0).getChild(0)), true), var.get("name")));
                    else
                        list.add(new Symbol(new Type(var.getChild(0).get("name"), false), var.get("name")));
                });
        return list;
    }

    private static List<String> buildImports(JmmNode programDecl) {
        List<String> list = new ArrayList<>();
        for (int i=0; i<programDecl.getNumChildren()-1; i++) {
            list.add(programDecl.getChild(i).get("name"));
        }

        return list;
    }

}
