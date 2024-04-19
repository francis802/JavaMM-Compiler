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

        if(right.toString().equals("Length")) {
            return null;
        }

        if(!Kind.VAR_REF_EXPR.check(assignment.getJmmChild(0))) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignment),
                    NodeUtils.getColumn(assignment),
                    "Left Var is not defined!",
                    null)
            );
            return null;
        }

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

        if (leftType.isArray() != rightType.isArray()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignment),
                    NodeUtils.getColumn(assignment),
                    leftType.isArray() ? "Left Var is an array and Right Var is not!" : "Right Var is an array and Left Var is not!",
                    null)
            );
            return null;
        }
        var importLst = table.getImports().stream().map(x -> x.split("\\.")[x.split("\\.").length - 1]).toList();
        if(!Objects.equals(leftType.getName(), rightType.getName())){
            if (!importLst.contains(rightType.getName()) && !importLst.contains(leftType.getName())) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignment),
                        NodeUtils.getColumn(assignment),
                        "Assigning to variable" + leftType.getName() + " from variable " + rightType.getName() + " of different types!",
                        null)
                );
                return null;
            }
            if (importLst.contains(leftType.getName()) && rightType.getName().equals(table.getClassName()) && !table.getSuper().equals(leftType.getName())) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignment),
                        NodeUtils.getColumn(assignment),
                        "Assigning class instance to imported class object, but class isn't extended class of imported!",
                        null)
                );
                return null;
            }
            if (leftType.getName().equals("int") || leftType.getName().equals("boolean")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignment),
                        NodeUtils.getColumn(assignment),
                        "Assigning to primitive type variable not matching the type!",
                        null)
                );
                return null;
            }

        }

        return null;
    }

    private Void visitArrayAssign(JmmNode assignment, SymbolTable table) {


        return null;
    }
}
