package com.example.capstone_car;

/*
Add list by delivery status from the received delivery list
Why is it divided by delivery status? >> Because the button event needs to be applied differently for each state
 */
public class ReceiveCode {
    public class ViewType {
        public static final int WAIT = 0;
        public static final int DLVYWAIT = 1;
        public static final int DLVY = 2;
    }
}
