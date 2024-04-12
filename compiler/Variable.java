package compiler;

public class Variable {

    
    public String variableType = ""; //Type of the variable
    public String name = ""; //Value of the variable
    public int memoryLocation;//Location of variable on the stack

    /**
     * Constructor for a variable
     * @param variableType the type of a given variable
     * @param name the name of the variable
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

    public String toString(){
        return "Name: " + name + "Type: " + variableType + " Memory loc: " + memoryLocation;
    }
    
}//end Variable
