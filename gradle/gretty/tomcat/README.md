Keystore was generated with:

~~~bash
$ keytool -genkey -alias tds -keyalg RSA -validity 3650 -keystore keystore
Enter keystore password: secret666
Re-enter new password: secret666
What is your first and last name?
  [Unknown]:  localhost
What is the name of your organizational unit?
  [Unknown]:
What is the name of your organization?
  [Unknown]:
What is the name of your City or Locality?
  [Unknown]:
What is the name of your State or Province?
  [Unknown]:
What is the two-letter country code for this unit?
  [Unknown]:
Is CN=localhost, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown correct?
  [no]:  yes

Enter key password for <tds>
	(RETURN if same as keystore password): RETURN
~~~