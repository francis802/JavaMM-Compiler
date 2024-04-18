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

import java.util.Objects;


public class ArrayAccess extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitArrayAccess);
        addVisit(Kind.DESCRIBED_ARRAY, this::visitArrayDescribed);
        addVisit(Kind.TYPE, this::visitArrayIndex);
        //addVisit(Kind.TYPE, this::visitArrayIndex);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayAccess(JmmNode accessedVar, SymbolTable table) {
        Type accessedVarType = TypeUtils.getExprType(accessedVar, table);


        if(accessedVar.getParent().toString().equals("ArraySubs") && !accessedVarType.isArray()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(accessedVar),
                    NodeUtils.getColumn(accessedVar),
                    "Accessing a non-array var!",
                    null)
            );
            return null;
        }

        return null;
    }

    private Void visitArrayIndex(JmmNode array, SymbolTable table) {
        return null;
    }

    private Void visitArrayDescribed(JmmNode array, SymbolTable table) {
        Type arrayType = TypeUtils.getExprType(array, table);
        for (JmmNode child : array.getChildren()) {
            Type childType = TypeUtils.getExprType(child, table);
            if (!Objects.equals(childType.getName(), arrayType.getName())) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(child),
                        NodeUtils.getColumn(child),
                        "Array was assigned with different types!",
                        null)
                );
                return null;
            }
        }
        return null;
    }


}