package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Objects;

public class ThisDeclaration extends AnalysisVisitor {
    private JmmNode currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.OBJECT, this::visitObjectCall);
        addVisit(Kind.FIELD_CALL, this::visitFieldCall);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method;
        return null;
    }

    private Void visitObjectCall(JmmNode fieldCall, SymbolTable table) {
        if (currentMethod.get("isMain").equals("true")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(fieldCall),
                    NodeUtils.getColumn(fieldCall),
                    "Cannot use 'this' in main method!",
                    null)
            );
            return null;
        }
        return null;
    }

    private Void visitFieldCall(JmmNode functionCall, SymbolTable table) {
        if (currentMethod.get("isMain").equals("true")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(functionCall),
                    NodeUtils.getColumn(functionCall),
                    "Cannot use 'this' in main method!",
                    null)
            );
            return null;
        }
        return null;
    }

}
