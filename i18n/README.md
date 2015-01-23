
# Internationalization Module

This module provides translations of user interface strings into supported languages, using [GWT's static
string internationalization tools](http://www.gwtproject.org/doc/latest/DevGuideI18n.html).

Translations are managed using PoEditor.com. Changes to the translation files, including changes to the
English text following initial development, should only be made online in PoEditor, not directly in the properties
files here.

## Adding new terms

During development, if you need to add new terms to UiConstants or UiMessages, add the terms directly to the
Java source file along with the proposed English text.

### Constants

For strings that require no parameters, add to UiConstants.java. Provide a @DefaultStringValue and optionally a
@Meaning attribute to provide a hint to the translators.

### Messages

For strings that require parameters, such as "Hello {your name here}", add a method to the UiMessages.java interface.

For more information on the message format, see the
[Messages JavaDoc](http://www.gwtproject.org/javadoc/latest/com/google/gwt/i18n/client/Messages.html)

## Updating PoEditor

Once you have settled on the new terms required for your feature, run

    mvn translations:push

To add the new terms to the PoEditor project.

## Incorporating new translations

Once the new terms have been translated, you can incorporate them in to your source by running

    mvn translations:pull

which update the translation properties files, _as well as_ the UiConstants and UiMessages source files if there
have been changes to the English text.

## Authenticating to PoEditor.com

In order to run `translations:push` and `translations:pull`, you must add your PoEditor API key to your maven
settings file, for example:

    <profile>
      <id>po</id>
      <properties>
        <po.editor.api.token>xxxxxxxxxxxxxxxxxxxxxxxxxxx</po.editor.api.token>
      </properties>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
    </profile>

