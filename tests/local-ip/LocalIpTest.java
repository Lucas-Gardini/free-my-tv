package io.freemytv;

import java.net.InetAddress;

/** Numeric literals only: the tests never query DNS or depend on the host network. */
public final class LocalIpTest {
    public static void main(String[] args) throws Exception {
        expect("192.168.127.16", "192.168.127.16");
        expect("10.0.0.5", "10.0.0.5");
        expect("172.16.1.8", "172.16.1.8");
        expect("127.0.0.1", "");
        expect("0.0.0.0", "");
        expect("169.254.10.20", "");
        expect("224.0.0.1", "");
        expect("::1", "");
        expect("fe80::1234", "");
        expect("2001:db8::1", "");
        if (!LocalIp.ipv4(null).isEmpty()) throw new AssertionError("Missing address must be unavailable");
        System.out.println("LocalIp: 11 checks passed");
    }

    private static void expect(String input, String expected) throws Exception {
        String actual = LocalIp.ipv4(InetAddress.getByName(input));
        if (!expected.equals(actual)) throw new AssertionError(input + ": expected '" + expected + "', got '" + actual + "'");
    }
}
