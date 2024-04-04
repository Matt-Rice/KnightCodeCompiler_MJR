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

        // Start MethodVisitor for main method
        
            mainVisitor=cw.visitMethod(Opcodes.ACC_PUBLIC+Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mainVisitor.visitCode();

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
     * Begins the KnightCode class
     */
    public void enterFile(KnightCodeParser.FileContext ctx){
        System.out.println("Entering File");

        beginClass();
    }//end enterFile

    @Override
    /**
     * Closes the KnightCode class
     */
    public void exitFile(KnightCodeParser.FileContext ctx){
        
        closeClass();

        System.out.println("Exiting File");
    }//end exitFile





}//end MyBaseListener
 