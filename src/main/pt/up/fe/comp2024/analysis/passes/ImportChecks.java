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
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ImportChecks extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.OBJECT_DECLARATION, this::visitObjectDeclaration);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {

        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        Type typeDecl = TypeUtils.getExprType(varDecl, table);
        var importLst = table.getImports().stream().map(x -> x.split("\\.")[x.split("\\.").length - 1]).toList();
        if (typeDecl.getName().equals("int") || typeDecl.getName().equals("boolean") || importLst.contains(typeDecl.getName()) || table.getClassName().equals(typeDecl.getName())) {
            return null;
        }
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varDecl),
                NodeUtils.getColumn(varDecl),
                "Type '" + typeDecl.getName() + "' does not exist or is not imported",
                null)
        );


        return null;
    }

    private Void visitObjectDeclaration(JmmNode objectDecl, SymbolTable table) {
        Type typeDecl = TypeUtils.getExprType(objectDecl, table);
        var importLst = table.getImports().stream().map(x -> x.split("\\.")[x.split("\\.").length - 1]).toList();
        if (importLst.contains(typeDecl.getName()) || table.getClassName().equals(typeDecl.getName())) {
            return null;
        }
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(objectDecl),
                NodeUtils.getColumn(objectDecl),
                "Object '" + typeDecl.getName() + "' does not exist or is not imported",
                null)
        );
        return null;
    }

}
