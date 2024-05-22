package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    int maxStackSize;
    int currentStackSize;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(OpCondInstruction.class, this::generateOpCond);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
    }



    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }

    private void addtoStackValue(){
        currentStackSize += 1;
        if(currentStackSize > maxStackSize) maxStackSize = currentStackSize;
    }

    private void subtoStackValue(){
        currentStackSize -= 1;
        if(currentStackSize > maxStackSize) maxStackSize = currentStackSize;
    }

    private String getParameterShortType(ElementType elements_type){
        var param_type = new StringBuilder();
        if(elements_type == ElementType.STRING) param_type.append("Ljava/lang/String;");
        else if(elements_type == ElementType.INT32) param_type.append("I");
        else if(elements_type == ElementType.BOOLEAN) param_type.append("Z");
        else if(elements_type == ElementType.VOID) param_type.append("V");
        else if(elements_type == ElementType.OBJECTREF) param_type.append("L");

        return param_type.toString();
    }

    private String getArrayElementsType(ElementType elements_type){
        var param_type = new StringBuilder();
        if(elements_type == ElementType.STRING) param_type.append("String");
        else if(elements_type == ElementType.INT32) param_type.append("int");
        else if(elements_type == ElementType.BOOLEAN) param_type.append("boolean");
        else if(elements_type == ElementType.OBJECTREF) param_type.append("idk");
        else param_type.append("sem type");

        return param_type.toString();
    }

    private String getFieldType(Type type){
        var param_type = new StringBuilder();
        switch (type.getTypeOfElement()){
            case ARRAYREF -> param_type.append("[" + getParameterShortType(((ArrayType) type).getElementType().getTypeOfElement()));
            case OBJECTREF -> param_type.append("L" + getClassName(ollirResult.getOllirClass(), ((ClassType) type).getName()) + ";");
            default -> param_type.append(getParameterShortType(type.getTypeOfElement()));
        }

        if (type.getTypeOfElement() == ElementType.OBJECTREF) {
            param_type.append(((ClassType) type).getName() + ";");
        }


        return param_type.toString();
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();


        // CLASS --------------------------------------------------------------------------------
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class public ").append(className).append(NL);


        // TODO: Hardcoded to Object, needs to be expanded
        if (classUnit.getSuperClass() != null){
            code.append(".super " + getClassName(classUnit,classUnit.getSuperClass())).append(NL);
        }
        else{
            code.append(".super java/lang/Object").append(NL);
        }

        // FIELDS --------------------------------------------------------------------------------
        var fields =  classUnit.getFields();

        for (var field: fields){
            code.append(".field ");

            if (field.isFinalField()){
                code.append( "final ");
            }
            if (field.isStaticField()){
                code.append("static ");
            }

            var modifier = field.getFieldAccessModifier() != AccessModifier.DEFAULT ? field.getFieldAccessModifier().name().toLowerCase() + " " : "";
            code.append(modifier).append(field.getFieldName()).append(" ");
            code.append(getParameterShortType(field.getFieldType().getTypeOfElement()));

            if (field.getFieldType().getTypeOfElement() == ElementType.OBJECTREF) {
                code.append(((ClassType) field.getFieldType()).getName() + ";");
            }
            code.append(NL);
        }

        // CONSTRUCTOR --------------------------------------------------------------------------------
        var constructor = new StringBuilder();

        if (classUnit.getSuperClass() != null){
            constructor.append( """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial""").append(" " + getClassName(classUnit,classUnit.getSuperClass()));
            constructor.append("/<init>()V\n" + TAB + " return\n.end method");
        }
        else{
            constructor.append("""
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """);
        }
        code.append(constructor);

        // METHODS --------------------------------------------------------------------------------
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        var is_static = method.isStaticMethod() ? "static " : "";
        // TODO: Hardcoded param types and return type, needs to be expanded

        code.append("\n.method ").append(modifier).append(is_static).append(methodName).append("(");

        // PARAMETERS TYPE --------------------------------------------------------------------------------
        var param_type = new StringBuilder();

        for(var param: method.getParams()){
            param_type.append(getFieldType(param.getType()));

        }


        code.append(param_type).append(")");

        // RETURN TYPE --------------------------------------------------------------------------------

        //code.append(getParameterShortType(method.getReturnType().getTypeOfElement()));
       // var temp = method.getReturnType().getTypeOfElement();
        //var tmp2 = getFieldType(method.getReturnType());
        code.append(getFieldType(method.getReturnType()));

        if (method.getReturnType().getTypeOfElement() == ElementType.OBJECTREF) {
            code.append(((ClassType) method.getReturnType()).getName() + ";");
        }


        code.append(NL);

        // INSIDE --------------------------------------------------------------------------------
        // Add limits



        var methods_code = new StringBuilder();
        var limit_locals = calculateLimitLocals(method);


        maxStackSize = 0;
        currentStackSize = 0;
        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            methods_code.append(instCode);

            if(inst.getInstType().equals(InstructionType.CALL) && ((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID) code.append("pop" + NL);
        }

        code.append(TAB).append(".limit stack " + maxStackSize).append(NL);
        code.append(TAB).append(".limit locals " + limit_locals).append(NL);


        code.append(methods_code);
        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private int calculateLimitLocals(Method method) {
        Set<Integer> virtualRegs = new TreeSet<>();
        virtualRegs.add(0); // init, base case

        // Verify all variables
        for (Descriptor variable : method.getVarTable().values()) {
            virtualRegs.add(variable.getVirtualReg()); // Variable index
        }
        return virtualRegs.size();
    }


    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        if(assign.getDest() instanceof ArrayOperand){
            return generateArrayAssign(assign);
        }
        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: Hardcoded for int type, needs to be expanded
        if(lhs.getType().getTypeOfElement() == ElementType.INT32 || lhs.getType().getTypeOfElement() == ElementType.BOOLEAN){
            if(reg > 3){
                code.append("istore ").append(reg).append(NL);
            }
            else{
                code.append("istore_").append(reg).append(NL);
            }
            subtoStackValue();
        }
        else if(lhs.getType().getTypeOfElement() == ElementType.ARRAYREF || lhs.getType().getTypeOfElement() == ElementType.STRING || lhs.getType().getTypeOfElement() == ElementType.THIS || lhs.getType().getTypeOfElement() == ElementType.OBJECTREF){
            if(reg > 3){
                code.append("astore ").append(reg).append(NL);
            }
            else {
                code.append("astore_").append(reg).append(NL);

            }
            subtoStackValue();
        }

        return code.toString();
    }

    private String generateArrayAssign(AssignInstruction assign){
        var code = new StringBuilder();
        Operand o = ((Operand) assign.getDest());
        Element op = ((Operand) assign.getDest());
        var reg = currentMethod.getVarTable().get(((Operand) op).getName()).getVirtualReg();

        code.append("aload_"+ reg + NL);
        addtoStackValue();

        code.append(getOperatorCases(((ArrayOperand) o).getIndexOperands().get(0)));

        code.append(generators.apply(assign.getRhs()));

        code.append(generateStore(o));

        return code.toString();
    }

    private String generateStore(Operand operand){
        var code = new StringBuilder();
        switch (operand.getType().getTypeOfElement()){
            case ARRAYREF, STRING, THIS, OBJECTREF:
                code.append("astore " + currentMethod.getVarTable().get(((Operand) operand).getName()).getVirtualReg() + NL);
                subtoStackValue();
                return code.toString();
            case BOOLEAN, INT32:
                if(currentMethod.getVarTable().get(((Operand) operand).getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF){
                    code.append("iastore" + NL);
                    subtoStackValue();
                    return code.toString();
                }
                else{
                    code.append("istore " + currentMethod.getVarTable().get(((Operand) operand).getName()).getVirtualReg() + NL);
                    subtoStackValue();
                    return code.toString();
                }
            default:
                return code.toString();
        }
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return getOperatorCases(literal);
    }

    private String generateOperand(Operand operand) {
        return getOperatorCases(operand);
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));
        var op = new StringBuilder();
        // apply operation
        switch (binaryOp.getOperation().getOpType()) {
            case ADD:
                op.append("iadd");
                subtoStackValue(); // Consome 2 operandos, empilha 1 resultado
                break;
            case MUL:
                op.append("imul");
                subtoStackValue(); // Consome 2 operandos, empilha 1 resultado
                break;
            case SUB:
                op.append("isub");
                subtoStackValue(); // Consome 2 operandos, empilha 1 resultado
                break;
            case DIV:
                op.append("idiv");
                subtoStackValue(); // Consome 2 operandos, empilha 1 resultado
                break;
            case LTH:
                op.append("isub");
                subtoStackValue();
                break;
            case NOTB:
                op.append("ixor");
                subtoStackValue(); // Consome 2 operandos, empilha 1 resultado
                break;
            // Caso queira adicionar operações lógicas AND e OR
            case ANDB:
                op.append("iand");
                subtoStackValue(); // Consome 2 operandos, empilha 1 resultado
                break;
            case ORB:
                op.append("ior");
                subtoStackValue(); // Consome 2 operandos, empilha 1 resultado
                break;
            default:
                throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded

        if(returnInst.hasReturnValue()){
            var return_val = generators.apply(returnInst.getOperand());
            code.append(return_val);
            if(returnInst.getReturnType().getTypeOfElement() == ElementType.INT32 || returnInst.getReturnType().getTypeOfElement() == ElementType.BOOLEAN) {
                code.append("ireturn").append(NL);
            }
            else code.append("areturn").append(NL);
        }
        else{
            code.append("return").append(NL);
        }
        subtoStackValue();
        return code.toString();
    }

    public static String getClassName(ClassUnit classUnit, String nameofClass) {
        for (String importElement: classUnit.getImports()) {
            String[] split = importElement.split("\\.");
            String lastImport = (split.length == 0) ? importElement : split[split.length - 1];

            if (lastImport.equals(nameofClass)) {
                return importElement.replace('.', '/');
            }
        }
        if(nameofClass.contains(".")) return classUnit.getClassName().replace(".", "/");
        return classUnit.getClassName();
    }

    private String generatePutField(PutFieldInstruction put_field_instr){
        var code = new StringBuilder();
        Element e1 = put_field_instr.getOperands().get(0);
        Element e2 = put_field_instr.getOperands().get(1);
        Element e3 = put_field_instr.getOperands().get(2);

        String field_type = getParameterShortType(e2.getType().getTypeOfElement());
        String field_name = ((Operand) e2).getName();
        String class_name = getClassName(ollirResult.getOllirClass(), ((ClassType) e1.getType()).getName());

        code.append(getOperatorCases(e1));
        code.append(getOperatorCases(e3));
        code.append("putfield " + class_name + "/" + field_name + " " + field_type);

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction get_field_instr){
        var code = new StringBuilder();
        Element e1 = get_field_instr.getOperands().get(0);
        Element e2 = get_field_instr.getOperands().get(1);

        String field_type = getParameterShortType(e2.getType().getTypeOfElement());
        String field_name = ((Operand) e2).getName();
        String class_name = getClassName(ollirResult.getOllirClass(), ((ClassType) e1.getType()).getName());

        code.append(getOperatorCases(e1));
        code.append("getfield " + class_name + "/" + field_name + " " + field_type).append(NL);

        return code.toString();
    }

    private String generateCall(CallInstruction call_instr){
        var code = new StringBuilder();

        switch(call_instr.getInvocationType()) {
            case invokespecial:
                for(var op : call_instr.getOperands()){
                    var s = getOperatorCases(op);
                    code.append(s);
                }

                code.append("invokespecial " + getClassName(ollirResult.getOllirClass(), ((Operand) call_instr.getOperands().get(0)).getName()) + "/" + "<init>" + "()" );

                code.append(getParameterShortType(call_instr.getReturnType().getTypeOfElement()));
                /*
                if (call_instr.getReturnType().getTypeOfElement() == ElementType.OBJECTREF) {
                    code.append(((ClassType) call_instr.getReturnType()).getName() + ";");
                }

                 */
                break;

            case invokestatic:
                for(var op : call_instr.getOperands()){
                    var s = getOperatorCases(op);
                    code.append(s);
                }
                var o = ((Operand) call_instr.getOperands().get(0)).getName();
                var class_name = getClassName(ollirResult.getOllirClass(), o);

                if(call_instr.getCaller().getType().getTypeOfElement() == ElementType.THIS || call_instr.getCaller().getType().getTypeOfElement() == ElementType.OBJECTREF){
                    code.append("invokestatic " + class_name + "/" +  ((LiteralElement) call_instr.getOperands().get(1)).getLiteral().replace("\"", ""));
                }
                else code.append("invokestatic " + o + "/" + ((LiteralElement) call_instr.getOperands().get(1)).getLiteral().replace("\"", ""));

                code.append("(");
                for(var arg : call_instr.getArguments()){
                    code.append(getFieldType(arg.getType()));
                }
                code.append(")");

                code.append(getParameterShortType(call_instr.getReturnType().getTypeOfElement()));
                /*
                if (call_instr.getReturnType().getTypeOfElement() == ElementType.OBJECTREF) {
                    code.append(((ClassType) call_instr.getReturnType()).getName() + ";");
                }

                 */
                code.append(NL);
                break;

            case invokevirtual:
                for(var op : call_instr.getOperands()){
                    var s = getOperatorCases(op);
                    code.append(s);
                }
                var o1 = ((Operand) call_instr.getOperands().get(0)).getName();
                var class_name1 = getClassName(ollirResult.getOllirClass(), o1);

                if(call_instr.getCaller().getType().getTypeOfElement() == ElementType.THIS || call_instr.getCaller().getType().getTypeOfElement() == ElementType.OBJECTREF){
                    code.append("invokevirtual " + class_name1 + "/" +  ((LiteralElement) call_instr.getOperands().get(1)).getLiteral().replace("\"", ""));
                }
                else code.append("invokvirtual " + o1 + "/" + ((LiteralElement) call_instr.getOperands().get(1)).getLiteral().replace("\"", ""));

                code.append("(");
                for(var arg : call_instr.getArguments()){
                    code.append(getFieldType(arg.getType()));
                }
                code.append(")");

                code.append(getParameterShortType(call_instr.getReturnType().getTypeOfElement()));
                /*
                if (call_instr.getReturnType().getTypeOfElement() == ElementType.OBJECTREF) {
                    code.append(((ClassType) call_instr.getReturnType()).getName() + ";" );
                }

                 */
                code.append(NL);
                break;

            case NEW:
                if (call_instr.getReturnType().getTypeOfElement() == ElementType.ARRAYREF){
                    for(var arg : call_instr.getArguments()){
                        code.append(getOperatorCases(arg));
                    }
                    code.append("newarray ");
                    var type_elements = ((ArrayType) call_instr.getReturnType()).getElementType();
                    code.append(getArrayElementsType(type_elements.getTypeOfElement()) + NL);
                }
                else{
                    code.append("new " + getClassName(ollirResult.getOllirClass(), ((Operand) call_instr.getOperands().get(0)).getName()) +  NL + "dup" + NL);
                }

                break;

            case ldc:
                var first_op = call_instr.getOperands().get(0);
                code.append(getOperatorCases(first_op)).append(NL);
                break;

            case arraylength:
                var tt = call_instr.getOperands().get(0);
                code.append(getOperatorCases(tt) + "arraylength").append(NL);
                break;
            default:
                code.append("");
        }

        return code.toString();
    }

    private String getOperatorCases(Element element){
        StringBuilder code = new StringBuilder();

        if (element instanceof ArrayOperand){
            return getOperatorArrayOperand((ArrayOperand) element);
        }

        if(element.isLiteral()){
            var literal = ((LiteralElement) element).getLiteral();
            if (element.getType().getTypeOfElement() != ElementType.INT32 && element.getType().getTypeOfElement() != ElementType.BOOLEAN){

            }
            else {
                int int_literal = Integer.parseInt(((LiteralElement) element).getLiteral());
                if (int_literal == -1) {
                    code.append("iconst_m1");
                    addtoStackValue();
                } else if (int_literal >= 0 && int_literal <= 5) {
                    code.append("iconst_").append(int_literal);
                    addtoStackValue();
                } else if (int_literal >= -128 && int_literal <= 127) {
                    code.append("bipush ").append(int_literal);
                    addtoStackValue();
                } else if (int_literal >= -32768 && int_literal <= 32767) {
                    code.append("sipush ").append(int_literal);
                    addtoStackValue();
                } else {
                    code.append("ldc ").append(int_literal);
                    addtoStackValue();
                }
                code.append(NL);

            }
            return code.toString();
        }
        else if(element.getType().getTypeOfElement() == ElementType.INT32 || element.getType().getTypeOfElement() == ElementType.BOOLEAN){
            var reg = currentMethod.getVarTable().get(((Operand) element).getName()).getVirtualReg();
            if(reg > 3){
                code.append("iload " + reg + NL);
            }
            else code.append("iload_" + reg + NL);
            addtoStackValue();
            return code.toString();
        }
        else if(element.getType().getTypeOfElement() == ElementType.OBJECTREF || element.getType().getTypeOfElement() == ElementType.STRING || element.getType().getTypeOfElement() == ElementType.ARRAYREF || element.getType().getTypeOfElement() == ElementType.THIS){
            var reg = currentMethod.getVarTable().get(((Operand) element).getName()).getVirtualReg();
            if(reg > 3){
                code.append("aload " + reg + NL);
            }
            else code.append("aload_" + reg + NL);
            addtoStackValue();
            return code.toString();
        }

        return "";
    }

    private String getOperatorArrayOperand(ArrayOperand array_op){
        var code = new StringBuilder();
        var reg = currentMethod.getVarTable().get(array_op.getName()).getVirtualReg();
        code.append("aload_" + reg + NL);
        addtoStackValue();
        code.append(getOperatorCases((array_op.getIndexOperands().get(0))));
        code.append("iaload" + NL);
        addtoStackValue();
        return code.toString();
    }

    private String generateSingleOpCond(SingleOpCondInstruction instruction){
        StringBuilder code = new StringBuilder();
        var info = "";
        info += "A";

        return "SingleOpCondition";
    }

    private String generateGoto(GotoInstruction instruction){
        StringBuilder code = new StringBuilder();
        var temp = ((Instruction) instruction);
        code.append("goto cmp_lt_" + instruction.getLabel());
        return code.toString();
    }

    private String generateOpCond(OpCondInstruction instruction){
        StringBuilder code = new StringBuilder();

        var first_part = generateBinaryOp(((BinaryOpInstruction) instruction.getCondition()));
        code.append(first_part);

        switch (((BinaryOpInstruction) instruction.getCondition()).getOperation().getOpType()){
            case LTH:
                code.append("iflt cmp_lt_" + instruction.getLabel());
                break;
            default:
                code.append("YET");
        }

        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction instruction){
        var code = new StringBuilder();

        var operand = getOperatorCases(instruction.getOperand());
        code.append(operand);


        var op_type = instruction.getOperation().getOpType();
        switch (op_type){
            case NOTB:
                code.append("ixor");
                break;
            default:
                code.append("Yet");
        }

        return code.toString() +NL;
    }

}


