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

public class Varargs extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        var params = method.getChildren(Kind.PARAM);
        int count = 0;
        while (!params.isEmpty()) {
            var param = params.get(0);
            var typeNode = param.getJmmChild(0);
            if (typeNode.get("isVarArgs").equals("true"))
                count++;
            params = param.getChildren(Kind.PARAM);
            if (!params.isEmpty() && count > 0) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Varargs must be the last parameter, and there can only be one.",
                        null)
                );
                return null;
            }
        }
        var varDecls = method.getChildren(Kind.VAR_DECL);
        for (var varDecl : varDecls) {
            var typeNode = varDecl.getJmmChild(0);
            if (typeNode.get("isVarArgs").equals("true")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Varargs can't be used as a local variable.",
                        null)
                );
                return null;
            }
        }
        var returnType = method.getJmmChild(0);
        if (returnType.get("isVarArgs").equals("true")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Varargs can't be used as a return type.",
                    null)
            );
            return null;
        }
        return null;
    }
    private Void visitClassDecl(JmmNode class_, SymbolTable table) {
        var fields = class_.getChildren(Kind.VAR_DECL);
        for (var field : fields) {
            var typeNode = field.getJmmChild(0);
            if (typeNode.get("isVarArgs").equals("true")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(class_),
                        NodeUtils.getColumn(class_),
                        "Varargs can't be used as a field.",
                        null)
                );
                return null;
            }
        }
        return null;
    }
}
