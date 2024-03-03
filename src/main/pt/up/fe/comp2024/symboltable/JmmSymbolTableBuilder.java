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

    //TODO: For now, the language doesn't support arrays

    public static JmmSymbolTable build(JmmNode root) {
        var classDecl = root.getJmmChild(root.getNumChildren()-1);
        System.out.println(classDecl);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("classname");
        String superclass = ""; //TODO: IMPLEMENT SUPERCLASS

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

            Type returnType = null;

            returnType = new Type(method.getJmmChild(0).get("name"), false);

            // TODO: SE O TIPO FOR VOID
            /*if(!method.getJmmChild(0).get("name").equals("void")) {
                returnType = new Type(method.getJmmChild(0).get("name"), false);
            }
            else {
                returnType = new Type("void", false);
            } */

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
        lst.add(new Symbol(new Type(paramDecl.getChildren().get(0).get("name"), false), varName));
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
                    String type = varDecl.getChild(0).get("name");
                    return new Symbol(new Type(type, false), varDecl.get("name"));
                }).toList();
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        //TODO: For now, the language doesn't support fields
        List<Symbol> list = new ArrayList<>();

        return list;
    }

    private static List<String> buildImports(JmmNode programDecl) {
        //TODO: For now, the language doesn't support imports
        List<String> list = new ArrayList<>();

        return list;
    }

}
