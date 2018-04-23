## Multiserver

##### What was the purpose of this project?

To apply what I have learned about parallelism and concurrency to developing a real software application, by creating a static web server that can serve many requests at once. A second goal was to better learn the underpinnings of HTTP by writing code that can conform to it.

##### How was the server tested?

I tested my server under load in a variety of ways. I found that the web browsers I use for day-to-day browsing all open many connections to the server, even when they don't absolutely need multiple -- probably to improve performance when bandwidth is high. This led me to want more granular control over the test requests, so I wrote a script that directed a 3-machine botnet to flood my server with HEADs and GETs over Telnet. Without strict limits on its behavior, it seemed to successfully achieve denial of service against the Multiserver. I would like to investigate this finding more.

##### What state is shared between threads? What concurrency problems were encountered?

My threads do not share almost any state. The main thread may access a helper's closeSocket method, but the worst concurrency problem I managed to cause thereby was to force an Exception to be thrown if the helper was using a socket when this happened -- which was my goal in that situation anyway, as it successfully killed the thread.

***

#### PREREQUISITES

JDK 6+

#### INSTALLATION

`javac ServerThread.java MultithreadedWebServer.java`

#### USAGE

`java MultithreadedWebServer [maxConnections] [rootDir] [userDir]`

Serves to localhost port 8888.

##### Arguments

 - `maxConnections`: Maximum number of connections to allow at once
 - `rootDir`: Absolute path of the root folder from which to serve content e.g. `/var/www/html`
 - `userDir`: Absolute path of the directory in which user folders are hosted e.g. `/home`. A request URL beginning in `~` followed by a username, a forward-slash, and finally a file path, will instruct the server to locate that file in the specified user's `public_html` directory. So a request for `/~root/a.jpg` would return the contents of `[userDir]/root/public_html/a.jpg`.