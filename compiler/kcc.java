/**
* Class that kicks off the compiler and accepts two command line arguments: 1 the name of the input .kcc file, the name of the file that will be outputted. Path to files must be included.
* @author Matt Rice
* @version 1.0
* Assignment 5
* CS322 - Compiler Construction
* Spring 2024
**/
package compiler;

import lexparse.*;//lexparse directory
import java.io.IOException;

//ANTLR packages
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.*;


public class kcc {

    public static void main(String[] args){
        
        CharStream input; //CharStream for input .kcc file
        KnightCodeLexer lexer; //lexer for input file
        CommonTokenStream tokens; //tokens of the input file
        KnightCodeParser parser; //Parser for the input file
        String output; //name for the output
      
        // Handles if args are not entered properly
        if (args.length < 2) {
            System.err.println("Run as: java compiler/kcc <pathToIp/input.kcc> <pathToOp/output>\nReplace pathToIp with the directory of the input file and pathToOp with the desired output location");
            return;
        }
      
        try{
            input = CharStreams.fromFileName(args[0]);  //get the input
            
            lexer = new KnightCodeLexer(input); //create the lexer
            tokens = new CommonTokenStream(lexer); //create the token stream
            parser = new KnightCodeParser(tokens); //create the parser
            output = args[1]; //get the output

            ParseTree tree = parser.file();  //set the start location of the parser
             
            MyBaseVisitor visitor = new MyBaseVisitor(output);//MyBaseVisitor object with the program name of the output which will be used to visit the tree
            
            //Visits the tree
            visitor.visit(tree);

            //Closes the class and writes out the result to the output loation
            visitor.closeClass();
        }
        catch(IOException e){
            System.out.println("Please make sure that the path to the files are correct and run again.");
        }
    
    }//end main
}//end kcc
