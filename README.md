TMSCA Contest Management
=================

Web application to administer [TMSCA](http://www.tmsca.org/) tournaments at the Elementary, Middle and High School levels with support for automatic tabulation, user accounts, online registration, contest result visualization, a gallery and much more using the Google App Engine PaaS. Designed from the bottom-up to support tournaments held at any location, an administrator can customize everything from the levels offered to the HTML emails sent to registered coaches.

Pages
-----

### Public Pages ###
* **Main page**: `/` Read a short description of the tournament and view a carousel of pictures with captions and titles (everything customizable HTML through the Admin Panel).
* **About page**: `/about` Read a short description of the tournament (customizable HTML through the Admin Panel), a tournament schedule (customizable through the Admin Panel), state qualifying scores (customizable through the Admin Panel), descriptions and example tests for each of the four contests, and an "About the Website" modal that provides attribution to the external frameworks utilized
* **Directions page**: `/directions` View an accordion with a set of directions to the host campus and a Google Maps view of the campus with an info window that provides the address and a directions link (everything customizable through the Admin Panel)
* **Contest Results pages**: `/contestResults` View the current grading progress (table and progress bar); results are hidden by default and viewing is enabled through the admin panel. Functionality includes sorting of the tables based on any criteria. A hamburger menu is provided for mobile viewports.
	* **Sweepstakes pages**: `/contestResults?type=*_sweep` View the school sweepstakes and the students who made up each team.
	* **Category Sweepstakes pages**: `/contestResults?type=*_categorySweep` View the school sweekstakes for each category (Science, Calculator, Number Sense, Math) along with the team that composed each score
	* **Visualizations pages**: `/contestResults?type=*_visualizations` View box-and-whisker plots and histograms for the tests in each subject, split by grade, with normal distribution overlays.
	* **Category Winners pages**: `/contestResults?type=*_category_*` View the student winners of a category with their score and school (only first names can be displayed through a setting in the Admin Panel).
* **Contact Us page**: `/contactUs` Contact the tournament administrator for any questions, clarifications, or registration changes (protected by No CAPTCHA reCAPTCHA).
* **Registration page**: `/registration` Register for the tournament and create an account at the same time. Either import your registrations via copying from a spreadsheet program or use the web interface to add students (protected by No CAPTCHA reCAPTCHA). This page is disabled when the administrator disables registration through the admin panel and will ask for school classification depending on an admin setting.
* **Login page**: `/login` Login to your user account or as the administrator and choose whether to remain logged in (> 30 minutes). Passwords are stored using PDKDF2. When testing mode is enabled through the admin panel, a popout describes how to log in as an admin.
	* **Logout**: `/logout` Logout of your account.
	* **Password Reset Page**: `/forgotPass` Request a password reset to be sent to your email account and then reset your password.

### Administrator Pages ###
In addition to access to all public pages, an administrator has access to a few others. Furthermore, the admin can always view the contest results page even if results are not yet public and always submit registrations even if the window has closed.

* **Main Page**: `/` Allows logging in as a coach by using their email
* **Control Panel**: `/adminPanel` Allows setting of numerous environmental variables that control the behavior of the website
	* **General Tab**
		* **Contest Title**: Set the contest title used throughout the website and in the navigation bar
		* **School Levels**: Set the school levels the tournament will host (a nonempty subset of `{elementary, middle, high}`)
		* **Registration Dates**: Set the dates within which online registration is open
		* **Registration Editing Dates**: Set the dates within which logged in coaches can edit their student data
		* **Test Charges**: Set the dollar cost for each test used on the registration page
		* **Update online scores**: Add a tabulation request to the task queue, see the time that scores were last auto-generated successfully, and create empty spreadsheets populated with student data from registration info
		* **Scrub Datastore**: Allows deleting all the tabulation data (scores, rankings, visualizations - `CategoryWinners`, `CategorySweepstakesWinners`, `SweepstakesWinners`, `Visualization`, `School` entities)
		* **Make results public**: Make results public on the Contest Results page
		* **Change Admin Password**: Allows changing of the administrator password
		* **Enable Testing Mode**: Enables testing mode which resets the administrator credentials to `admin` and `password` and displays a popout on the Login page describing this mode
		* **Ask for School Classification**: Allows for making the school classification question at registration optional, required or not present
		* **Hide Full Names**: This option hides full names on public results
	* **Content Tab**
		* **School Name**: Set the name of the host school used throughout the website and in emails
		* **Address**: Set the address of the host school used on the Directions page
		* **Absolute Location**: Set the absolute location (longitude and latitude) of the host school used on the Directions page
		* **About Text**: Set the tournament description text used on the About page and the main page with a WYSIWYG HTML editor
		* **Schedule**: Set the tournament schedule in the form of a YAML Associative Array displayed on the About page
		* **Directions**: Set human-readable directions to the host school in the form of a YAML Associative Array displayed on the Directions page
		* **Slideshow**: Set the image, title, and captions of the slideshow in the form of a YAML List of Lists displayed on the Main page
	* **Awards Tab**
		* **Award Critiera**: Set the placings which entitle students of each level (elementary, middle, high) to each type of award (medal, trophy), used on the category winners pages, category sweepstakes winners pages, and the sweepstakes winners pages
		* **Qualifying Criteria**: Set the middle school state qualifying scores for each test, used on all student pages, school score pages, and the about page
	* **Emails Tab**
		* **E-Mail Address**: Specify the email address to which questions submitted through the Contact Us page are sent to
		* **Google E-Mail**: Specify the email address from which emails to users are sent to (the Google App Engine user that hosts the project)
		* **Registration Email**: Set the email sent to users who register with a WYSIWYG HTML editor (default available) and the `$account (bool)`, `$name`, `$title`, `$cost`, `$url` variables available
		* **Registration Email**: Set the email sent to users who register with a WYSIWYG HTML editor (default available) and the `$account (bool)`, `$name`, `$title`, `$cost`, `$url` variables available
		* **Forgot Password Email**: Set the email sent to users who forget their password with a WYSIWYG HTML editor (default available) and the `$title`, `$user`, `$url` variables available
		* **Question Email**: Set the email sent to the admin when a question is submitted through the Contact Us page with a WYSIWYG HTML editor (default available) and the `$name`, `$email`, `$message` variables available
	* **APIs Tab**
		* **ReCAPTCHA API Keys**: Set the private and public reCAPTCHA keys used on the Contact Us and Registration pages
		* **Google OAuth 2.0 Credentials**: Set the Client ID and Client Secret used for the tabulation process and creating tabulation spreadsheets
		* **Google Site Verification**: Set the Google Site Verification code used as the content attribute in `<meta name="google-site-verification">` on the Main page
		* **Google Analytics Tracking JS**: Set the Google Analytics Tracking Code (only the JS part, without the HTML `script` tag) used on all pages
* **Contest Data pages**: `/data` Allow viewing and editing of contest data such as registrations and questions.
	* **Registrations page**: `data/registrations` Displays a table and donut graph of the "Registered Students and Fees by Level" and "Test Registrations by Subject and Level." Within a tab for each level, allows the viewing of all registrations and inline editing of some properties of them in the filterable table itself, also exposes access to a registration editing interface via the edit button.
		* **Edit registration page**: `/editRegistration?key=*` Allows for the editing of a registration (all of its fields save the password field), deleting the user's account and deleting their entire registration (currently an irreversible action)
	* **Questions page**: `/data/questions` Allows for the viewing of questions submitted through the Contact Us page and marking them as resolved (archiving) or deleting them entirely. Functionality includes sorting of the tables based on any criteria.
	* **Scores page**: `/data/scores`:
		* **All students pages**: `/data/scores?type=*_students` View the scores of all students with their grade and school name. Toggle marking of qualifying scores for the middle students page.
		* **Sweepstakes pages**: `/data/scores?type=*_sweep` View the school sweepstakes and the students who made up each team.
		* **Category Sweepstakes pages**: `/data/scores?type=*_categorySweep` View the school sweekstakes for each category (Science, Calculator, Number Sense, Math) along with the team that composed each score
		* **Visualizations pages**: `/data/scores?type=*_visualizations` View box-and-whisker plots for the tests in each subject, split by grade.
		* **School pages**: `/data/scores?type=*_school_*` View the school scores page for any school that displays their students' scores, school specific visualizations, and a breakdown of current sweepstakes scores. Toggle marking of qualifying scores for middle school pages.
		* **Category Winners pages**: `/data/scores?type=*_category_*` View the student winners of a category with their score and school.

### User Pages ###
In addition to access to all public pages, a logged-in user has access to a few others and modifications to some.
* **Main page**: `/` Rather than displaying the carousel and a contest description, this page provides all available information about the user's registration, access to the change password link and allows the user to change the students that compose their registration within the timespan set in the Admin Panel.
* **Change Account Password page**: `/changePass` Allows modification of the logged-in users password after confirming their current password.
* **Contact Us page**: `/contactUs` The logged-in user can submit questions that are associated with their account automatically.
* **View Scores page**: `/viewScores` View the logged-in school's student scores, school specific visualizations, and a breakdown of current sweepstakes scores. Toggle marking of qualifying scores on middle school pages.

### Extras ###
* Custom error pages with detailed diagnostic information are provided for HTTP errors 401, 403, 404, 405, and 500
* All pages are wired up to work with custom Google Analytics Tracking Code
* Pages often contain a print button with customized print views
* Access to the Google Drive API is conducted through OAuth 2.0 with Google+ Sign In

Tabulation
----------

### The admin will... ###
1. Enqueue a spreadsheet creation task to generate empty spreadsheets with student data pre-populated through the Admin Panel.
2. Each spreadsheet (one for each level) will contain worksheets corresponding to each school enrolled where the worksheets are simple tables consisting of student name, grade, and score for each subject.
3. Populate scores throughout the spreadsheet, marking ties with either integers following the decimal (e.g. 123.4 to denote the 4th 123) or letters (e.g. 123 to denote the 4th 123). For example:

	|Name |Grade  |NS  |CA  |MA  |SC  |
	|:----:|:-----:|:--:|:--:|:--:|:--:|
	|John Doe |6|356|312.1|    |   |
	|Sarah Doe|7|   |264  |265B|102|
	|Bobby Doe|8|132|     |265A|140|

4. Enqueue a tabulation task through the Admin Panel.

### The tabulation task will... ###
1. Retrieve contest information from the GAE Datastore

	```java
	Retrieve.contestInfo()
	```
2. Get award criteria from the GAE Datastore

	```java
	Retrieve.awardCriteria(contestInfo)
	```
3. Authenticate to the Google Documents Service using an OAuth 2.0 Authentication Token from the Datastore

	```java
	authService(SpreadsheetService service, Entity contestInfo)
	```
4. Populate base data structures by traversing Google Documents Spreadsheets

	```java
	getSpreadSheet(String docString, Service service)
	updateDatabase(Level level, SpreadsheetEntry spreadsheet, Set<Student> students, Map<String, School> schools, Set<Test> testsGraded, Service service)
	```
	* `Set<Student> students` (`HashSet`) →
		* `String name`
		* `int grade`
		* `School school`
		* `Map<Subject, Score> scores` (`HashMap`)
	* `Map<String, School> schools` (`HashMap`) →
		* `String name`
		* `Level level`
		* `int totalScore`
		* `Set<Student>` (`HashSet`)
		* `Map<Test, Integer> numTests` (`HashMap`)
		* `Map<Test, List<Score>> anonScores` (`HashMap`)
		* `Map<Subject, Pair<Student[], Integer>> topScores` (`HashMap`)
	* `Map<Test, List<Student>> categoryWinners` (`HashMap`)
	* `Map<Subject, List<School>> categorySweepstakesWinners` (`HashMap`)
	* `List<School> sweepstakesWinners` (`ArrayList`)
5. Populate category winners lists with top scorers (as defined by award criteria)

	```java
	tabulateCategoryWinners(Level level, Set<Student> students, Map<Test, List<Student>> categoryWinners, Set<Test> testsGraded, Map<String, Integer> awardCriteria)
	```
6. Calculate school sweepstakes scores and number of tests fields

	```java
	school.calculateScores();
	school.calculateTestNums();
	```
7. Populate category sweepstakes winners maps and sweepstakes winners lists with top scorers

	```java
	tabulateCategorySweepstakesWinners(Map<String, School> schools, Map<Subject, List<School>> categorySweepstakesWinners)
	tabulateSweepstakesWinners(Map<String, School> schools, List<School> sweepstakeWinners)
	```

8. Persist JDOs in the GAE Datastore

	```java
	persistData(Level level, Collection<School> schools, Map<Test, List<Student>> categoryWinners, Map<Subject, List<School>> categorySweepstakesWinners, List<School> sweepstakesWinners)
	```

9. Update the GAE Datastore by modifying registrations to include actual number of tests taken

	```java
	updateRegistrations(Level level, Map<String, School> schools)
	```
10. Update the GAE Datastore by modifying contest information entity to include tests graded and last updated timestamp

	```java
	updateContestInfo(Set<Test> testsGraded, Entity contestInfo)
	```

Current Deployments
-------------------------
- https://tmsca.skc.name - Dulles TMSCA Tournament (November 22nd, 2014)
- https://villagetmsca.com - Village School TMSCA Qualifier (February 21st, 2015)
- https://fcmstmsca.com - First Colony TMSCA Qualifier (February 28th, 2015)
- https://qvtmsca.com - Quail Valley TMSCA Qualifier (March 7th, 2015)

License
-------

Content of TMSCA Tournament Management System (HTML, CSS, Static Assets, etc.) by Sushain Cherivirala is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

Codebase of TMSCA Tournament Management System (Java, JS, etc.) is licensed under the GNU General Public License Version 3.
