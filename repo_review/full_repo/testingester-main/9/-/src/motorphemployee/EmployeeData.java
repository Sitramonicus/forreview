package motorphemployee;

import java.io.*;

public class EmployeeData {
   
    public static void main(String [] args){
    
    String csvFile = "/Users/set/Downloads/Employee Data.csv";
    String line = "";
    
    try {
    BufferedReader br = new BufferedReader(new FileReader(csvFile));
    
    
    while((line = br.readLine()) != null) {
        System.out.println(line);
    
    }
   } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}