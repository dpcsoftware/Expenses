Expenses
==============================================================

Expenses is a simple Android application to manage personal
expenses.

Version: 1.3.3

Developed By Daniel Pereira Coelho <dpcsoftware@gmail.com>

License
=============================================================

Copyright 2013-2015 Daniel Pereira Coelho
   
Expenses is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation in version 3.

Expenses is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

Check the COPYING file to read the full license.

Buil Instructions
==============================================================

*Download the Android SDK and set up a full development
environment for Android using Android Studio. Instructions:
[http://developer.android.com/sdk/index.html]

*Clone the repository

*Open Android Studio
	- Select Open an existing Android Studio project
	- Select the correct folder

*Export APK Package
	- Click on Build -> Generate Signed APK...
	- Follow instructions to sign and export your application
	- More infomation about how to sign the application:
	[http://developer.android.com/tools/publishing/app-signing.html]

*Have fun with your builds and modifications!

*Collaborate with the project on:
[http://github.com/dpcsoftware/Expenses]

Changelog
==============================================================
1.3.3:
	- Fixed a bug when sharing or opening a file in the Export Data menu

1.3.2:
	- Fixed a bug concerning the storage access permission

1.3.1:
	- Categories can now have custom colors
	- Statistics on time have new options
	- Added statistics by group screen
	- Minor design changes

1.3:
	- Redesigned interface matching new Android standards
	- You can now search over your expenses records
	- You can define a Budget, track it over time and receive notifications
	- ActionBarSherlock was droped in favor of android-support-v7 library app-compat
	- Project is now build using Android Studio IDE rather than Eclipse ADT

1.2.1:
	- Minor bug fixes

1.2:
	- Expenses is now free software (as in freedom) licensed under the GPL version 3
	- Backups are saved only on the SD card. Tight integration with Google Drive is no longer available. Reason: incompatibility with the GPL, make possible to use other cloud storage services
	- Added an option to export data to a Open Document Spreadsheet
	- Added a widget for quick access to add an expense and to view the total of a group
