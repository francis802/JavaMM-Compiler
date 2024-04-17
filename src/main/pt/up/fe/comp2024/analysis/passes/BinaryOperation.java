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
import pt.up.fe.specs.util.SpecsCheck;

import java.util.List;
import java.util.Objects;

public class BinaryOperation extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        JmmNode firstOperand = binaryExpr.getJmmChild(0);
        JmmNode secOperand = binaryExpr.getJmmChild(1);

        String op = binaryExpr.get("op");

        Type firstOperandType = TypeUtils.getExprType(firstOperand, table);
        Type secOperandType = TypeUtils.getExprType(firstOperand, table);


        if (Objects.equals(firstOperandType.getName(), "unknown")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    "Var is not defined!",
                    null)
            );
            return null;
        }

        if (Objects.equals(secOperandType.getName(), "unknown")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    "Var is not defined!",
                    null)
            );
            return null;
        }

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
            if (!Objects.equals(firstOperandType.getName(), "int") || firstOperandType.isArray()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        "First Operand must be of type integer",
                        null)
                );
                return null;
            }

            if (!Objects.equals(secOperandType.getName(), "int") || secOperandType.isArray()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        "Second Operand must be of type integer",
                        null)
                );
                return null;
            }
        }

        if(op.equals("&&") || op.equals("||")) {
            if(!Objects.equals(firstOperandType.getName(), "boolean")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        "First Operand must be of type integer",
                        null)
                );
                return null;
            }
            if(!Objects.equals(secOperandType.getName(), "boolean")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        "Second Operand must be of type integer",
                        null)
                );
                return null;
            }
        }

        if(op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=") || op.equals("==") || op.equals("!=")) {
            if(!Objects.equals(firstOperandType.getName(),"int")  || !Objects.equals(secOperandType.getName(),"int") || firstOperand.get("isArray").equals("true") || secOperand.get("isArray").equals("true")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        "Types don't match in the comparison",
                        null)
                );
                return null;
            }
        }

        return null;
    }
}
