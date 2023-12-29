#  Android Sync Client
Client receives files from the Windows Server.

## Background
I have always been fascinated by how wireless communication works, specifically how one could send information from one device to another without clicking a buttuon (file synchronization). As a student I used multiple devices inorder for both school and personal work. I would have an ipad where I had most of lecture notes, a phone where I'd reply to important messages because I despise of texting on an iPad it feels unpleasantly awkward. Also I had a laptop where I would attend online classs, conferences etc. Problem is whenever I'd download some school study material It was necessary for me to have them on all my devices for convinience sake. It was impractical for me to manually download these files to all my devices or copy from device to device, I am a computer engineer I don't do "tedius". Ergo I decide to learn more about TCP/IP through building an application that utilizes sockets inorder to sync files across all my devices. 

## Features
Can transfer all sorts of files audio, documents, video, images etc.
Has a graphical user interface.
Can transfer files seamlessly at the click of a button.
Fast and reliable file transfer.
Saves date of last sync, so user knows when last sync occured.

## Note
System is limited to a small sized buffer, meaning it can transfer small files mainly because my school documents were small pdf file ergo that was no need for me to use a bigger a transfer buffer. Future work will implement an option for the user to adjust the transfer buffer size based on needs.
