package fi.aalto.roopepalomaki.distanceawarebarcode;

public class DecodeResult {
    public final byte[] bytes;
    public final boolean didPassChecksum;
    public final int numberOfErrorsCorrected;

    public DecodeResult(byte[] bytes, boolean didPassChecksum, int numberOfErrorsCorrected) {
        this.bytes = bytes;
        this.didPassChecksum = didPassChecksum;
        this.numberOfErrorsCorrected = numberOfErrorsCorrected;
    }
}

