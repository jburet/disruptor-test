package jbu.zab;

import jbu.zab.msg.ApplicationData;


public class TestApplicationData extends ApplicationData {

    private int data;

    public TestApplicationData(int data) {
        this.data = data;
    }

    public int getData() {
        return data;
    }
}
