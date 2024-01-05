In this task, you will inspect examples using the Cipher API. Some examples have been marked as positive or negative. In particular, we are interested in the use of Cipher when instantiated with "AES/GCM/NoPadding”. Your goal is to answer the following questions to understand the distribution of API use and detect more examples similar to the given positive examples.


You have 12 mins in total. If you are close to running out of time (i.e., 2 mins left), skip (past the dataset comprehension questions) directly to the last part of the task (constructing a pattern). 


From code/meteor_app, run `sh run_cipher.sh`
Go to localhost:3000.

Click on "predefined examples". 
Scroll down and click on the Infer Pattern button at the bottom of the page.


Now, on the left pane, we see the inferred pattern
“Cipher.getInstance(..."AES/GCM/NoPadding"...)” . Examples matching this pattern
instantiates Cipher with the string, "AES/GCM/NoPadding" , as an argument.
This pattern is a starting point for analyzing these examples. The right panel now shows
possible features for inclusion in the pattern. SURF offers tools for analyzing the
examples that match the pattern.


1. What is one exception constructed in a catch block (after catching another exception)? 
2. Which negative instances invoke Cipher.init() but not Cipher.updateAAD()?
3.  How many positive instances constructed a new SecretKeySpec()
4. What is one class of exception caught by the positive instances
5. Which unlabelled examples match NoSuchPaddingException? 

6. Given that we wish to focus on examples that catch the same exceptions as the given positive examples but the negative examples, provide feedback to the tool and allow it to reinfer a pattern. 

If you are close to running out of time, skip (past the dataset comprehension questions) directly to the last part of the task (constructing a pattern). 
Check the suggest checkboxes of the feature you believe should be included. 

Then, click on the “Reinfer Pattern” button. 

Once a pattern has been inferred, click on the “End Task” button.