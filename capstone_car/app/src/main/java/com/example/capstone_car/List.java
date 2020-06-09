package com.example.capstone_car;

public class List {
    private int dlvy_num, dlvy_wait_num, dlvy_time, viewType, car_num, category;
    private String user_name, start_point, end_point, status, end_date;

    public List() {}

    // On call, on delivery
    public List(int dlvy_num, int car_num, String user_name, String start_point, String end_point, int dlvy_time, String status, int viewType) {
        this.dlvy_num = dlvy_num;
        this.car_num = car_num;
        this.user_name = user_name;
        this.start_point = start_point;
        this.end_point = end_point;
        this.dlvy_time = dlvy_time;
        this.status = status;
        this.viewType = viewType;
    }

    // Completed delivery
    public List(String user_name, String start_point, String end_point, String status, String end_date, int category) {
        this.user_name = user_name;
        this.start_point = start_point;
        this.end_point = end_point;
        this.status = status;
        this.end_date = end_date;
        this.category = category;       // Spinner
    }

    // Waiting
    public List(int dlvy_num, int dlvy_wait_num, String user_name, String start_point, String end_point, String status, int viewType) {
        this.dlvy_num = dlvy_num;
        this.dlvy_wait_num = dlvy_wait_num;
        this.user_name = user_name;
        this.start_point = start_point;
        this.end_point = end_point;
        this.status = status;
        this.viewType = viewType;
    }

    public int getDlvyNum() { return dlvy_num; }

    public void setDlvyNum(int dlvy_num) { this.dlvy_num = dlvy_num; }

    public int getCarNum() { return car_num; }

    public void setCarNum(int car_num) { this.car_num = car_num; }

    public int getDlvyWaitNum() { return dlvy_wait_num; }

    public void setDlvyWaitNum(int dlvy_wait_num) { this.dlvy_wait_num = dlvy_wait_num; }

    public int getDlvyTime() { return dlvy_time; }

    public void setDlvyTime(int dlvy_time) { this.dlvy_time = dlvy_time; }

    public String getUserName() {
        return user_name;
    }

    public void setUserName(String user_name) { this.user_name = user_name; }

    public String getStartPoint() {
        return start_point;
    }

    public void setStartPoint(String start_point) { this.start_point = start_point; }

    public String getEndPoint() {
        return end_point;
    }

    public void setEndPoint(String end_point) { this.end_point = end_point; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEndDate() {
        return end_date;
    }

    public void setEndDate(String end_date) {
        this.end_date = end_date;
    }

    public int getViewType() { return viewType; }

    public void setViewType(int viewType) { this.viewType = viewType; }

    public int getCategory() { return category; }                           // Spinner

    public void setCategory(int category) { this.category = category; }
}
