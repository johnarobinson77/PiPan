# PiPan
Android UI for a camera pan controller running on a raspberry pi
# Overview

While as an application, this Android app is not useful to anybody but me. But it may serve as an example of how to use an Android app as a GUI for a text-based application on a server using ssh to communicate with the server. This app is used to control a python script running on a raspberry pi that runs a camera pan controller. However, Sshcom is a class for connecting to and running commands on a server that is general enough to be used in a number of applications.

# Sshcom Usage

The core of the communication with the server is in the Sshcom class. Sshcom is a wrapper around the Jsch library and provides an Android compatible higher-level interface. It is a higher-level interface, in that it follows a command/response protocol, rather than just sending and receiving the text.

The main interface is one where the app assumes the server &quot;terminal&quot; is sitting at some sort of prompt wait for a text command. The app sends a text command through Sshcom to the server and the server sends some text back to Sshcom. Sshcom will send the resulting text back to the app&#39;s UI thread until one of two conditions is met, either a &quot;waitFor&quot; string is matched, or a timeout is met. At that point, the SshCom is ready to work on the next command.

## Main Interfaces to Sshcom

These are the main interfaces to the Sshcom class.

### Login Information

The following constructor:

public Sshcom(Activity activity, Handler sshHandler)

constructor restores the logon information that was stored previously if it exists. The constructor requires the Activity should be the main activity of the app. The Handler will be discussed later.

Sshcom internally stores the following login info for several hosts:

private String[] baseIP;

private String[] username;

private String[] password;

private String[] directory;

private String[] cliPrompt;

There are the expected public setters and getters for each of those fields and it requires the select to indicate which host login info to update. The following method will save all of the above strings in the saved preferences area of the app.

public void saveSettings(int select)

In addition, there is the following login info:

private String IP;

IP is the string that Sshcom actually uses for the host but and is generally set to the same value as baseIP. However, sometimes it is necessary to patch the host IP address based on the current settings.

Before any ssh communications can be sent, an asynchronous thread must be started using the method:

public Handler startRunCommandThread()

Though it returns a Handler, it is not necessary to store it.

### Sending Commands to Sshcom

The interface from the UI to the async thread handing SSH communications is

public void sendRunCommandMsg(

int type, // type of command,

int tag, // command tag

String command, // text string sent to the server

String waitFor, // A text string that indicates the end of return

Integer timeOut, // number of seconds to wait for text.

CommandResultCallback callback //pointer to a class containing a

//method to handle return text

)

- type in the above function can be any of the following

Sshcom.RunCommandType._OPEN\_CHANNEL_ = 0; _// Open the channel_

Sshcom.RunCommandType._RUN\_COMMAND_ = 1; _// Send a command and wait for response_

pSshcom.RunCommandType._CLOSE\_CHANNEL_ = 2; _// close the channel_

- tag is an int that gets passed to the callback function handling the response from the server.
- command is the String containing the command to be sent. Sshcom adds a \&lt;return\&gt; at the end of the string.
- waitFor is a String that is matched against the text returned from the server. It is meant to indicate to Sshcom that it should not expect any more text from the server after seeing that String. Its typical value would be the command line prompt for the server CLI. It can be null indicating to not search for tet, just wait for the timeout.
- timeout is an Integer indicating to Sshcom how long it should wait to get all the text back from the server.
- callback in an instance of the CommandResultCallback Interface that receives the response from the server. It is described in detail below.

Before any text command can be sent to the server the channel to that server needs to be opened by calling sendRunCommand with type set to OPEN\_CHANNEL. Opening a channel logs into the server using the IP, username, and password described in the Login Information above. The rest of the parameters to the sendRunCommand can be null but providing a callback will be useful in seeing the severs response to the login before a command is sent.

After a channel is open, any number of commands can be sent using the RUN\_COMMAND type.

Finally, the channel can be closed using the CHANNEL\_CLOSE type. Even if you send an &quot;exit&quot; command, the channel should be closed to keep the internal state consistent.

### CommandResultCallback

To receive the text from the server after a command is sent a callback must be defined by overriding the onComplete method in the interface shown below

public interface CommandResultCallback {

public void onComplete(int tag, // user specific

ReturnStatus returnStatus, // return status

String result // result

);

}

- tag is an int that is passed from the command issued available to the user for any use, such as disambiguating which sendRunCommand caused this particular return.
- returnStatus is one of the following:

