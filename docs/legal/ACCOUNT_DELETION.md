# Deleting your NixUra account and data

This page is the **web-accessible deletion path** for Google Play's account-deletion requirement. Publish it at a public URL
(for example on a GitHub Pages site) and enter that URL in Play Console > App content > Data safety > Account deletion.

## In the app (fastest)
Settings > Account > **Delete account**, enter your password, confirm. Everything on our server tied to the account is erased immediately:
the account record (email, name, age, password hash), all signed-in devices and tokens, pending codes and the security log entries about you.

## If you cannot open the app
Email **adhamelgendydev@gmail.com** from the address of the account with the subject "Delete my NixUra account". We will verify
it is you by sending a confirmation code to that address and delete the account within 30 days (normally within a few days).

## What is kept
- One anonymous log line that "an account was deleted" (no email, no id).
- Encrypted backups may hold copies until they roll over (up to [30] days), then they are gone.

## What stays on your device
Habits and events stored locally remain on the phone until you use Settings > Erase all data or uninstall the app. Deleting the account does not need them to be erased.
