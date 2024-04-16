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

public class Assignment extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitVarAssign);
        addVisit(Kind.ASSIGN_STMT, this::visitArrayAssign);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarAssign(JmmNode assignment, SymbolTable table) {
        JmmNode left = assignment.getJmmChild(0);
        JmmNode right = assignment.getJmmChild(1);

        Type leftType = new Type("unknown", false);
        Type rightType = new Type("unknown", false);

        System.out.println(left);
        System.out.println(right);

        for (var field: table.getFields()) {
            //if(table.getFields().stream().anyMatch(param -> param.getName().equals(left.get("name")))) {
            if (field.getName().equals(left.get("name"))) {
                leftType = field.getType();
            }

            if (field.getName().equals(right.get("name"))) {
                rightType = field.getType();
            }
        }

        for (var variable: table.getLocalVariables(currentMethod)) {
            //if(table.getFields().stream().anyMatch(param -> param.getName().equals(left.get("name")))) {
            if (variable.getName().equals(left.get("name"))) {
                leftType = variable.getType();
            }

            if (variable.getName().equals(right.get("name"))) {
                rightType = variable.getType();
            }
        }

        if (Objects.equals(leftType.getName(), "unknown")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignment),
                    NodeUtils.getColumn(assignment),
                    "Var is not defined!",
                    null)
            );
            return null;
        }

        if (Objects.equals(rightType.getName(), "unknown")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignment),
                    NodeUtils.getColumn(assignment),
                    "Var is not defined!",
                    null)
            );
            return null;
        }

        return null;
    }

    private Void visitArrayAssign(JmmNode assignment, SymbolTable table) {


        return null;
    }
}
