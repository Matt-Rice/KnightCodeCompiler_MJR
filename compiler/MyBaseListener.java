/**
* Class that overrides some of the methods of the ANTLR generated base listener that will be responsible for performing bytecode operations for the grammar rules
* @author Matt Rice
* @version 1.0
* Assignment 5
* CS322 - Compiler Construction
* Spring 2024
**/
package compiler;

import lexparse.*;
import org.antlr.v4.runtime.ParserRuleContext; // need to debug every rule
//Explicit import for ASM bytecode constants
import org.objectweb.asm.*;  //classes for generating bytecode
import compiler.utils.*;
import java.util.*;
import java.lang.*;

public class MyBaseListener extends KnightCodeBaseListener{

    private ClassWriter cw;  //ClassWriter for a KnightCode class
	private MethodVisitor mainVisitor; //global MethodVisitor
	private String programName; //name of the class and the output file (used by ASM)
    private Map<String, Variable> symbolTable; //map that will store the name of the variable along with its corresponding Variable object which will contain some of its attributes
    private int memoryPointer;
    /**
     * Constructor for MyBaseListener
     * @param programName the name of the program
     */
    public MyBaseListener(String programName){
        this.programName = programName;
        
    }//end constructor

    /**
     * Method to setup the compiled program by initializing the ClassWriter, making the constructor, and starting the main method
     */
    public void beginClass(){
        
        // Set up the classwriter
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC,programName, null, "java/lang/Object",null);
        
        // Creating Constructor for the class
        {
			MethodVisitor mv=cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0); //load the first local variable: this
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V",false);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(1,1);
			mv.visitEnd();
		}

    }//end beginClass

    /**
     * Ends the main method and writes the ClassWriter data into the outputFile
     */
    public void closeClass(){

            mainVisitor.visitInsn(Opcodes.RETURN);
            mainVisitor.visitMaxs(0, 0);
            mainVisitor.visitEnd();
    
            cw.visitEnd();
    
                byte[] b = cw.toByteArray();
    
                Utilities.writeFile(b,this.programName+".class");
    
            System.out.println("\n\n\nCompiling Finished");
        
    }//end closeClass

    @Override
    /**
     * Begins the KnightCode class and is triggered once file is entered 
     */
    public void enterFile(KnightCodeParser.FileContext ctx){
        System.out.println("Entering File");

        programName = ctx.ID().getText();

        beginClass();
    }//end enterFile

    @Override
    /**
     * Closes the KnightCode class triggered once the end of the program is reached
     */
    public void exitFile(KnightCodeParser.FileContext ctx){
        
        closeClass();

        System.out.println("Exiting File");
    }//end exitFile

    @Override
    // triggered once declare is reached
    /**
     * Once Declare is entered, a HashMap for the symbol table will be initialized and the stack memory pointer will be set to zero
     */
    public void enterDeclare(KnightCodeParser.DeclareContext ctx){
        symbolTable = new HashMap<>();
        memoryPointer = 0;
    }//end enterDeclare

    @Override
    /**
     * Once variable is entered, the name and type will be used to instantiate a new Variable object using the attributes from the declaration and put it into the symbol table
     */
    public void enterVariable(KnightCodeParser.VariableContext ctx){
        String type = ctx.vartype().getText();
        String name = ctx.identifier().getText();
        Variable v = new Variable(name, type, memoryPointer++);
        symbolTable.put(name, v);
    }//end enterVariable

    @Override
    /**
     * Triggers when body is entered and initializes the main method
     */
    public void enterBody(KnightCodeParser.BodyContext ctx){
        // Start MethodVisitor for main method
        
        mainVisitor=cw.visitMethod(Opcodes.ACC_PUBLIC+Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mainVisitor.visitCode();
    }//end enterBody

    public void evalExpr(KnightCodeParser.ExprContext ctx){
        
        // If the expr is just a number reads and parses the text as an int and loads it to constant pool
        if (ctx instanceof KnightCodeParser.NumberContext){
            int value = Integer.parseInt(ctx.getText());
            
            //debug
            System.out.println("expr val " + value);
            mainVisitor.visitLdcInsn(value);
        }//number

        // If the expr is an identifier
        else if (ctx instanceof KnightCodeParser.IdContext){
            String id = ctx.getText();
            Variable var = symbolTable.get(id);
            
            //debug
            System.out.println("expr id " + id);

            // If type of the variable is INTEGER
            if (var.getType().equals("INTEGER")){
                mainVisitor.visitVarInsn(Opcodes.ILOAD, var.getLocation());
            }

            else if (var.getType().equals("STRING")){
                mainVisitor.visitVarInsn(Opcodes.ALOAD, var.getLocation());
            }
            
        }//id

        //Addition context
        else if (ctx instanceof KnightCodeParser.AdditionContext){

        }
            
    }//end evalExpr

    @Override
    /**
     * Is triggered when Setvar is entered and will define a previously declared variable
     */
    public void enterSetvar(KnightCodeParser.SetvarContext ctx){
        String varName = ctx.ID().getText(); 
        Variable var = symbolTable.get(varName);
        
        //Need to evaluate EXPR before setting stuff
        //Make a method that takes in expr context and checks for operators and such

        // If the variable was not previously declared
        // May do error handling in the future
        if (var == null){
            System.err.println(varName + " has not been declared yet");
            System.exit(1);
        }

        //Defines variable if it is an INTEGER
        if (var.getType().equals("INTEGER")){
            int value = Integer.parseInt(ctx.expr().getText());
            mainVisitor.visitIntInsn(Opcodes.SIPUSH, value);
            mainVisitor.visitVarInsn(Opcodes.ISTORE, var.getLocation());
        }
        
        //Defines variable if it is an STRING
        if (var.getType().equals("STRING")){
            String value = ctx.expr().getText();
            mainVisitor.visitLdcInsn(value);
            mainVisitor.visitVarInsn(Opcodes.ASTORE, var.getLocation());
        }

    }//end enterSetvar
    
}//end MyBaseListener
 