Sshcom.ReturnStatus_.NULL_, _// no info_

Sshcom.ReturnStatus_.ERROR_, _// Error_

Sshcom.ReturnStatus_.CHANNEL\_OPENED_, _// Channel was opened_

Sshcom.ReturnStatus_.CHANNEL\_CLOSED_, _// Channel was closed_

Sshcom.ReturnStatus_.STILL\_RUNNING_, _// Partial return expecting more_

Sshcom.ReturnStatus_.WAITFOR\_FOUND_, _// Final return waitFor text was found_

Sshcom.ReturnStatus_.TIMEOUT_ _// Final return command timed out_

- result is a String containing the actual text.

The meanings of ReturnStatus values are as follows:

- NULL is useful if it is desired to call the callback directly rather than from Sshcom. It will never be sent to NULL when called from Sshcom.
- ERROR indicates that some failure in the channel to the server. The result string will contain an error message.
- CHANNEL\_OPENED and CHANNEL\_CLOSED are responses to the sendRunCommandMsg with type set to OPEN\_CHANNEL and CLOSE\_CHANNEL respectively.
- STILL\_RUNNING indicates that the server has sent the text in result but the waitFor or timeout conditions have not been met so more text from the last command is expected.
- WAITFOR\_FOUND indicates that the text in the result contains the waitFor string and Sshcom is ready to process the next call to sendRunCommandMsg.
- TIMEOUT indicated that the number of seconds in the timeout parameter of the last sendRunCommandMsg call has passed and it will start working on the next call to sendRunCommandMsg.

The callback runs in the UI thread and so updating any UI widgets cane done in the callback

### Unsolicited Commands

Somtimes it is necessary to send a command to interrupt a process that was started by sendRunCommandMsg() but the waitFor and timeout conditions have not been met yet. This can be implemented with the

Sshcom._sendUnsolicitedCommand(String cmd)_

function. The text in cmd is sent to the server immediately even if there is a pending callback. The typical use for this is to send a ^C or some other interrupt character when some command is taking too long. It does not however terminate the process of Sshcom waiting for text from the server so expect more callbacks.

## Sshcom test()

At the end of the Sshcom class definition is a test() method that initiates a series of commands in the testCallback.onComplete method that provides an example of how this user decided to create a callback but there are other ways to write one.

The test() method calls the callback function directly with a NULL type and a tag of 0. A switch state on the tag causes a command to open the channel to be sent and a waitFor of the cliPropmt stored in the preferences. The tag for that command is set to 1.

When the callback is called as a result of that last command, the tag of 1 causes the callback to send a cd (change directory) command to the directory saved in the preferences. Again the waitFor is test to shd cliPrompt but the tag is set to 2.

When the callback for the cd command is called, the tag of 2 causes it to send a; &quot;ls -l&quot; command with a tag of 3.

The callback call with a tag of 3 causes the &quot;exit&quot; command to be sent and the channel to be closed.

Note that in each of these cases, the return text is sent to some UI widget, in this case, a scrolling text widget. If the return is of type STILL\_RUNNING, the returned text is sent and the return is immediate. Only when the type of WAIT\_FOR or TIMEOUT does the callback proceed to the next command based on the tag.

# Background for This App

The reason for this app started from a project I am doing of creating a timelapse video of panorama images. I attached a camera mount to a stepper motor that rotates the camera horizontal direction or a pan. The stepper motor is controlled by a raspberry pi running the Raspbien operating system in headless mode.

A python script running on the raspberry pi sends the stepper motor through a sequence of 3 or 4 pan positions telling the camera to snap a picture at each pan position. That sequence is repeated for as long as I want the timelapse to run. In a later process, one set of images from the 3 or 4 pan positions is combined into a panorama image and then the set of panorama images are used to create a timelapse video.

The above is pan device is mounted on a tripod and run on a large battery, typically well away from any wifi access point. There is no input or output device attached to the raspberry pi. Instead, I have it connect to my phone&#39;s hotspot. Up until this app I would ssh into the raspberry pi using a terminal emulator app called termux. (An amazing app for linux geeks BTW.)

But typing linux commands into the small keyboard on the phone, especially if it&#39;s cold and dark outside is not very fun. With this app, I have buttons that send the command I would normally type into turmux and have the app interpret the response. It is essentially a GUI for the pan controller.

A link to the website describing the pan controller will be sent at some point in the future.
