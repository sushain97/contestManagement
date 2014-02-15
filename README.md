TMSCA Contest Management
=================

Google App Engine Project to manage TMSCA Tournaments at the Middle School and High School level with support for automatic tabulation, user accounts contest result visualization, and much more.

Pages
-----

### Public Pages ###
* **Main page**: `/` Read a short description of the tournament and carousel of pictures (everything customizable through the Admin Panel).
* **About page**: `/about` Read a short description of the tournament (customizable through the Admin Panel), a tournament schedule (customizable through the Admin Panel), and an "About the Website" modal that provides attribution to the external frameworks utilized
* **Directions page**: `/directions` View an accordion with a set of directions to the host campus and a Google Maps view of the campus with an info window that provides the address and a directions link (everything customizable through the Admin Panel)
* **Contest Results pages**: `/contestResults` View the current grading progress; results are hidden by default and viewing is enabled through the admin panel. Functionality includes sorting of the tables based on any criteria.
    * **Sweepstakes pages**: `/contestResults?type=*_sweep` View the school sweepstakes and the students who made up each team.
    * **Category Sweepstakes pages**: `/contestResults?type=*_categorySweep` View the school sweekstakes for each category (Science, Calculator, Number Sense, Math) along with the team that composed each score
    * **Visualizations pages**: `/contestResults?type=*_visualizations` View box-and-whisker plots for the tests in each subject, split by grade.
    * **Category Winners pages**: `/contestResults?type=*_category_*` View the student winners of a category with their score and school (only first names can be displayed through the Admin Panel).
* **Contact Us page**: `/contactUs` Contact the tournament administrator for any questions, clarifications, or registration changes (protected by a reCAPTCHA).
* **Registration page**: `/registration` Register for the tournament and create an account at the same time. Either import your registrations via copying from a spreadsheet program or use the web interface to add students (protected by a reCAPTCHA). This page is disabled when the adminstrator disables registration through the admin panel.
* **Login page**: `/login` Login to your user account or as the administrator and choose whether to remain logged in (> 30 minutes). Passwords are stored using PDKDF2. When testing mode is enabled through the admin panel, a popout describes how to log in as an admin.
    * **Logout**: `/logout` Logout of your account.
    * **Password Reset Page**: `/forgotPass` Request a password reset to be sent to your email account and then reset your password.

### Administrator Pages ###
In addition to access to all public pages, an administrator has access to a few others. Furthermore, the admin can always view the contest results page even if results are not yet public and always submit registrations even if the window has ended.
* **Control Panel**: `/adminPanel` Allows setting of numerous environmental variables that control the behavior of the website
    * **Contest Title**: Set the contest title used throughout the website and in the navigation bar
    * **School Name**: Set the name of the host school used throughout the website and in emails
    * **Address**: Set the address of the host school used on the Directions page
    * **Absolute Location**: Set the absolute location (longitude and latitude) of the host school used on the Directions page
    * **About text**: Set the tournament description text used on the About page and the main page
    * **Schedule**: Set the tournament schedule in the form of a YAML Associative Array displayed on the About page
    * **Directions**: Set human-readable directions to the host school in the form of a YAML Associative Array displayed on the Directions page
    * **Slideshow**: Set the image, title, and captions of the slideshow in the form of a YAML List of Lists displayed on the Main page
    * **School Levels**: Set the school levels the tournament will host (a nonempty subset of ``{middle, high}``)
    * **Award Critera**: Set the placings which entitle students of each level (middle, high) to each type of award (medal, trophy), used on the category winners pages, category sweepstakes winners pages, and the sweepstakes winners pages
    * **Registration Dates**: Set the dates within which online registration is open
    * **E-Mail Address**: Specify the email address to which questions submitted through the Contact Us page are sent to
    * **Google E-Mail**: Specify the email address from which emails to users are sent to (the Google App Engine user that hosts the project)
    * **ReCAPTCHA API Keys**: Set the private and public reCAPTCHA keys used on the Contact Us and Registration pages
    * **Google OAuth 2.0 Credentials**: Set the Client Id and Client Secret to authenticate to the Spreadsheet API for the Tabulation driver
    * **Google Site Verification**: Set the Google Site Verification code used as the content attribute in `<meta name="google-site-verification">` on the Main page
    * **Google Analytics Tracking JS**: Set the Google Analytics Tracking Code (only the JS part, without the HTML `script` tag) used on all pages
    * **Test Charges**: Set the dollar cost for each test used on the registration page
    * **Update online scores**: Add a tabulation request to the task queue
    * **Make results public**: Make results public on the Contest Results page
    * **Change Admin Password**: Allows changing of the administrator password
    * **Enable Testing Mode**: Enables testing mode which resets the administrator credentials to `admin` and `password` and displays a popout on the Login page describing this
    * **Hide Full Names**: This option hides full names on public results
* **Contest Data pages**: `/data` Allow viewing and editing of contest data such as registrations and questions.
    * **Registrations page**: `data?choice=registrations` Allows the viewing of all registrations and inline editing of some properties of them in the table itself, also exposes access to a registration editing interface.
        * **Edit registration page**: `/editRegistration?key=*` Allows for the editing of a registration (all of its fields save the password field), deleting the user's account and deleting their entire registration (currently an irreversible action)
        * **Questions page**: `/data?choice=questions` Allows for the viewing of questions submitted through the Contact Us page and marking them as resolved (archiving) or deleting them entirely. Functionality includes sorting of the tables based on any criteria.
        * **Scores page**: `/data?choice=scores`:
            * **All students pages**: `/data?choice=scores&type=*_students` View the scores of all students with their grade and school name.
            * **Sweepstakes pages**: `/data?choice=scores&type=*_sweep` View the school sweepstakes and the students who made up each team.
            * **Category Sweepstakes pages**: `/data?choice=scores&type=*_categorySweep` View the school sweekstakes for each category (Science, Calculator, Number Sense, Math) along with the team that composed each score
            * **Visualizations pages**: `/data?choice=scores&type=*_visualizations` View box-and-whisker plots for the tests in each subject, split by grade.
            * **School pages**: `/data?choice=scores&type=*_school_*` View the school scores page for any school that displays the top 20 scorers, anonymous scores, school specific visualizations, and a breakdown of current sweepstakes scores.
            * **Category Winners pages**: `/data?choice=scores&type=*_category_*` View the student winners of a category with their score and school (only first names can be displayed through the Admin Panel).

### User Pages ###
In addition to access to all public pages, a logged-in user has access to a few others and modifications to some.
* **Main page**: `/` Rather than displaying the carousel and a contest description, this page provides all available information about the user's registration and access to the change password link.
* **Change Account Password page**: `/changePass` Allows modification of the logged-in users password after confirming their current password.
* **Contact Us page**: `/contactUs` The logged-in user can submit questions that are associated with their account automatically.
* **View Scores page**: `/viewScores` View the logged-in school's "top 20 scorers", anonymous scores, school specific visualizations, and a breakdown of current sweepstakes scores.

### Extras ###
* The title in the navigation bar is customizable through the Admin Panel
* Pages often contain a print button
* Custom error pages with detailed diagnostic information are provided for HTTP errors 401, 403, 404, 405, and 500

Tabulation
----------

### `Class Main extends HttpServlet` ###

### `Class School` ###

### `Class Score` ###

### `Class Student` ###

### `enum Test` ###