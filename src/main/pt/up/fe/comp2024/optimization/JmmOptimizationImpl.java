package pt.up.fe.comp2024.optimization;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());
        System.out.println(ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        JmmSemanticsResult newSemanticsResult = semanticsResult;
        boolean isChangedPropagate = true;
        boolean isChangedFold = true;
        if(newSemanticsResult.getConfig().containsKey("optimize") && newSemanticsResult.getConfig().get("optimize").equals("true")) {
            System.out.println("-o OPTIMIZATION STARTED");
            System.out.println(newSemanticsResult.getRootNode().toTree());
            while (isChangedPropagate || isChangedFold) {
                ConstantPropagation constantPropagation = new ConstantPropagation();
                Pair<JmmSemanticsResult,Boolean> resultPropagate = constantPropagation.optimize(newSemanticsResult);
                newSemanticsResult = resultPropagate.a;
                ConstantFolding constantFolding = new ConstantFolding();
                Pair<JmmSemanticsResult,Boolean> resultFold = constantFolding.optimize(newSemanticsResult);
                newSemanticsResult = resultFold.a;
                isChangedPropagate = resultPropagate.b;
                isChangedFold = resultFold.b;
            }
            System.out.println("-o OPTIMIZATION FINISHED");
            System.out.println(newSemanticsResult.getRootNode().toTree());
        }
        return newSemanticsResult;
    }

}
