 /**
* Class that defines a Variable constructor and provides methods to get its attributes
* @author Matt Rice
* @version 1.0
* Assignment 5
* CS322 - Compiler Construction
* Spring 2024
**/
package compiler;

public class Variable {

    
    public String variableType = ""; //Type of the variable
    public String name = ""; //Value of the variable
    public int memoryLocation;//Location of variable on the stack

    /**
     * Constructor for a variable
     * @param variableType the type of a given variable
     * @param name the name of the variable
     * @param memoryLocation the memory location of the variable on the stack
     */
    public Variable(String name, String variableType, int memoryLocation){
        
        this.variableType = variableType;
        this.name = name;
        this.memoryLocation = memoryLocation;

    }//end constructor

    /**
     * Returns the variable type of the variable
     * @return the variable type
     */
    public String getType(){
        return variableType;
    }//end getType

    /**
     * Returns the name of the variable
     * @return the name of the variable
     */
    public String getName(){
        return name;
    }//end getName

    /**
     * Returns the memory location of the variable
     * @return the memory location of the variable
     */
    public int getLocation(){
        return memoryLocation;
    }// end getLocation

    /**
     * Returns each of the attributes of a variable as a string
     * @return a String containing all of the attributes of the Variable
     */
    public String toString(){
        return "Name: " + name + " Type: " + variableType + " Location: " + memoryLocation;
    }
    
}//end Variable
