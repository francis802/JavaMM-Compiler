package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.sql.SQLOutput;
import java.util.Objects;

public class Assignment extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitVarAssign);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");

        return null;
    }

    private Void visitVarAssign(JmmNode assignment, SymbolTable table) {
        JmmNode left = assignment.getJmmChild(0);
        JmmNode right = assignment.getJmmChild(1);

        System.out.println(left);
        System.out.println(right);


        Type leftType = TypeUtils.getExprType(left, table);
        Type rightType = TypeUtils.getExprType(right, table);

        if (Objects.equals(leftType.getName(), "")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignment),
                    NodeUtils.getColumn(assignment),
                    "Left Var is not defined!",
                    null)
            );
            return null;
        }

        if (Objects.equals(rightType.getName(), "")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignment),
                    NodeUtils.getColumn(assignment),
                    "Right Var is not defined!",
                    null)
            );
            return null;
        }

        if (!leftType.equals(rightType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignment),
                    NodeUtils.getColumn(assignment),
                    "Assigning non-valid value to variable " + left.get("name"),
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
