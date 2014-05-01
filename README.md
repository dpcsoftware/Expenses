Expenses
==============================================================

Expenses is a simple Android application to manage personal
expenses.

Version: 1.2.1

Developed By Daniel Pereira Coelho <dpcsoftware@gmail.com>

Special thanks to Jake Wharton, developer of ActionBarSherlock
library.

License
=============================================================

Copyright 2013, 2014 Daniel Pereira Coelho
   
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
environment for Android using Eclipse. Instructions:
[http://developer.android.com/sdk/index.html]

*Download ActionBarSherlock v 4.4.0
[http://www.actionbarsherlock.com]

*Open Eclipse

*Import the Expenses project and the ActionBarSherlock project
into workspace:
	- Go to Menu -> File -> Import... -> Existing
	Android Code Into Workspace
	- Pick up the folder where you extracted the source code
	and click Finish

*Add Dependency
	- Right click on the Expenses project -> Properties -> Android
	-> Library Section -> Add...
	- Then Pick ActionBarSherlock
	
*Export APK Package
	- Right click on the Expenses project -> Export -> Export
	Android Application
	- Follow instructions to sign and export your application
	- More infomation about how to sign the application:
	[http://developer.android.com/tools/publishing/app-signing.html]
	
*Have fun with your builds and modifications!

*Collaborate with the project on:
[http://github.com/dpcsoftware/Expenses]

Changelog
==============================================================

1.2.1:
	- Minor bug fixes

1.2:
	- Expenses is now free software (as in freedom) licensed under the GPL version 3
  - Backups are saved only on the SD card. Tight integration with Google Drive is no longer available. Reason: incompatibility with the GPL, make possible to use other cloud storage services
  - Added an option to export data to a Open Document Spreadsheet
  - Added a widget for quick access to add an expense and to view the total of a group
        



