package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class LengthMethod extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.LENGTH, this::visitVarRefExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarRefExpr(JmmNode length, SymbolTable table) {
        for(var desc : length.getDescendants()) {
            if (desc.getKind().equals("ArrayDeclaration")) {
                return null;
            }
        }

        var message = String.format("Calculating the length for a non-array variable", length);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(length),
                NodeUtils.getColumn(length),
                message,
                null)
        );

        /*for(var desc : length.getDescendants()) {
            System.out.println(desc.getKind());
            if(!(desc.getKind().equals("Parenthesis") && desc.getChild(0).getKind().equals("ArrayDeclaration"))) {
                var message = String.format("Calculating the length for a non-array variable", length);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(length),
                        NodeUtils.getColumn(length),
                        message,
                        null)
                );
            } else if (!desc.getKind().equals("ArrayDeclaration")) {
                var message = String.format("Calculating the length for a non-array variable", length);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(length),
                        NodeUtils.getColumn(length),
                        message,
                        null)
                );
            }
        }*/


        return null;
    }


}
