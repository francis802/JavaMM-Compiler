package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.optimization.OllirExprResult;
import pt.up.fe.comp2024.optimization.OptUtils;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";

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
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case TRUE_LITERAL, FALSE_LITERAL -> new Type("boolean", false);
            case FIELD_CALL -> getFieldExprType(expr, table);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*" -> new Type(INT_TYPE_NAME, false);
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
            if (import_.equals(varRefExpr.get("name"))) {
                return new Type(import_, false);
            }
        }
        throw new RuntimeException("Variable '" + varRefExpr.get("name") + "' not found in table");
    }

    private static Type getFieldExprType(JmmNode fieldCall, SymbolTable table) {
        for(var field: table.getFields()){
            if (field.getName().equals(fieldCall.get("name"))){
                return field.getType();
            }
        }
        throw new RuntimeException("Field '" + fieldCall.get("name") + "' not found in table");
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
