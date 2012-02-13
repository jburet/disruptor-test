package jbu.zab;

import jbu.zab.msg.ApplicationData;

public class ByteArrayApplicationData extends ApplicationData {

    private byte[] data;

    public ByteArrayApplicationData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
