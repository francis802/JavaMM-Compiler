package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Objects;

public class BinaryOperation extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        JmmNode firstOperand = binaryExpr.getJmmChild(0);
        JmmNode secOperand = binaryExpr.getJmmChild(1);

        String op = binaryExpr.get("op");

        String firstOperandKind = firstOperand.getKind();
        String secOperandKind = secOperand.getKind();

        if (Objects.equals(firstOperandKind, "unknown")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    "Var is not defined!",
                    null)
            );
            return null;
        }

        if (Objects.equals(secOperandKind, "unknown")) {
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
            if (!Objects.equals(firstOperandKind, "int") || firstOperand.get("isArray").equals("true")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        "First Operand must be of type integer",
                        null)
                );
                return null;
            }

            if (!Objects.equals(secOperandKind, "int") || secOperand.get("isArray").equals("true")) {
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
            if(!Objects.equals(firstOperandKind, "boolean")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        "First Operand must be of type integer",
                        null)
                );
                return null;
            }
            if(!Objects.equals(secOperandKind, "boolean")) {
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
            if(!Objects.equals(firstOperandKind,"int")  || !Objects.equals(secOperandKind,"int") || firstOperand.get("isArray").equals("true") || secOperand.get("isArray").equals("true")) {
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
