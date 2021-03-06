/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sahlbach.gradle.plugins.jettyEclipse

import org.eclipse.jetty.server.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Monitor <p/> Listens for stop commands eg via gradle jettyEclipseStop and causes jetty to stop either by exiting
 * the jvm or by stopping the Server instances. The choice of behaviour is controlled by either passing true (exit jvm)
 * or false (stop Servers) in the constructor.
 */
class Monitor extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    private String key
    private ServerSocket serverSocket
    private final Server server

    public Monitor(int port, String key, Server server) throws IOException {
        this.server = server
        if (port <= 0) {
            throw new IllegalStateException("Bad stop port")
        }
        if (key == null) {
            throw new IllegalStateException("Bad stop key")
        }

        this.key = key
        daemon = true
        name = "JettyEclipseStopMonitor"
        serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))
        serverSocket.reuseAddress = true
    }

    public void run() {
        while (serverSocket != null) {
            Socket socket = null
            try {
                socket = serverSocket.accept()
                socket.setSoLinger(false, 0)
                LineNumberReader lin = new LineNumberReader(new InputStreamReader(socket.inputStream))

                String key = lin.readLine()
                if (!this.key.equals(key)) {
                    continue
                }
                try {
                    socket.close()
                } catch (Exception e) {
                    logger.debug("Exception when stopping server", e)
                }
                try {
                    socket.close()
                } catch (Exception e) {
                    logger.debug("Exception when stopping server", e)
                }
                try {
                    serverSocket.close()
                } catch (Exception e) {
                    logger.debug("Exception when stopping server", e)
                }
                serverSocket = null
                try {
                    logger.info("Stopping server due to received '{}' command...", key)
                    server.stop()
                } catch (Exception e) {
                    logger.error("Exception when stopping server", e)
                }
                //We've stopped the server. No point hanging around any more...
                return
            } catch (Exception e) {
                logger.error("Exception during monitoring Server", e)
            } finally {
                if (socket != null) {
                    try {
                        socket.close()
                    } catch (Exception e) {
                        logger.debug("Exception when stopping server", e)
                    }
                }
            }
        }
    }
}
