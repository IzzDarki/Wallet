## User manual
Only contains documentation about advanced features. The app is designed to be self-explanatory.

### Authentication
It is strongly recommended to setup authentication.
Once set up, you will be prompted to authenticate every time you open the app.
However, if you open the app again within 3 minutes, you will not have to authenticate again.
An exception is when the screen is turned off (while the app is opened).
In that case you will be prompted to authenticate again the next time you open the app.

### Autofill
You can choose this app as autofill service in the android settings.

The app then tries to find entries that match an incoming autofill request. This is done by
- Comparing the entry name with the url from which the autofill request is coming from (e.g. when logging in to a website `somewebsite.com` and the entry name is `Somewebsite`, the entry is used for the autofill request)
- Comparing the url with the entries fields (e.g. when there is a field with the value `example.com`, the entry is used for requests coming from `.../example.com`)
- Comparing the entry name with the app, that sent the autofill request (e.g. when there is a field with value `example.app`, the entry is used for requests coming from the app with package name `example.app`)
- If there is a field with the name like `url`, `website` or `app` and the value `*`, the entry is always used for autofill requests from any url or app respectively

#### Exclude an entry from autofill
Use the `no-autofill` label to exclude an entry from being used for autofill
