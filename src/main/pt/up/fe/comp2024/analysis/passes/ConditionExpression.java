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

public class ConditionExpression extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CONDITIONAL_STMT, this::visitConditionExpression);
        addVisit(Kind.WHILE_STMT, this::visitConditionExpression);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");

        return null;
    }

    private Void visitConditionExpression(JmmNode statement, SymbolTable table) {
        JmmNode condition = statement;

        if(statement.getJmmChild(0).getChildren().toArray().length > 1) {
            condition = statement.getJmmChild(0).getChildren().get(0);
        }
        else {
            Type varType = TypeUtils.getExprType(statement.getChild(0), table);

            if(varType.isArray() || !varType.getName().equals("boolean")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(statement),
                        NodeUtils.getColumn(statement),
                        "Condition expression doesn't return a boolean",
                        null)
                );
                return null;
            }
        }

        if(condition.getKind().equals("BinaryExpr")) {
            var condType = TypeUtils.getExprType(condition, table);
            if (!condType.getName().equals("boolean")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(statement),
                        NodeUtils.getColumn(statement),
                        "Condition expression doesn't return a boolean",
                        null)
                );
                return null;
            }
        }
        return null;
    }
}
