 /**
* Class that overrides some of the methods of the ANTLR generated BaseVisitor class that will be responsible for performing bytecode operations for the grammar rules
* @author Matt Rice
* @version 1.0
* Assignment 5
* CS322 - Compiler Construction
* Spring 2024
**/
package compiler;

import lexparse.*;
import org.objectweb.asm.*;  //classes for generating bytecode
import compiler.utils.*;
import java.util.*;


public class MyBaseVisitor extends KnightCodeBaseVisitor<Object>{

    private ClassWriter cw;  //ClassWriter for a KnightCode class
	private MethodVisitor mainVisitor; //global MethodVisitor
	private String programName; //name of the output file
    private Map<String, Variable> symbolTable; //map that will store the name of the variable along with its corresponding Variable object which will contain some of its attributes
    private int memoryPointer; //Memory pointer to the top of the stack


    /**
     * Constructor for MyBaseVisitor
     * @param programName the name of the program
     */
    public MyBaseVisitor(String programName){
        this.programName = programName;
        
    }//end constructor

    /**
     * Method that removes the first and last characters of a string (Will be used to remove quotes around Strings when printing)
     * @param s the string that will be modified
     * @return the string without the first and last characters
     */
    public String removeFirstandLast(String s){
        return s.substring(1, s.length() -1);
    }//end removeFirstandLast

    /**
     * Method that prints the key value pairs from the SymbolTable
     */
    public void printSymbolTable(){
        System.out.println("SymbolTable");
        for (Map.Entry<String, Variable> entry : symbolTable.entrySet()){
            System.out.println("Key: " + entry.getKey() + " Var: " + entry.getValue().toString());
        }
    }//end printSymbolTable

    /**
     * Method that will set up the ClassWriter and create the constructor
     * @param name the name of the program that will be created
     */
    public void beginClass(String name){
        
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

            //Ends the mainVisitor
            mainVisitor.visitInsn(Opcodes.RETURN);
            mainVisitor.visitMaxs(0, 0);
            mainVisitor.visitEnd();

            //Ends the ClassWriter
            cw.visitEnd();

                //Creates an array of type byte that contains the contents of the ClassWriter
                byte[] b = cw.toByteArray();

                //Writes the byte array to the output file
                Utilities.writeFile(b,this.programName+".class");
        
            //Let the user know that compiling has finished
            System.out.println("Compiling Finished for: " + this.programName);
        
    }//end closeClass

    @Override
    /**
     * Calls the beginClass method which creates the ClassWriter and constructor for the KnightCode class 
     * @param ctx the parser tree context of the file
     * @return the visitor result
     */
    public Object visitFile(KnightCodeParser.FileContext ctx){
        
        //Begins the class
        beginClass(programName);
        return super.visitFile(ctx);
    }//end visitFile

    @Override
    /**
     * Once Declare is visited, a HashMap for the symbol table will be initialized and the stack memory pointer will be set to zero
     * @param ctx the parser tree context of the declaration
     * @return the visitor result
     */
    public Object visitDeclare(KnightCodeParser.DeclareContext ctx){
        
        //Initializes the symbol table hashmap
        symbolTable = new HashMap<>();

        //Initializes the memoryPointer to the top of the stack at 0
        memoryPointer = 0;

        return super.visitDeclare(ctx);
    }//end visitDeclare

    @Override
    /**
     * Once variable is visited, the name and type will be used to instantiate a new Variable object using the attributes from the declaration and put it into the symbol table
     * @param ctx the parser tree context of the vairable
     * @return the visitor result
     */
    public Object visitVariable(KnightCodeParser.VariableContext ctx){
        
        //Gets the type of the variable
        String type = ctx.vartype().getText();

        // Check if declared type is unsupported
        if (!type.equals("INTEGER") && !type.equals("STRING")){
            System.err.println("Compilation error: the entered type is not supported.");
            System.exit(1);
        }

        // Creates the variable and adds it to the symbol table
        String name = ctx.identifier().getText();
        Variable v = new Variable(name, type, memoryPointer++);
        symbolTable.put(name, v);

        return super.visitVariable(ctx);
    }//end visitVariable

