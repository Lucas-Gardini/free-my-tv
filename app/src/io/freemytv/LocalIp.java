package io.freemytv;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.net.Inet4Address;
import java.net.InetAddress;

/** The usable LAN IPv4 shown for connecting to the TV. */
final class LocalIp {
    private LocalIp() {}

    static String ipv4(InetAddress address) {
        return address instanceof Inet4Address && !address.isAnyLocalAddress() && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress() && !address.isMulticastAddress() ? address.getHostAddress() : "";
    }

    /** Called off the UI thread. A LAN without internet still has a useful address for ADB. */
    static String read(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) return "";
            String current = address(manager, manager.getActiveNetwork(), -1);
            if (!current.isEmpty()) return current;
            Network[] networks = manager.getAllNetworks();
            for (int transport : new int[] {NetworkCapabilities.TRANSPORT_ETHERNET, NetworkCapabilities.TRANSPORT_WIFI}) {
                for (Network network : networks) {
                    current = address(manager, network, transport);
                    if (!current.isEmpty()) return current;
                }
            }
        } catch (RuntimeException e) {
            // Some firmware restricts network state. The optional label must never break Home.
        }
        return "";
    }

    private static String address(ConnectivityManager manager, Network network, int transport) {
        if (network == null) return "";
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        if (capabilities == null || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return "";
        if (transport >= 0 ? !capabilities.hasTransport(transport)
                : !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    && !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "";
        LinkProperties properties = manager.getLinkProperties(network);
        if (properties != null) for (LinkAddress link : properties.getLinkAddresses()) {
            String ip = ipv4(link.getAddress());
            if (!ip.isEmpty()) return ip;
        }
        return "";
    }
}
