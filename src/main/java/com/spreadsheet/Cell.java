package com.spreadsheet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cell {
    String id;
    String formula;
    int value;
    SSEngine engine=SSEngine.getSSEngine();
    ArrayList<String> operands=new ArrayList<>();
    ArrayList<String> operators=new ArrayList<>();
    // definition of the set of operators
    final Pattern p = Pattern.compile("([*]|[+]|[-]|[/])");

    public Cell(String id, String formula) {
        this.id = id;
        this.formula = formula;
        this.value = 0;
        parseFormula();
    }

    public String getId() {
        return id;
    }

    public String getFormula() {return formula;}

    public int getValue() {return value;}

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ID: ").append(id)
                .append("\n Value: ").append(value)
                .append("\n Formula: ").append(formula)
                .append("\n Operands: ").append(printOperands())
                .append("\n Operators: ").append(printOperators())
                .append("\n ===================");
        return sb.toString();
    }

    @Override
    public Cell clone(){
        Cell clone=new Cell(this.id,this.formula);
        clone.value=this.value;
        return clone;
    }
    // INTERNAL METHODS - do not call ---------------------------------------
     void setFormula(String formula) {
        this.formula = formula;
        parseFormula();
    }

    static boolean isOperandNumeric(String op){
        boolean retval=true;
        try {
            Integer.parseInt(op);
        } catch (NumberFormatException e){
            retval=false;
        }
        return retval;
    }

    private void parseFormula() {
        if (!formula.startsWith("=")) {
            try {
                value = Integer.parseInt(formula);
            } catch (NumberFormatException ex) {
                value = 0;
            }
        } else {
            String stringToParse = formula.substring(1);
            operators.clear();
            operands.clear();
            boolean fail=false;
            //Pattern p = Pattern.compile("([*]|[+]|[-]|[/])");
            Matcher m = p.matcher(stringToParse);
            int laststart = 0;
            String operand=null;
            while (m.find()) {
                String op = m.group();
                operators.add(op);
                int a = m.start();
                operand = stringToParse.substring(laststart, a);
                if (operand==null||"".equals(operand)) operand="null";
                //System.out.println("---------"+operand);
                laststart = a + 1;
                operands.add(operand);
                if (!isOperandNumeric(operand) && !engine.cellMap.containsKey(operand)) {
                        fail = true;
                }
            }
            operand = stringToParse.substring(laststart, stringToParse.length());
            if (operand==null||"".equals(operand)) operand="null";
            //System.out.println("---------"+operand);
            operands.add(operand);
            if (!isOperandNumeric(operand) && !engine.cellMap.containsKey(operand)) {
                fail = true;
            }
            if (fail) {
                System.out.println("ERROR IN STRING PARSING");
                formula = formula + " ### ERROR IN FORMULA ###";
                value = 0;
                operands.clear();
                operators.clear();
            }
        }
    }
    /**
     * returns true if no circular depencencies are found starting from this cell
     * @param id
     * @return
     */
     boolean checkCircularDependencies(String id){
        SSEngine ssEngine=SSEngine.getSSEngine();
        // this cell depends on its operands
        for (String s:operands) {
            // check from first level dependencies
            if (s.equals(id)) {
                System.out.println("------- ERROR!---------CIRCULAR DEPENDENCIES!");
                return false;
            }
            // recursively check all operand chains
            Cell x=ssEngine.cellMap.get(s);
            if (x!=null)
                if (! x.checkCircularDependencies(id)) {
                    System.out.println("------- ERROR!---------CIRCULAR DEPENDENCIES!");
                    return false;
                }
        }
        return true;
    }

    private boolean evaluateCell(){
        if (operators.isEmpty()) {
            //no operators, there is either a single reference or a value
            if (operands.isEmpty()) { // value
                //do nothing
            } else { // single reference
                String operandID = operands.get(0);
                value=getOperandValue(operandID);
            }
        } else {
            int tempValue=getOperandValue(operands.get(0));
            int index=0;
            for (String op:operators){
                String n1=""+tempValue;
                String n2=operands.get(++index);
                tempValue=compute(op, n1,n2);
            }
            value=tempValue;
        }
        // now update all cells which depend from this cell

        return true;
    }
    Set<Cell> recursiveEvaluateCell() {
        Set<Cell> affectedCells=new HashSet<>();
        evaluateCell();
        affectedCells.add(this);
        for (Cell x: engine.dependance.get(id)) {
            Set<Cell> subset=x.recursiveEvaluateCell();
            affectedCells.addAll(subset);
        }
        return affectedCells;
    }
    private int getOperandValue(String s){
        int value=0;
        try {
            value = Integer.parseInt(s);
        }catch (NumberFormatException e) {
            Cell x=engine.cellMap.get(s);
            if (s!=null) {
                value = engine.cellMap.get(s).value;
            }
        }
        //System.out.println("===> Operand: "+s+" value:"+value);
        return value;
    }
    private int compute(String operator, String operand1,String operand2){
        //System.out.println("===> Computing "+operand1+operator+operand2);
        int x=getOperandValue(operand1);
        int y=getOperandValue(operand2);
        switch (operator) {
            case "+":
                return (x+y);
            case "-":
                return (x-y);
            case "*":
                return (x*y);
            case "/":
                return (x/y);
        }
        return 0; // it never comes here
    }
    private String printOperands(){
        StringBuffer sb=new StringBuffer("");
        for (String op:operands)
            sb.append(op).append(" ");
        return sb.toString();
    }
    private String printOperators(){
        StringBuffer sb=new StringBuffer("");
        for (String op:operators)
            sb.append(op).append(" ");
        return sb.toString();
    }

    // testing method =========================================================
    private static void createAndAddCell(String id, String formula){
        SSEngine ssEngine=SSEngine.getSSEngine();
        Cell c=new Cell(id,formula);
        System.out.println(c);
        boolean isOK=c.checkCircularDependencies(c.id);
        if (isOK) {
            System.out.println("it.unitn.ronchet.Spreadsheet.Cell "+c.id+" has no circular references");
            ssEngine.addCell(c);
        } else {
            System.out.println("it.unitn.ronchet.Spreadsheet.Cell "+c.id+" has circular references - not added");
        }
        System.out.println("====================================");
    }
    public static void main (String arg[]) {
        createAndAddCell("A1","=A1+B1*C3-B2"); // not OK
        createAndAddCell("B2","=D1+B1"); // OK
        createAndAddCell("A1","=D1+B1*D1-B2"); // OK
        createAndAddCell("D1","=A1-B2"); //not OK
    }
}
