package cz.vondr.yaps.unit_tests.tool.http

class SocketUtil {
    static int findFreePort() {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        port
    }
}