    @Override
    /**
     * Method that visits the body and initializes the main method
     * @param ctx the parser tree context of the body
     * @return the visitor result
     */
    public Object visitBody(KnightCodeParser.BodyContext ctx){  
        // Start MethodVisitor for main method
        mainVisitor=cw.visitMethod(Opcodes.ACC_PUBLIC+Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mainVisitor.visitCode();

        return super.visitBody(ctx);
    }//end visitBody
    
    /**
     * Method that will evaluate an expression depending on what type of context it is an instance of. It uses recursion so that once it is broken down to a terminal, it can be loaded so operations can be performed
     * @param ctx the context of the expr that is to be evaluated
     */
    public void evalExpr(KnightCodeParser.ExprContext ctx){
        
        //If the expr is just a number reads and parses the text as an int and loads it to constant pool
        if (ctx instanceof KnightCodeParser.NumberContext){
            //Gets value of the number
            int value = Integer.parseInt(ctx.getText());

            //Loads value to constant pool
            mainVisitor.visitLdcInsn(value);
        }//number

        //If the expr is an instance of identifier
        else if (ctx instanceof KnightCodeParser.IdContext){
            //Gets the identifier
            String id = ctx.getText();

            //Creates a variable reference object for the variable stored in the symbol table with the given id
            Variable var = symbolTable.get(id);
            
            //If type of the variable is INTEGER
            if (var.getType().equals("INTEGER")){
                //Loads the information where the variable is stored at
                mainVisitor.visitVarInsn(Opcodes.ILOAD, var.getLocation());
            }

            //If type of the variable is STRING
            else if (var.getType().equals("STRING")){
                mainVisitor.visitVarInsn(Opcodes.ALOAD, var.getLocation());
            } 
            
        }//id   

        //If the expr is an instance of Subtraction
        else if (ctx instanceof KnightCodeParser.SubtractionContext){
            
            //Loop that evaluates each expression from the context so subtraction can be performed
            for(KnightCodeParser.ExprContext expr : ((KnightCodeParser.SubtractionContext)ctx).expr()){
                evalExpr(expr);
            }//for
        
        //Load the subtraction opcode
        mainVisitor.visitInsn(Opcodes.ISUB);
            
        }//sub

        //If the expr is an instance of Addition
        else if (ctx instanceof KnightCodeParser.AdditionContext){

            //Loop that evaluates each expression from the context so addition can be performed
            for(KnightCodeParser.ExprContext expr : ((KnightCodeParser.AdditionContext)ctx).expr()){
                evalExpr(expr);
            }//for
        
        //Load the addition opcode
        mainVisitor.visitInsn(Opcodes.IADD);
            
        }//add

        //If the expr is an instance of Multiplication
        else if (ctx instanceof KnightCodeParser.MultiplicationContext){
            
            //Loop that evaluates each expression from the context so multiplication can be performed
            for(KnightCodeParser.ExprContext expr : ((KnightCodeParser.MultiplicationContext)ctx).expr()){
                evalExpr(expr);
            }//for
        
        //Load the multiplication opcode
        mainVisitor.visitInsn(Opcodes.IMUL);
            
        }//mul

        //If the expr is an instance of Division
        else if (ctx instanceof KnightCodeParser.DivisionContext){
            
            //Loop that evaluates each expression from the context so division can be performed
            for(KnightCodeParser.ExprContext expr : ((KnightCodeParser.DivisionContext)ctx).expr()){
                evalExpr(expr);
            }//for
        
        //Load the division opcode
        mainVisitor.visitInsn(Opcodes.IDIV);
            
        }//div

    }//end evalExpr


    @Override
    /**
     * Method that when visiting a comparison, will perform the comparison operation and if true load one, if false load 0
     * @param ctx the parser tree context of the comparison
     * @return the visitor result
     */
    public Object visitComparison(KnightCodeParser.ComparisonContext ctx){
        
        Label trueLabel = new Label();//Label for true
        Label endLabel = new Label();//Label for false

        //Get the comparison operator
        String op = ctx.comp().getText();

        //Evaluates both of the expressions to make sure they are loaded
        evalExpr(ctx.expr(0));
        evalExpr(ctx.expr(1));

        //Switch statement that decides which comparison instruction should be loaded
        switch (op) {
            case ">":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPGT, trueLabel);
                break;
        
            case "<":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPLT, trueLabel);
                break;

            case "=":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, trueLabel);
                break;
            case "<>":
            mainVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, trueLabel);
                break;
        }

        //If not true, load 0 and jump to end
        mainVisitor.visitLdcInsn(0);
        mainVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

        //If true load 1
        mainVisitor.visitLabel(trueLabel);
        mainVisitor.visitLdcInsn(1);

        //Visit the end label
        mainVisitor.visitLabel(endLabel);

        return super.visitComparison(ctx);
    }//end visitComparison


    /**
     * Method that will check if a string is either a number or an identifier in the symbol table and will load it accordingly
     * @param operand the string with the ID or value to be loaded
     */
    public void loadInteger(String operand){
        
        //Memory loaction of the variable
        int location;
        
        //If the string is a key of the symbol table ("It's the ID of a variable")
        if (symbolTable.containsKey(operand)){
            Variable var = symbolTable.get(operand);
            location = var.getLocation();
            mainVisitor.visitVarInsn(Opcodes.ILOAD, location);
        }
        //If it's a number
        else {
            mainVisitor.visitLdcInsn(Integer.parseInt(operand));
        }
    }//end loadInteger
    
    @Override
    /**
     * Method that handles the logic for a simple IF THEN ELSE logic based off of a comparison using jumps
     * @param ctx the parser tree context of the decision
     * @return null
     */
    public Object visitDecision(KnightCodeParser.DecisionContext ctx){
        
        //Labels used for jumping
        Label trueLabel = new Label();//if the comp is true
        Label endLabel = new Label();//the end label
        
        
        //Load the children to be compared
        String num1 = ctx.getChild(1).getText();
        String num2 = ctx.getChild(3).getText();

        loadInteger(num1);
        loadInteger(num2);

        //Gets the comparison operator
        String op = ctx.comp().getText();
        
        //Handles whether or not it will jump to the IF THEN block
        switch (op) {
            case ">":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPGT, trueLabel);
                break;
        
            case "<":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPLT, trueLabel);
                break;

            case "=":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, trueLabel);
                break;
            case "<>":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, trueLabel);
                break;
        }

        //Check to see if there is an else statement
        boolean hasElse = false; //true if there is an else statement
        int endLocation = 6; //minimum location of the endif

         //Loop that figures out the location of the endif
         while (!ctx.getChild(endLocation).getText().equals("ENDIF")){
            endLocation++;
        }  
        
        //Checks to see if an else statement is included
        for(int i = 0; i<ctx.children.size(); i++){
            if(ctx.getChild(i).getText().equals("ELSE")){
                hasElse = true;
                break;
            }
        }

        int elseLocation = 6; // least possible child index for else location (IF x comp y THEN stat ELSE)
        
        //Handles else block if there is an else block
        if(hasElse){
    
            //Loop that figures out how many stats there are in the if block
            while (!ctx.getChild(elseLocation).getText().equals("ELSE")){
                elseLocation++;
            }  
            
            //ELSE
            //Loop that runs all of the stats within the else block
            for(int i = elseLocation+1; i<ctx.getChildCount(); i++){
                visit(ctx.getChild(i));
            }
        }

        //Jump to end after else or no else has executed and the comp was false
        mainVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

        //IF THEN
        // Go here when comparison is true
        mainVisitor.visitLabel(trueLabel);

        //handles the if, when there is an else
        if(hasElse){
            //Starts at location of first stat of the then block and visits stats until reaches the location of the else
            for (int i = 5; i< elseLocation;i++){
                visit(ctx.getChild(i));
            }
        }
        //Handles if the if when there is no else
        else{
            //Starts at location of first stat in the if block and visits stats until it reaches location of the ENDIF
            for (int i = 5; i< endLocation;i++){
                visit(ctx.getChild(i));
            }
        }

        //End label
        mainVisitor.visitLabel(endLabel);

        return null;

    }//end visitDecision

    @Override
    /**
     * Is triggered when Setvar is visited and will define a previously declared variable
     * @param ctx the parser tree context of the setVar
     * @return the visitor result
     */
    public Object visitSetvar(KnightCodeParser.SetvarContext ctx){
        
        //Name of variable to be set
        String varName = ctx.ID().getText(); 

        //Creates variable object for the variable
        Variable var = symbolTable.get(varName);
        
        //Stops the visiting if the variable was not previously declared
        if (var == null){
            System.err.println("ERROR: " + varName + " has not been declared yet");
            System.exit(1);
        }
        //Evaluates the expressions before storing
        else if(ctx.expr() != null){
            evalExpr(ctx.expr());

            //Defines variable if it is an INTEGER
            if (var.getType().equals("INTEGER")){
                mainVisitor.visitVarInsn(Opcodes.ISTORE, var.getLocation());
            }
            
        }
        //Defines variable if it is an STRING
        else if (var.getType().equals("STRING") && ctx.STRING() != null){
            String str = removeFirstandLast(ctx.STRING().getText());
            mainVisitor.visitLdcInsn(str);
            mainVisitor.visitVarInsn(Opcodes.ASTORE, var.getLocation());
        } 
        
        return super.visitSetvar(ctx);
        
    }//end visitSetvar

    @Override
    /**
     * Is triggered whenever print is encountered and will either print out the value of the identifier specified, or a string that is specified
     * @param ctx the parser tree context of the print
     * @return the visitor result
     */
    public Object visitPrint(KnightCodeParser.PrintContext ctx){
       
        //Fetches the print stream field
        mainVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

        // If the subject of the printing is an ID then it searches and finds its stack location so it can be loaded to be printed
        if(ctx.ID() != null){   
            String varID = ctx.ID().getText();
            Variable var = symbolTable.get(varID);
            int location = var.getLocation(); //location of the variable

            if (var.getType().equals("INTEGER")){
                mainVisitor.visitVarInsn(Opcodes.ILOAD, location);
                mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
            }
            else{
                mainVisitor.visitVarInsn(Opcodes.ALOAD, location);
                mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            }
        }

        //If the subject is a String, it will load the string to the constant pool
        else if(ctx.STRING()!=null){
            String str = removeFirstandLast(ctx.STRING().getText());
            mainVisitor.visitLdcInsn(str);
            mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
        return super.visitPrint(ctx);
    }//end visitPrint

    
    @Override
    /**
     * Method that will read an input from the user and store it in the variable whose identifier follows the read call 
     * @param ctx the parser tree context of the read
     * @return the visitor result
     */
    public Object visitRead(KnightCodeParser.ReadContext ctx){
        
        //Initializes the variable that will store the value inputted by the user
        Variable var = symbolTable.get(ctx.ID().getText());

        //Sets the location where the scanner will be loaded to as the next valuable in the memory location
        int scanLocation = memoryPointer++;

        // Initializes the Scanner object
        mainVisitor.visitTypeInsn(Opcodes.NEW, "java/util/Scanner"); // Creates Scanner and pushes it to the stack
        mainVisitor.visitInsn(Opcodes.DUP); // Duplicates the Scanner reference which will be used in initializing and storing the scanner
        mainVisitor.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;"); // System.in
        mainVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false); // Initializes Scanner
        mainVisitor.visitVarInsn(Opcodes.ASTORE, scanLocation); // Stores Scanner

        //Handles if variable is of type int
        if (var.getType().equals("INTEGER")){

            // Read integer input from the user
            mainVisitor.visitVarInsn(Opcodes.ALOAD, scanLocation); // Loads scanner
            mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Scanner", "nextInt", "()I", false); // Scan.nextLong()
            mainVisitor.visitVarInsn(Opcodes.ISTORE, var.getLocation()); // Store the int value in a variable
        }
        
        //Handles if variable is of type String
        else if (var.getType().equals("STRING")){
            
            // Read String from the user
            mainVisitor.visitVarInsn(Opcodes.ALOAD, scanLocation); // Loads scanner
            mainVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Scanner", "nextLine", "()Ljava/lang/String;", false); // Scan.nextLong()
            mainVisitor.visitVarInsn(Opcodes.ASTORE, var.getLocation()); // Store the String value in a variable
        }

        return super.visitRead(ctx);
    }//end visitRead
    
    @Override
    /**
     * Method that will handle the logic for a simple while loop
     * @param ctx the parser tree context of the loop
     * @return null
     */
    public Object visitLoop(KnightCodeParser.LoopContext ctx){
        
        //Labels used for jumping
        Label beginLabel = new Label(); //beginning of loop
        Label endLoop = new Label(); //leaves the loop
        
        //Begin loop Label
        mainVisitor.visitLabel(beginLabel);

        //Load the children to be compared
        String num1 = ctx.getChild(1).getText();
        String num2 = ctx.getChild(3).getText();

        loadInteger(num1);
        loadInteger(num2);

        //Gets the comparison operator
        String op = ctx.comp().getText();
        
        //Handles whether or not it will jump to endLoop
        //Loads the jump opposite of the operator because it needs to jump to the end of the loop when the comparison is not true
        switch (op) {
            case ">":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPLE, endLoop);
                break;
        
            case "<":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, endLoop);
                break;

            case "=":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, endLoop);
                break;
            case "<>":
                mainVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, endLoop);
                break;
        }//end switch

        //Loop that visits all of the stats within the while block
        for(int i = 5; i<ctx.getChildCount(); i++){
            visit(ctx.getChild(i));
        }
        
        //Jumps back to top if loop is set to execute
        mainVisitor.visitJumpInsn(Opcodes.GOTO, beginLabel);
        
        //End label
        mainVisitor.visitLabel(endLoop);

        return null;

    }//end visitLoop

}//end MyBaseVisitor
 