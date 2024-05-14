package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.optimization.OllirExprResult;
import pt.up.fe.comp2024.optimization.OptUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case VAR_DECL, PARAM -> new Type(expr.getJmmChild(0).get("name"), expr.getJmmChild(0).get("isArray").equals("true"));
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case TRUE_LITERAL, FALSE_LITERAL -> new Type("boolean", false);
            case FIELD_CALL -> getFieldExprType(expr, table);
            case FUNCTION_CALL -> getFunctionCallType(expr, table);
            case OBJECT_DECLARATION -> new Type(expr.get("name"), false);
            case ARRAY_DECLARATION -> new Type(expr.getJmmChild(0).get("name"), true);
            case ARRAY_SUBS -> new Type(getExprType(expr.getJmmChild(0),table).getName(), false);
            case DESCRIBED_ARRAY -> new Type(INT_TYPE_NAME, true);
            case ASSIGN_STMT, PARENTHESIS -> getExprType(expr.getJmmChild(0), table);
            case OBJECT -> new Type(table.getClassName(), false);
            case RETURN_STMT -> new Type(expr.getParent().getJmmChild(0).get("name"), expr.getParent().getJmmChild(0).get("isArray").equals("true"));
            case NEGATION -> new Type(BOOLEAN_TYPE_NAME, false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        JmmNode method;
        if(varRefExpr.getAncestor(METHOD_DECL).isPresent()) {
            method = varRefExpr.getAncestor(METHOD_DECL).get();
            for (var local : table.getLocalVariables(method.get("name"))) {
                if (local.getName().equals(varRefExpr.get("name"))) {
                    return local.getType();
                }
            }
            for (var param : table.getParameters(method.get("name"))) {
                if (param.getName().equals(varRefExpr.get("name"))) {
                    return param.getType();
                }
            }
        }
        for(var field: table.getFields()){
            if (field.getName().equals(varRefExpr.get("name"))){
                return field.getType();
            }
        }
        for (var import_ : table.getImports()) {
            var lst = import_.split("\\.");
            String importName = lst[lst.length - 1];
            if (importName.equals(varRefExpr.get("name"))) {
                return new Type(importName, false);
            }
        }
        return new Type("", false);
    }

    private static Type getFieldExprType(JmmNode fieldCall, SymbolTable table) {
        for(var field: table.getFields()){
            if (field.getName().equals(fieldCall.get("name"))){
                return field.getType();
            }
        }
        throw new RuntimeException("Field '" + fieldCall.get("name") + "' not found in table");
    }

    private static Type getFunctionCallType(JmmNode functionCall, SymbolTable table) {
        Type callerType = getExprType(functionCall.getJmmChild(0), table);
        if (callerType.getName().equals("int") || callerType.getName().equals("boolean")){
            return new Type("", false);
        }
        var importLst = table.getImports().stream().map(x -> x.split("\\.")[x.split("\\.").length - 1]).toList();
        if ((callerType.getName().equals(table.getClassName()) && !importLst.contains(table.getSuper())) || (OBJECT.check(functionCall.getJmmChild(0)) && table.getSuper().isEmpty())){
            var methods = functionCall.getAncestor(Kind.CLASS_DECL).get().getChildren(Kind.METHOD_DECL);
            for (var method : methods){
                if (method.get("name").equals(functionCall.get("name"))){
                    var typeNode = method.getJmmChild(0);
                    return new Type(typeNode.get("name"), typeNode.get("isArray").equals("true"));
                }
            }
            return new Type("", false);
        }
        else {
            if (ASSIGN_STMT.check(functionCall.getParent())){
                return getExprType(functionCall.getParent().getJmmChild(0), table);
            }
            if (RETURN_STMT.check(functionCall.getParent())){
                var parMethod = functionCall.getAncestor(METHOD_DECL).get();
                return table.getReturnType(parMethod.get("name"));
            }
            if (FUNCTION_CALL.check(functionCall.getParent())){
                int paramNum = -1;
                for (int i = 1; i < functionCall.getParent().getNumChildren(); i++){
                    if (functionCall.getParent().getJmmChild(i).equals(functionCall)){
                        paramNum = i;
                        break;
                    }
                }
                var methods = functionCall.getAncestor(CLASS_DECL).get().getChildren(METHOD_DECL);
                for (var method : methods){
                    if (method.get("name").equals(functionCall.getParent().get("name"))){
                        var params = method.getChildren(PARAM);

                        while (!params.isEmpty()){
                            paramNum--;
                            var param = params.get(0);
                            if (paramNum == 0){
                                var typeNode = param.getJmmChild(0);
                                return new Type(typeNode.get("name"), typeNode.get("isArray").equals("true"));
                            }
                            paramNum--;
                            params = param.getChildren(PARAM);
                        }
                        return new Type("", false);
                    }
                }
                return getExprType(functionCall.getParent(), table);
            }
            if (BINARY_EXPR.check(functionCall.getParent())){
                return getExprType(functionCall.getParent(), table);
            }
            return new Type("void", false);
        }
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName()) && sourceType.isArray() == destinationType.isArray();
    }
}
