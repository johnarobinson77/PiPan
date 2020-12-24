# Overview

PiPan is an Android app used to interface to a camera pan controller written in Python running on a raspberry pi. The python script is a text-based program that directly positions the camera at certain angles.

As is, this app is not useful to anybody but me because there is only one such pan controller in the world. But this code may serve as an example of how to use an Android app as a GUI for a text-based application on a server using ssh to communicate with that server. In particular, Sshcom is a class for connecting to and running commands on a general enough server to use in similar applications.

# Sshcom Usage

The core of the communication with the server is in the Sshcom class. Sshcom is a wrapper around the Jsch library and provides an Android compatible higher-level interface. It is a higher-level interface in that it follows a command/response protocol, rather than just sending and receiving the text.

The main interface is one where the app assumes the server "terminal" is sitting at some sort of prompt wait for a text command. The app sends a text command through Sshcom to the server, and the server sends some text back to Sshcom. Sshcom will send the resulting text back to the app&#39;s UI thread until one of two conditions is met, either a &quot;waitFor&#39;&#39; string matches or a timeout completes. At that point, the Sshcom is ready to work on the next command.

## Requirements

Sshcom requires class SshcomForegroundService in the project. Other than when starting the foreground service itself, all the rest of the interface is through Sshcom.

The AndroidManifest file for your app will require these permissions:
```xml
<uses-permission android:name="android.permission.INTERNET">
<uses-permission android:name="android.permission.FOREGROUND\_SERVICE">
```
And the declaration of this service
```xml
<service
  android:name=".Sshcom.SshcomForegroundService";
  android:enabled="true"
  android:exported="false"
```
## Main Interfaces to Sshcom

These are the main interfaces to the Sshcom class.

### Login Information

The following constructor:
```java
public Sshcom(Activity activity, Handler sshHandler)
```
restores the logon information that was previously stored if it exists. The constructor requires the Activity should be the main activity of the app. The Handler is discussed later in this document.

Sshcom internally stores the following login info for several hosts:
```java
private String[] baseIP;
private String[] username;
private String[] password;
private String[] directory;
private String[] cliPrompt;
```
There are the expected public setters and getters for each of those fields, and it requires the select to indicate which host login info to update. The following method will save all of the above strings in the saved preferences area of the app.
```java
public void saveSettings(int select)
```
They will be read back in by the constructor next time the app starts.

In addition, Sshcom stores the following login info:
```java
private String IP;
```
IP is the string that Sshcom actually uses for the host, and Sshcom sets it to the same value as baseIP whenever baseIP is set. However, sometimes it is necessary to patch the host IP address based on the current settings. If necessary it should be updated before a connect command is issued.

Before any ssh communications using Sshcom, a foreground service must be started using the following lines in an Activity class:
```java
Intent intent = new Intent(MainActivity.this, SshcomForegroundService.class);
startService(intent);
```
which starts a foreground service in which the SSH communications across the network are done. By putting it in a foreground service, long-running jobs on the server proceed even when the app or the device goes idle or asleep. As required by Android, this pops up a system notification. Edit SshcomForegroundService to modify that Notification.

### Sending Commands to Sshcom

The interface from the UI to the async thread handing SSH communications is
```java
public void sendRunCommandMsg(
int type, // type of command,
int tag, // command tag
String command, // text string sent to the server
String waitFor, // A text string that indicates the end of return
Integer timeOut, // Number of seconds to wait for text.
CommandResultCallback callback //Pointer to a class containing a
//method to handle return text
)
```
- type in the above parameter list can be any of the following:
```java
Sshcom.RunCommandType.OPEN_CHANNEL = 0; // Open the channel
Sshcom.RunCommandType.RUN_COMMAND = 1; // Send a command and wait for response
Sshcom.RunCommandType._CLOSE_CHANNEL = 2; // close the channel
```
- tag is an int that gets passed to the callback function handling the server&#39;s response.
- command is the String containing the command to be sent. Sshcom adds a \&lt;return\&gt; at the end of the string.
- waitFor is a string that is matched against the text returned from the server. It indicates to Sshcom that it should not expect any more text from the server after seeing that string. Its typical value would be the command line prompt for the server CLI. It can be null, indicating not to search for text, just wait for the timeout.
- timeout is an integer indicating to Sshcom how long it should wait to get all the text back from the server in seconds. It defaults to 5-second timeout is null, but waitFor is not
- callback is an instance of the CommandResultCallback Interface that receives the server&#39;s response. It is described in detail below.

Before your app can send any commands to the server, the channel to that server needs to be opened by calling sendRunCommand with the type set to OPEN\_CHANNEL. Opening a channel logs into the server using the IP, username, and password described in the login information section above. The rest of the parameters to the sendRunCommand can be null, but providing a callback will be useful in seeing the server response to the login before a command is sent.

After a channel is open, your app can send any number of commands using the RUN\_COMMAND type.

Finally, the channel can be closed using the CHANNEL\_CLOSE type. Even if you send an &quot;exit&quot; command, the channel should be closed to keep the Sshcom internal state consistent.

### CommandResultCallback

