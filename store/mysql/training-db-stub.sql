
-- Upon sign up, all new users are given access to a common "Training" database
-- that gives new users something to play with
-- At the moment, the identity of this training database is hard coded into 
-- org/activityinfo/server/login/SignUpConfirmationController.java

-- To allow for testing, this script populates a minimum training database

insert into userlogin (userid, email, name, locale) values (999, 'training@example.com', 'Training owner', 'en');
insert into userdatabase (databaseId, name, countryId, ownerUserId, lastSchemaUpdate) values (507, 'Training', 1, 999, now());
insert into partner (partnerId, name) values (274, 'Training');
