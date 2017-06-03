package codegeneration;

import cli.Resources;
import cli.Resources.DataType;
import cli.Resources.JSONType;
import parser.Node;
import parser.SymbolTable;

import java.util.ArrayList;

public class CodeGenerator {
    private String                 code      = null;
    private String                 filename  = null;
    private String                 filepath  = null;
    private Node                   hir       = null;
    private ArrayList<SymbolTable> st        = null;
    private String                 spacement = null;

    public CodeGenerator(Node hir, ArrayList<SymbolTable> st){
        this.hir = hir;
        this.st = st;
        this.spacement = new String("");
    }

    public void run(){
        code = generate(hir);
    }

    private String generate(Node node){
        String content = new String("");

        Resources.JSONType type = node.getType();

        //Displays
        System.out.println(node.getType() + " " + node.getSpecification());
        if(node.getReference() != null)
            System.out.println(" " + node.getReference().getName() + " " + node.getReference().getType() + "\n");

        switch (type) {
            case START:{
                for(Node n : node.getAdj())
                    content += generate(n) + "\n";
                break;
            }
            case FUNCTION:{
                content += handleFunction(node.getSpecification(),node.getAdj());
                break;
            }
            case VARIABLEDECLARATION:{
                content += handleVariableDeclaration(node);
                break;
            }
            case ASSIGNMENT:{
                content += handleAssignment(node,node.getAdj());
                break;
            }
            case IDENTIFIER:{
                content += node.getReference().getName();
                break;
            }
            case LITERAL:{
                content += node.getSpecification();
                break;
            }
            case OPERATION:{
                content += handleOperation(node,node.getAdj());
                break;
            }
            case IFSTATEMENT:{
                content += handleIf(node.getAdj());
                break;
            }
            case ARRAYLOAD:{
                content += handleArray(node.getAdj());
                break;
            }
            case ARRAYDECLARATION:{
                content += handleArrayContent(node.getAdj());
                break;
            }
            //Conditions
            //Loops
            default:
                break;
        }

        return content;
    }

    private String handleFunction(String name,ArrayList<Node> children){
        boolean lastParam = false, firstParam = true;

        String code = new String(spacement + "function " + name + "(");

        for(Node c : children){
            //parameters
            if(c.getType() == Resources.JSONType.PARAM){
                if(firstParam)
                    firstParam = false;
                else
                    code += ",";
                code+= c.getReference().getName();
            }
            //return
            else if(c.getType() == Resources.JSONType.RETURN){
                code += "\n\n" + spacement + "return " + generate(c.getAdj().get(0)) + ";";
            }
            //body
            else{
                if(!lastParam){
                    lastParam = true;
                    code+= ")\n" + spacement + "{";
                    spacement += Resources.DEF_SPC;				//add 1 tab
                }
                code+="\n" + spacement + generate(c) + endPunctuation(c.getType());
            }
        }

        //In case of no parameters or body
        if(!lastParam){
            code+= ")\n" + spacement + "{";
            spacement += Resources.DEF_SPC;
        }

        spacement = spacement.replaceFirst(Resources.DEF_SPC, "");			//rem 1 tab
        code+= spacement + "\n}";
        return code;
    }

    private String handleVariableDeclaration(Node node){
        boolean firstDeclaration = true;

        String code = new String("");

        //Multiple declarations in line
        if(node.getAdj().size() > 0 && node.getAdj().get(0).getType() == JSONType.VARIABLEDECLARATION){
            for(Node n : node.getAdj()){
                System.out.println(n.getReference() + n.getSpecification() + n.getType());

                if(firstDeclaration){
                    firstDeclaration = false;
                    code += getType(n) + " ";
                }
                else code += ", ";
                code += generate(n);
            }
        }
        //Individual declaration
        else{
            code += node.getReference().getName();

            //If direct assignment
            for(Node n : node.getAdj()){
                code += " = " + generate(n);
            }
        }

        return code;
    }

    private String handleAssignment(Node node, ArrayList<Node> assignment){
        String code = new String("");
        int i = 0;

        code += node.getReference().getName();

        if(node.getSpecification().equals("storearray")){
            /*
            Tratar de arrays
             */
        }

        while(i < node.getAdj().size()){
            code += " = " + generate(node.getAdj().get(i));
            i++;
        }
        return code;
    }

    public String handleArray(ArrayList<Node> subnodes){
        String code = new String("");
        boolean index = false;
        for(Node n : subnodes){
            if(index)
                code += "[" + generate(n) + "]";
            else{
                code += generate(n);
                index = true;
            }
        }
        return code;
    }

    public String handleArrayContent(ArrayList<Node> subnodes){
        String code = new String("[");
        boolean first = true;

        for(Node n : subnodes){
            if(first) first = false;
            else code += ", ";
            code +=generate(n);
        }
        code+="]";
        return code;
    }

    public String handleOperation(Node node, ArrayList<Node> subnodes){
        String code = new String("");

        if(node.getSpecification().equals("!")){
            code += "!(" + generate(subnodes.get(0)) + ")";
        }
        else{
            code += "(" + generate(node.getAdj().get(0)); //right
            code += " " + node.getSpecification() + " ";        //operation
            code += generate(node.getAdj().get(1)) + ")"; //left
        }


        return code;
    }

    public String handleIf(ArrayList<Node> subnodes){
        String code = new String("");
        code += "\n" + spacement + "if(" + generate(subnodes.get(0)) + ")\n" +
                  spacement +  "{\n";

        spacement += Resources.DEF_SPC;

        //body
        for(int i = 1; i < subnodes.size(); i++)
            code += spacement + generate(subnodes.get(i)) + endPunctuation(subnodes.get(i).getType()) +  "\n";

        spacement = spacement.replaceFirst(Resources.DEF_SPC, "");
        code += spacement +  "}\n";

        return code;
    }

    public String getCode(){
        return code;
    }

    public String endPunctuation(JSONType type){
        if(!(type.equals(JSONType.IFSTATEMENT) || type.equals(JSONType.WHILESTATEMENT)))
            return ";";
        else
            return "";
    }

    public DataType getType(Node n){
        if(n.getReference() == null && n.getAdj().size() != 0){
            return getType(n.getAdj().get(0));
        }
        else
            return n.getReference().getType();
    }
}