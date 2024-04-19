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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class DuplicatedInstances extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.PROGRAM, this::visitProgram);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitProgram(JmmNode program, SymbolTable table) {
        List<String> carrier = new ArrayList<>();
        for (String import_ : table.getImports()) {
            if (carrier.contains(import_)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(program),
                        NodeUtils.getColumn(program),
                        "Duplicated import!",
                        null)
                );
            }
            carrier.add(import_);
        }
        var classImportsLst = table.getImports().stream().map(x -> x.split("\\.")[x.split("\\.").length - 1]).toList();
        carrier.clear();
        for (var class_ : classImportsLst) {
            if (carrier.contains(class_)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(program),
                        NodeUtils.getColumn(program),
                        "Duplicated import class declaration!",
                        null)
                );
            }
            carrier.add(class_);
        }
        carrier.clear();
        for (var field_ : table.getFields()) {
            if (carrier.contains(field_.getName())) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(program),
                        NodeUtils.getColumn(program),
                        "Duplicated field declaration!",
                        null)
                );
            }
            carrier.add(field_.getName());
        }
        carrier.clear();
        for (var method_ : table.getMethods()) {
            if (carrier.contains(method_)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(program),
                        NodeUtils.getColumn(program),
                        "Duplicated method declaration!",
                        null)
                );
            }
            carrier.add(method_);
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        String methodName = method.get("name");
        List<String> carrier = new ArrayList<>();
        var localVars = table.getLocalVariables(methodName);
        for (var localVar : localVars) {
            if (carrier.contains(localVar.getName())) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Duplicated local variable declaration!",
                        null)
                );
            }
            carrier.add(localVar.getName());
        }
        carrier.clear();
        var params = table.getParameters(methodName);
        for (var param : params) {
            if (carrier.contains(param.getName())) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Duplicated parameter declaration!",
                        null)
                );
            }
            carrier.add(param.getName());
        }
        return null;
    }


}