<p align="center">
  <a href="https:/github.com/Awe03/ChatAppGr11">
    <h1 align="center">Chat App</h1>
  </a>
</p>

## Getting Started
Install Ngrok<br>
Why?<br>
The network firewall in Greenwood High (FortiGuard) blocks connections to other devices. This means that the websocket server running on one system **cannot** be accessed on the other device (client).<br>
To bypass these restrictions we use Ngrok. Ngrok creates a secure tunnel to the localhost server and provides a public URL, essentially bypassing the restriction.<br>

- Visit [Ngrok](https://ngrok.com/download) to download the .exe and authenticate yourself.
- Run the following command and paste the 'Forwarding' url in the terminal when prompted to. The URL will look something like `http://a1b2-c3d4-e5f6.ngrok-free.app`
```bash
ngrok http --scheme=http 8080
```
`NOTE: NGROK MUST BE RUN ON THE DEVICE RUNNING THE WEBSOCKET SERVER. REFER BELOW FOR MORE INFORMATION`

## Details on the program
Taking a look in the directory, you will find:
- `MainUser.java` - this file contains code for the client and also automatically starts the websocket server on the same device.
- `Client.java` - this file would be distributed to other clients that will be connecting to the websocket server from another device or so.
- `AdminConsole.java` -  shows an overview on the message logs, active users and an option to reset chat history. This will **ONLY** work on the device on which the websocket server is running, as it depends on the history file.
- `WebsocketServer.java` - this file contains code for the websocket server; receiving, authentication and broadcasting all messages
- `WriteToLocal.java` <br> - this file contains code for the handling of the history on the server side. It writes all messages to a file so that message history is preserved even if a client or even the server goes down.

Kindly ignore all other files and directories. They are not relevant to the project `(/.idea, .gitignore, ChatApp.iml)`.

There are multiple ways to run the program, depending on how you wish to distribute it. One option is to run `MainUser.java` and run `Client.java` on other devices. Otherwise, you can run `WebsocketServer.java` and distribute `Client.java` to all devices.
As mentioned earlier, the `AdminConsole.java` will **ONLY** work on the device on which the websocket server is running so do note that.


## Disclaimer

I am **NOT** responsible for any damages caused by the use of this software. This software is intended for educational purposes only. Use at your own risk. I am not promoting or encouraging any bypassing of network restrictions. This software is intended to be used in a controlled environment and should not be used for malicious purposes.

## Credits

Developed by Shan Sharma