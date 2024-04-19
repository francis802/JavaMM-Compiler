package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ReservedWords extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarRefExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        for (var desc : method.getDescendants()) {
            if (desc.getKind().equals("Param")) {
                if (desc.get("name").equals("length") || desc.get("name").equals("Main") || desc.get("name").equals("String")) {
                    var message = String.format("Reserved variable identifier", desc);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(desc),
                            NodeUtils.getColumn(desc),
                            message,
                            null)
                    );
                }
            }
        }
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        if (varRefName.equals("length") || varRefName.equals("String") || varRefName.equals("main")) {
            var message = String.format("Reserved variable identifier", varRefName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varRefExpr),
                    NodeUtils.getColumn(varRefExpr),
                    message,
                    null)
            );
        }

        return null;
    }


}
