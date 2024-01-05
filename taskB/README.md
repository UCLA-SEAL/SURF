In this task, you will inspect examples using the SecureRandom API for generating keys. Some examples have been marked as positive or negative. In particular, we are interested in the use of SecureRandom, when System.currentTimeMillis() is passed as a seed, for using KeyPairGenerator. Your goal is to answer the following questions to understand the distribution of API use and detect more examples similar to the given positive examples.

You have 12 mins in total. If you are close to running out of time (i.e., 2 mins left), skip (past the dataset comprehension questions) directly to the last part of the task (constructing a pattern). 


From code/meteor_app, run `sh run_random.sh`
Go to localhost:3000.

Click on "predefined examples". 
Scroll down and click on the Infer Pattern button at the bottom of the page.


Now, on the left pane, we see the inferred pattern “SecureRandom.setSeed(...System.currentTimeMillis()...)”. Examples matching this pattern sets the random seed with the system’s current time. 
This pattern is a starting point for analyzing these examples. The right panel now shows possible features for inclusion in the pattern. SURF offers tools for analyzing the examples that match the pattern. 

1. What is one argument to SecureRandom.setSeed?

2. Which examples invoke KeyPairGenerator.getInstance() but not KeyPairGenerator.generateKeyPair()?

3. How many examples (that sets System.currentTimeMillis() as a seed) construct a new SecureRandom through its constructor, i.e., new SecureRandom(..)?

4. Which examples (that sets System.currentTimeMillis() as a seed) do not call KeyPairGenerator.getInstance()?

5. How many unlabelled examples (that sets System.currentTimeMillis() as a seed) call KeyPairGenerator.getInstance() ?

6. Given that we wish to focus on examples that generates a key using KeyPairGenerator using the same function as the given positive examples, provide feedback to the tool and allow it to reinfer a pattern.
If you are close to running out of time, skip (past the dataset comprehension questions) directly to the last part of the task (constructing a pattern). 



Check the suggest checkboxes of the features you believe should be included. Then, click on the “Reinfer Pattern” button. Once a pattern has been inferred, click on the “End Task” button.