A callback must be defined to receive the text from the server after sending a command. This is done by overriding the onComplete method in the interface shown below:
```java
public interface CommandResultCallback {
public void onComplete(int tag, // user-specific
ReturnStatus returnStatus, // return status
String result // result
);
}
```
- tag is an int that is passed from the command issued and is available to the app for any use, such as disambiguating which sendRunCommand caused this particular return.
- returnStatus is one of the following:
```java
Sshcom.ReturnStatus.NULL, // no info
Sshcom.ReturnStatus.ERROR, // Error
Sshcom.ReturnStatus.CHANNEL_OPENED, // Channel was opened
Sshcom.ReturnStatus.CHANNEL_CLOSED, // Channel was closed
Sshcom.ReturnStatus.STILL_RUNNING, // Partial return expecting more
Sshcom.ReturnStatus.WAITFOR_FOUND, // Final return waitFor text was found
Sshcom.ReturnStatus.TIMEOUT // Final return command timed out
- result is a String containing the actual text.
```
The meanings of ReturnStatus values are as follows:

- NULL is useful to allow the app to call the callback directly rather than from Sshcom. It will never be sent to NULL when called as the result of sendRunCommandMsg().
- ERROR indicates that some failure in the channel to the server. The result string will contain an error message.
- CHANNEL_OPENED and CHANNEL_CLOSED are responses to the sendRunCommandMsg with type set to OPEN_CHANNEL and CLOSE_CHANNEL, respectively.
- STILL_RUNNING indicates that the server has sent the text in result, but the waitFor or timeout conditions have not been met, so more text from the last command is expected.
- WAITFOR_FOUND indicates that the text in the result contains the waitFor string, and Sshcom is ready to process the next call to sendRunCommandMsg.
- TIMEOUT indicated that the number of seconds in the timeout parameter of the last sendRunCommandMsg call has passed, and it will start working on the next call to sendRunCommandMsg.

The callback runs in the UI thread, and so updating any UI widgets can be done in the callback.

Sshcom sneds a callback with all the text received in a 1 second period. If that text does not contain the waitFor string or has not timed out, the ReturnStatus will be STILL\_RUNNING. This allows the UI to receive partial responses when the last command&#39;s output is long-running and sporadic.

As a result, the responses come back piecemeal. Sometimes it is necessary to parse the entirety of the text response since the last command. Sshcom provides a helper function that provides a String of an accumulation of all the text sent between that call to the senRunCOmmandMsg() and the receipt of the WAITFOR\_FOUND or TIMEOUT return. The function is
```java
public static String getCmdOutputBuffer()
```
### Unsolicited Commands

Sometimes it is necessary to send a command to interrupt a process started by sendRunCommandMsg(), but the waitFor and timeout conditions have not been met yet. To implement this, use the call:
```java
Sshcom._sendUnsolicitedCommand(String cmd)_
```
function. The text in cmd is sent to the server immediately, even if there is a pending callback. The typical use for this is to send a ^C or some other interrupt character when some command is taking too long. However, it does not terminate the process of Sshcom waiting for a waitFor match from the server. To break out of the loop that is waiting for the full response, execute a call to the command:
```java
public static void endWaitForResponse()
```
### Shutting Down Sshcom

When communications with the server are complete, and Sshcom is no longer required, issue the following two instructions in an Activity class:
```java
Intent intent = new Intent(this, SshcomForegroundService.class);
stopService(intent);
```
The above shuts down the asynchronous task and the foreground service. It also removes the system notification.

## Sshcom test()

At the end of the Sshcom class definition is a test() method that initiates a series of commands in the testCallback.onComplete method. It provides an example of how this programmer decided to create a callback. But there are other ways to write one.

Note that at the top of the function is a call to:
```java
NotificationsViewModel._append_(result)
```
The above sends the text to a scrolling TextVIew in the app. The programmer should replace it with whatever function you need to send the text to a UI widget.

The test() method calls the callback function directly with a NULL type and a tag of 0. A switch state on the tag causes a command to open the channel to be sent and a waitFor of the cliPropmt stored in the preferences. The tag for that command is set to 1.

When the callback is called as a result of that last command, the tag of 1 causes the callback to send a cd (change directory) command to the directory saved in the preferences. Again the waitFor is set to the stored cliPrompt, but the tag is set to 2.

When the cd command&#39;s callback is called, the tag of 2 causes it to send an &quot;ls -l&quot; command with a tag of 3.

The callback called with a tag of 3 causes the &quot;exit&quot; command to be sent and the channel closed.

Note that in each of these cases, the return text is sent to some UI widget, in this case, a scrolling text widget, in this case, NotificationsViewModel.append(result)&quot; If the return is of type STILL\_RUNNING, the returned text is sent, and the return is immediate. Only when the type of WAIT\_FOR or TIMEOUT does the callback proceed to the next command based on the tag.

# Background for This App

This app started from a project I am doing to create a timelapse video of panorama images. I attached a camera mount to a stepper motor that rotates the camera in the horizontal direction or a pan. The stepper motor is controlled by a raspberry pi running the Raspbien operating system in headless mode.

A python script running on the raspberry pi sends the stepper motor through a sequence of 3 or 4 pan positions telling the camera to snap a picture at each pan position. That sequence is repeated for as long as I want the timelapse to run. Each set of images from the 3 or 4 pan positions is combined into a panorama image in a later process. Then the collection of panorama images are used to create a timelapse video.

The above pan device is mounted on a tripod and run on a large battery, typically well away from any wifi access point. There is no input or output device attached to the raspberry pi. Instead, it is connected to my phone&#39;s hotspot. Until this app, I would ssh into the raspberry pi using a terminal emulator app called termux. (An amazing app for Linux geeks, BTW.)

But typing Linux commands using the small screen on the phone, especially if it&#39;s cold and dark outside, is challenging. With this app, I have buttons that send the command I would have typed into turmux and have the app interpret the response. It is essentially a remote GUI for the pan controller.

A link to the website describing the pan controller will be added in the future.
