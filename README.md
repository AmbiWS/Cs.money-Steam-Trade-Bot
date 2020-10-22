# CS.MONEY Steam Trade Bot
### About

Developed in 2018 and Updated in 2019.  
This is a **debug** build, only for revision.  
Not for use, requires some rework (due to backend change of cs.money website).

Tools: Java, Swing, Selenium Webdriver, NV Websocket Client, Commons IO.  

This is one of a several projects for trade automation in Steam.  
Current bot was developed to automatic purchase items from user list in cs.money website.  
Java code can be found [here!](https://github.com/AmbiWS/Cs.money-Steam-Trade-Bot/tree/main/src/main/java)  

---  

### Bot's Algorithm
1. Connection to target web-server.
2. Login to target web-server.
3. Subscription check.
4. Parsing user list of items from target web-server.
5. Connection to cs.money website via Selenium Webdriver (to generate and parse tokens, cookies, etc...).
6. Creation of a new cs.money web session programmatically.
7. Connection to cs.money WebSocket Server.
8. Parsing WebSocket events in background.
9. Comparing new items from WS Events with user list.
10. Buying item if a match via post-request.

--- 

### GUI Screenshot  

![Screenshot 1](https://github.com/AmbiWS/Cs.money-Steam-Trade-Bot/blob/main/src/scr/Screen.jpg)  
