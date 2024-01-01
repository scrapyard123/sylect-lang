// SPDX-License-Identifier: MIT

package forward.java;

// TODO: Remove bridge once all features are in place
public class ResourceData {
    private final byte[] data;

    public ResourceData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public int isAbsent() {
        return data == null ? 1 : 0;
    }

    public int getLength() {
        return data.length;
    }
}
