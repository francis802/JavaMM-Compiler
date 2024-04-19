package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

public class MethodCalls extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNCTION_CALL, this::visitFunctionCall);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        var returns = method.getChildren(Kind.RETURN_STMT);
        if (returns.isEmpty() && !method.get("isMain").equals("true")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Method does not return a value.",
                    null)
            );
            return null;
        }
        if (returns.size() > 1) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Method has more than one return statement.",
                    null)
            );
            return null;
        }
        if (returns.size() == 1 && method.get("isMain").equals("true")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Main method should not have a return statement.",
                    null)
            );
            return null;
        }
        if(method.get("isMain").equals("true")){
            return null;
        }
        var stmts = method.getChildren();
        System.out.println(stmts);
        if (!Kind.RETURN_STMT.check(stmts.get(stmts.size() - 1))) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Return statement should be the last statement in a method.",
                    null)
            );
            return null;
        }
        return null;
    }

    private Void visitFunctionCall(JmmNode funcCall, SymbolTable table) {
        Type funcType = TypeUtils.getExprType(funcCall, table);
        if (funcType.getName().isEmpty()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(funcCall),
                    NodeUtils.getColumn(funcCall),
                    "Function call is invalid.",
                    null)
            );
            return null;
        }
        Type callerType = TypeUtils.getExprType(funcCall.getJmmChild(0), table);
        for (var import_ : table.getImports()) {
            var lst = import_.split("\\.");
            String importName = lst[lst.length - 1];
            if (importName.equals(callerType.getName())) {
                return null;
            }
        }
        List<JmmNode> paramLst = List.of();
        var methods = funcCall.getAncestor(Kind.CLASS_DECL).get().getChildren(Kind.METHOD_DECL);
        for (var method : methods){
            if (method.get("name").equals(funcCall.get("name"))){
                paramLst = method.getChildren(Kind.PARAM);
            }
        }
        List<JmmNode> params = new ArrayList<>();
        if (!paramLst.isEmpty()) {
            var tempParam = paramLst.get(0);
            params.add(tempParam);
            while (tempParam.getChildren().size() == 2) {
                params.add(tempParam.getJmmChild(1));
                tempParam = tempParam.getJmmChild(1);
            }
        }
        boolean varagsDetected = false;
        for (int i = 1; i < funcCall.getNumChildren(); i++) {
            Type argType = TypeUtils.getExprType(funcCall.getJmmChild(i), table);
            JmmNode param;
            if (!varagsDetected) {
                if(i > params.size()){
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(funcCall),
                            NodeUtils.getColumn(funcCall),
                            "Function call has too many arguments.",
                            null)
                    );
                    return null;
                }

                param = params.get(i - 1);
                if (param.getJmmChild(0).get("isVarArgs").equals("true")) {
                    if (params.lastIndexOf(param) != params.size() - 1) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(funcCall),
                                NodeUtils.getColumn(funcCall),
                                "Varargs must be the last parameter in a function call",
                                null)
                        );
                        return null;
                    }
                    varagsDetected = true;
                }
            }
            else {
                param = params.get(params.size() - 1);
            }
            Type paramType = TypeUtils.getExprType(param, table);
            if (varagsDetected){
                if (!argType.getName().equals(paramType.getName())) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(funcCall),
                            NodeUtils.getColumn(funcCall),
                            "Function call argument type does not match parameter type.",
                            null)
                    );
                    return null;
                }
                else if (argType.isArray() && params.size() != (funcCall.getNumChildren()-1)) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(funcCall),
                            NodeUtils.getColumn(funcCall),
                            "Eitheir you send one array to varargs or you send multiple arguments of the same type of varargs. You can't do both.",
                            null)
                    );
                    return null;
                }
            }
            else {
                if(!argType.equals(paramType)){
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(funcCall),
                            NodeUtils.getColumn(funcCall),
                            "Function call argument type does not match parameter type.",
                            null)
                    );
                    return null;
                }
            }
        }
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        if (returnStmt.getAncestor(Kind.METHOD_DECL).isEmpty()) {
            return null;
        }
        var method = returnStmt.getAncestor(Kind.METHOD_DECL).get();
        Type returnType = table.getReturnType(method.get("name"));
        Type exprType = TypeUtils.getExprType(returnStmt.getJmmChild(0), table);
        if (!returnType.equals(exprType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmt),
                    NodeUtils.getColumn(returnStmt),
                    "Return statement type does not match expression return type.",
                    null)
            );
            return null;
        }
        return null;
    }
}
