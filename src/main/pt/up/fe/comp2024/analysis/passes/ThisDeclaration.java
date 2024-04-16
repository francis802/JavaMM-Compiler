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
        addVisit(Kind.VAR_REF_EXPR, this::visitThisDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method;
        return null;
    }

    private Void visitThisDecl(JmmNode thisDecl, SymbolTable table) {
        System.out.println(currentMethod.get("name") + currentMethod.getChildren());


        return null;
    }
}
