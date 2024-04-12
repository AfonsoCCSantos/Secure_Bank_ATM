## How to run

The project was implemented using Java and therefore we provide two .jar files. One jar file to run the Bank and one jar file to run the ATM. They are named Bank and ATM respectively. 

### Running the Bank
To run the Bank, use: <br>
`java -jar Bank.jar -p <port> -s <auth-file>` <br>
(The port and the auth-file are optional arguments as said in the project description).

### Running the ATM
To run the ATM, use: <br>
`java -jar ATM.jar -a <account>` <br>
(The account parameter is required, other parameters described in the project description can be used following the same flag logic). A parameter for the operation is always necessary, as explicited in the project description. <br>

After running the Bank.jar file. The generated auth file should be placed in the directory where the ATM.jar will be run.



